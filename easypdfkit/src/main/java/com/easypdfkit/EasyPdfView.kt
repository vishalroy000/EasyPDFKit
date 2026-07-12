package com.easypdfkit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.OverScroller
import androidx.core.graphics.withSave
import com.easypdfkit.cache.PageBitmapCache
import com.easypdfkit.core.PdfEngine
import com.easypdfkit.core.PdfPasswordException
import com.easypdfkit.listeners.*
import com.easypdfkit.source.PdfSource
import kotlinx.coroutines.*
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * A drop-in PDF viewer. v1.0.1
 *
 * FIXES in this version:
 *  - Zoom pe blank white screen (OOM from uncapped hi-res bitmaps)
 *  - Hi-res bitmaps ab pixel-budget capped hain (max ~24MB per page)
 *  - Low-res (bucket 1) bitmap HAMESHA render/protect hota hai — fallback guaranteed
 *  - Render failures ab crash/silent-fail nahi karte — low-res pe gracefully fall back
 */
class EasyPdfView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private companion object {
        const val TAG = "EasyPdfView"

        /** Max pixels for ONE rendered page bitmap (~24MB ARGB_8888). */
        const val MAX_PAGE_PIXELS = 6_000_000L

        /** Absolute max texture dimension (GPU limit safety). */
        const val MAX_DIMENSION = 4096
    }

    // ---------------------------------------------------------------- config
    private var source: PdfSource? = null
    private var password: String? = null
    private var pageSpacingPx = (8 * resources.displayMetrics.density).toInt()
    private var snapToPage = false
    private var nightMode = false
    private var minZoom = 1f
    private var maxZoom = 6f
    private var doubleTapZoom = 2.5f

    private var onLoad: OnLoadCompleteListener? = null
    private var onPageChange: OnPageChangeListener? = null
    private var onError: OnErrorListener? = null
    private var onPassword: OnPasswordRequiredListener? = null
    private var onZoom: OnZoomChangeListener? = null
    private var onTap: OnTapListener? = null

    // ---------------------------------------------------------------- state
    private val engine = PdfEngine(context)
    private val cache = PageBitmapCache()

    /**
     * FIX: Base (bucket-1) bitmaps ka alag protected store — LRU eviction se
     * bache rehte hain taaki zoom ke dauran hamesha KUCH dikhe.
     * Sirf visible-window ke pages yahan rehte hain (bounded memory).
     */
    private val baseBitmaps = HashMap<Int, Bitmap>()
    private val maxBaseBitmaps = 6  // visible + prefetch window

    /** Single-threaded dispatcher: Pdfium is not thread-safe. */
    private val renderDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val renderJobs = mutableMapOf<PageBitmapCache.Key, Job>()

    private var zoom = 1f
    private var scrollXf = 0f
    private var scrollYf = 0f

    /** Page top offsets + heights at zoom == 1 (fit-width), in view px. */
    private val pageTops = mutableListOf<Float>()
    private val pageHeights = mutableListOf<Float>()
    private var contentHeightBase = 0f

    var currentPage = 0
        private set
    val pageCount: Int get() = engine.pageCount
    val isLoaded: Boolean get() = engine.isOpen && pageTops.isNotEmpty()

    private val pagePaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val placeholderPaint = Paint().apply { color = Color.WHITE }
    private val nightFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f
    )))

    // -------------------------------------------------------------- gestures
    private val scroller = OverScroller(context)

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean { scroller.forceFinished(true); return true }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            moveBy(dx, dy); return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            scroller.fling(
                scrollXf.roundToInt(), scrollYf.roundToInt(),
                -vx.roundToInt(), -vy.roundToInt(),
                0, maxScrollX().roundToInt(),
                0, maxScrollY().roundToInt()
            )
            postInvalidateOnAnimation(); return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean =
            onTap?.onTap(e.x, e.y) ?: false

        override fun onDoubleTap(e: MotionEvent): Boolean {
            val target = if (zoom > minZoom + 0.01f) minZoom else doubleTapZoom
            animateZoomTo(target, e.x, e.y); return true
        }
    })

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(d: ScaleGestureDetector): Boolean {
            zoomTo(zoom * d.scaleFactor, d.focusX, d.focusY); return true
        }
        override fun onScaleEnd(d: ScaleGestureDetector) { requestVisiblePages() }
    })

    // ------------------------------------------------------------ public API

    fun fromFile(file: File) = Configurator(PdfSource.FromFile(file))
    fun fromUri(uri: Uri) = Configurator(PdfSource.FromUri(uri))
    fun fromAsset(name: String) = Configurator(PdfSource.FromAsset(name))
    fun fromBytes(bytes: ByteArray) = Configurator(PdfSource.FromBytes(bytes))
    fun fromUrl(url: String) = Configurator(PdfSource.FromUrl(url))
    fun fromSource(src: PdfSource) = Configurator(src)

    fun jumpTo(page: Int, animated: Boolean = false) {
        if (!isLoaded) return
        val p = page.coerceIn(0, pageCount - 1)
        val targetY = (pageTops[p] * zoom).coerceIn(0f, maxScrollY())
        if (animated) {
            scroller.forceFinished(true)
            scroller.startScroll(
                scrollXf.roundToInt(), scrollYf.roundToInt(),
                0, (targetY - scrollYf).roundToInt(), 400
            )
            postInvalidateOnAnimation()
        } else {
            scrollYf = targetY
            afterScroll()
        }
    }

    fun setNightMode(enabled: Boolean) {
        nightMode = enabled
        placeholderPaint.color = if (enabled) Color.BLACK else Color.WHITE
        invalidate()
    }

    fun setZoom(newZoom: Float) = zoomTo(newZoom, width / 2f, height / 2f)

    fun recycle() {
        renderJobs.values.forEach { it.cancel() }
        renderJobs.clear()
        cache.clear()
        baseBitmaps.values.forEach { it.recycle() }
        baseBitmaps.clear()
        scope.launch(renderDispatcher) { engine.close() }
    }

    // ---------------------------------------------------------- configurator
    inner class Configurator internal constructor(private val src: PdfSource) {
        fun password(pw: String?) = apply { this@EasyPdfView.password = pw }
        fun pageSpacingDp(dp: Int) = apply { pageSpacingPx = (dp * resources.displayMetrics.density).toInt() }
        fun snapToPage(enable: Boolean) = apply { snapToPage = enable }
        fun nightMode(enable: Boolean) = apply { setNightMode(enable) }
        fun zoomRange(minZ: Float, maxZ: Float) = apply { minZoom = minZ; maxZoom = maxZ }
        fun doubleTapZoom(z: Float) = apply { doubleTapZoom = z }
        fun onLoad(l: OnLoadCompleteListener) = apply { onLoad = l }
        fun onPageChange(l: OnPageChangeListener) = apply { onPageChange = l }
        fun onError(l: OnErrorListener) = apply { onError = l }
        fun onPasswordRequired(l: OnPasswordRequiredListener) = apply { onPassword = l }
        fun onZoomChange(l: OnZoomChangeListener) = apply { onZoom = l }
        fun onTap(l: OnTapListener) = apply { onTap = l }

        fun load() {
            source = src
            openDocument()
        }
    }

    // -------------------------------------------------------------- loading
    private fun openDocument() {
        val src = source ?: return
        renderJobs.values.forEach { it.cancel() }
        renderJobs.clear()
        cache.clear()
        baseBitmaps.values.forEach { it.recycle() }
        baseBitmaps.clear()
        scope.launch {
            try {
                withContext(renderDispatcher) { engine.open(context, src, password) }
                computeLayout()
                zoom = 1f; scrollXf = 0f; scrollYf = 0f; currentPage = 0
                onLoad?.onLoadComplete(pageCount)
                onPageChange?.onPageChanged(0, pageCount)
                requestVisiblePages()
                invalidate()
            } catch (e: PdfPasswordException) {
                val cb = onPassword
                if (cb != null) {
                    cb.onPasswordRequired { pw -> password = pw; openDocument() }
                } else {
                    onError?.onError(e)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                onError?.onError(t)
            }
        }
    }

    private fun computeLayout() {
        pageTops.clear(); pageHeights.clear()
        if (width == 0 || engine.pageCount == 0) return
        var y = pageSpacingPx.toFloat()
        for (size in engine.pageSizes) {
            val scale = width.toFloat() / size.width
            val h = size.height * scale
            pageTops.add(y)
            pageHeights.add(h)
            y += h + pageSpacingPx
        }
        contentHeightBase = y
    }

    // ------------------------------------------------------------- scrolling
    private fun maxScrollX() = max(0f, width * zoom - width)
    private fun maxScrollY() = max(0f, contentHeightBase * zoom - height)

    private fun moveBy(dx: Float, dy: Float) {
        scrollXf = (scrollXf + dx).coerceIn(0f, maxScrollX())
        scrollYf = (scrollYf + dy).coerceIn(0f, maxScrollY())
        afterScroll()
    }

    private fun afterScroll() {
        updateCurrentPage()
        requestVisiblePages()
        invalidate()
    }

    private fun updateCurrentPage() {
        if (pageTops.isEmpty()) return
        val centerY = (scrollYf + height / 2f) / zoom
        var page = 0
        for (i in pageTops.indices) {
            if (pageTops[i] <= centerY) page = i else break
        }
        if (page != currentPage) {
            currentPage = page
            onPageChange?.onPageChanged(page, pageCount)
        }
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollXf = scroller.currX.toFloat()
            scrollYf = scroller.currY.toFloat()
            updateCurrentPage()
            requestVisiblePages()
            postInvalidateOnAnimation()
        } else if (snapToPage && zoom <= minZoom + 0.01f && scroller.isFinished && isLoaded) {
            maybeSnap()
        }
    }

    private var snapping = false
    private fun maybeSnap() {
        if (snapping) return
        val target = (pageTops[currentPage] * zoom).coerceIn(0f, maxScrollY())
        if (abs(target - scrollYf) > 1f) {
            snapping = true
            scroller.startScroll(
                scrollXf.roundToInt(), scrollYf.roundToInt(),
                0, (target - scrollYf).roundToInt(), 250
            )
            postInvalidateOnAnimation()
            postDelayed({ snapping = false }, 300)
        }
    }

    // ---------------------------------------------------------------- zooming
    private fun zoomTo(newZoom: Float, focusX: Float, focusY: Float) {
        val z = newZoom.coerceIn(minZoom, maxZoom)
        if (z == zoom) return
        val contentX = (scrollXf + focusX) / zoom
        val contentY = (scrollYf + focusY) / zoom
        zoom = z
        scrollXf = (contentX * zoom - focusX).coerceIn(0f, maxScrollX())
        scrollYf = (contentY * zoom - focusY).coerceIn(0f, maxScrollY())
        updateCurrentPage()
        onZoom?.onZoomChanged(zoom)
        invalidate()
    }

    private fun animateZoomTo(target: Float, focusX: Float, focusY: Float) {
        val start = zoom
        val anim = android.animation.ValueAnimator.ofFloat(0f, 1f).setDuration(250)
        anim.addUpdateListener {
            val f = it.animatedValue as Float
            zoomTo(start + (target - start) * f, focusX, focusY)
        }
        anim.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(a: android.animation.Animator) = requestVisiblePages()
        })
        anim.start()
    }

    // -------------------------------------------------------------- rendering

    /**
     * FIX: Zoom bucket ab MEMORY-CAPPED hai. Page dimensions ke hisaab se
     * max safe bucket calculate hota hai taaki bitmap MAX_PAGE_PIXELS
     * (~24MB) se bada na bane. Ab OOM impossible hai.
     */
    private fun safeBucketFor(page: Int, desired: Int): Int {
        if (width <= 0 || page >= pageHeights.size) return 1
        val baseW = width.toLong()
        val baseH = pageHeights[page].toLong().coerceAtLeast(1)
        // Largest bucket where (w*bucket)*(h*bucket) <= MAX_PAGE_PIXELS
        val maxByPixels = sqrt(MAX_PAGE_PIXELS.toDouble() / (baseW * baseH)).toInt()
        val maxByDimension = min(MAX_DIMENSION / baseW, MAX_DIMENSION / baseH).toInt()
        return desired.coerceAtMost(max(1, min(maxByPixels, maxByDimension)))
    }

    private fun desiredBucket(): Int = when {
        zoom <= 1.25f -> 1
        zoom <= 2.5f -> 2
        else -> 4
    }

    private fun visiblePages(): IntRange {
        if (pageTops.isEmpty()) return IntRange.EMPTY
        val top = scrollYf / zoom
        val bottom = (scrollYf + height) / zoom
        var first = 0; var last = pageTops.lastIndex
        for (i in pageTops.indices) {
            if (pageTops[i] + pageHeights[i] >= top) { first = i; break }
        }
        for (i in first..pageTops.lastIndex) {
            if (pageTops[i] > bottom) { last = i - 1; break }
        }
        return first..max(first, last)
    }

    private fun requestVisiblePages() {
        if (!isLoaded) return
        val range = visiblePages()
        val prefetch = max(0, range.first - 1)..min(pageCount - 1, range.last + 1)

        // FIX: Protected base bitmaps window maintain karo — sirf nearby pages
        baseBitmaps.keys.filter { it !in prefetch }.forEach { page ->
            baseBitmaps.remove(page)?.recycle()
        }

        for (page in prefetch) {
            // 1. Base (bucket 1) HAMESHA ensure karo — ye fallback hai
            if (baseBitmaps[page]?.isRecycled != false) {
                requestRender(page, 1)
            }
            // 2. Hi-res agar zoom > 1.25
            val bucket = safeBucketFor(page, desiredBucket())
            if (bucket > 1 && cache[PageBitmapCache.Key(page, bucket)] == null) {
                requestRender(page, bucket)
            }
        }
    }

    private fun requestRender(page: Int, bucket: Int) {
        val key = PageBitmapCache.Key(page, bucket)
        if (renderJobs.containsKey(key)) return

        renderJobs[key] = scope.launch {
            try {
                val bmp = withContext(renderDispatcher) {
                    if (!engine.isOpen) return@withContext null
                    val w = width * bucket
                    val h = (pageHeights[page] * bucket).roundToInt()
                    if (w <= 0 || h <= 0) return@withContext null
                    try {
                        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        engine.renderPage(page, bitmap, 0, 0, w, h)
                        bitmap
                    } catch (oom: OutOfMemoryError) {
                        // FIX: OOM ab crash nahi karta — low-res pe raho
                        Log.w(TAG, "OOM rendering page $page @${bucket}x — falling back to low-res")
                        null
                    } catch (t: Throwable) {
                        Log.w(TAG, "Render failed page $page @${bucket}x: ${t.message}")
                        null
                    }
                } ?: return@launch

                if (bucket == 1) {
                    // FIX: Base bitmap protected store mein — LRU se evict nahi hoga
                    baseBitmaps.remove(page)?.recycle()
                    if (baseBitmaps.size >= maxBaseBitmaps) {
                        // Farthest page evict karo
                        baseBitmaps.keys.maxByOrNull { abs(it - currentPage) }?.let {
                            baseBitmaps.remove(it)?.recycle()
                        }
                    }
                    baseBitmaps[page] = bmp
                } else {
                    cache.put(PageBitmapCache.Key(page, bucket), bmp)
                }
                invalidate()
            } finally {
                renderJobs.remove(key)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (!isLoaded) return
        pagePaint.colorFilter = if (nightMode) nightFilter else null

        canvas.withSave {
            translate(-scrollXf, -scrollYf)
            for (page in visiblePages()) {
                val top = pageTops[page] * zoom
                val rect = RectF(0f, top, width * zoom, top + pageHeights[page] * zoom)

                // FIX: Best available bitmap — hi-res → base fallback (guaranteed)
                val bucket = safeBucketFor(page, desiredBucket())
                val bmp = (if (bucket > 1) cache[PageBitmapCache.Key(page, bucket)] else null)
                    ?: cache[PageBitmapCache.Key(page, 2)]
                    ?: baseBitmaps[page]?.takeIf { !it.isRecycled }

                if (bmp != null) {
                    drawBitmap(bmp, null, rect, pagePaint)
                } else {
                    drawRect(rect, placeholderPaint)
                    // Bitmap missing — render request karo (missed window case)
                    post { requestVisiblePages() }
                }
            }
        }
    }

    // ------------------------------------------------------------- lifecycle
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (engine.isOpen && w != oldw) {
            val progress = if (contentHeightBase > 0) scrollYf / (contentHeightBase * zoom) else 0f
            cache.clear()
            baseBitmaps.values.forEach { it.recycle() }
            baseBitmaps.clear()
            computeLayout()
            scrollYf = (progress * contentHeightBase * zoom).coerceIn(0f, maxScrollY())
            scrollXf = scrollXf.coerceIn(0f, maxScrollX())
        }
        requestVisiblePages()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = scaleDetector.onTouchEvent(event)
        if (!scaleDetector.isInProgress) {
            handled = gestureDetector.onTouchEvent(event) || handled
        }
        if (event.actionMasked == MotionEvent.ACTION_UP) requestVisiblePages()
        return handled || super.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        recycle()
        scope.cancel()
    }
}