package com.easypdfkit.core

import android.content.Context
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import com.easypdfkit.source.PdfSource
import io.legere.pdfiumandroid.PdfDocument
import io.legere.pdfiumandroid.PdfiumCore
import io.legere.pdfiumandroid.util.Size

/** Thrown when the document is encrypted and the supplied password is wrong/missing. */
class PdfPasswordException(message: String) : Exception(message)

/**
 * Thin, thread-confined wrapper around [PdfiumCore].
 *
 * Pdfium is NOT thread-safe: every call into this class must happen on the
 * single render dispatcher owned by [com.easypdfkit.EasyPdfView]. The wrapper
 * exists so the view never touches Pdfium types directly, which keeps the door
 * open for swapping engines later.
 */
internal class PdfEngine(context: Context) {

    private val core = PdfiumCore(context)
    private var document: PdfDocument? = null
    private var fd: ParcelFileDescriptor? = null
    private val openedPages = mutableSetOf<Int>()

    var pageCount: Int = 0
        private set

    /** Native page sizes in PDF points, indexed by page. */
    val pageSizes = mutableListOf<Size>()

    val isOpen: Boolean get() = document != null

    @Throws(PdfPasswordException::class)
    fun open(context: Context, source: PdfSource, password: String?) {
        close()
        val descriptor = source.openFileDescriptor(context)
        val doc = try {
            core.newDocument(descriptor, password)
        } catch (t: Throwable) {
            descriptor.close()
            // Pdfium reports a bad/missing password as an IOException with this marker
            if (t.message?.contains("password", ignoreCase = true) == true) {
                throw PdfPasswordException("Document is password protected")
            }
            throw t
        }
        fd = descriptor
        document = doc
        pageCount = core.getPageCount(doc)
        pageSizes.clear()
        for (i in 0 until pageCount) {
            // getPageSize works without opening the page (uses document catalog)
            pageSizes.add(core.getPageSize(doc, i))
        }
    }

    /**
     * Renders a page region into [bitmap] (ARGB_8888).
     *
     * @param startX/startY offset of the page's top-left relative to the bitmap, in pixels
     * @param sizeX/sizeY   full rendered page size in pixels at the current zoom
     */
    fun renderPage(
        pageIndex: Int,
        bitmap: Bitmap,
        startX: Int, startY: Int,
        sizeX: Int, sizeY: Int
    ) {
        val doc = document ?: return
        if (pageIndex !in openedPages) {
            core.openPage(doc, pageIndex)
            openedPages.add(pageIndex)
        }
        core.renderPageBitmap(doc, bitmap, pageIndex, startX, startY, sizeX, sizeY, true)
    }

    fun close() {
        document?.let { core.closeDocument(it) }
        document = null
        openedPages.clear()
        runCatching { fd?.close() }
        fd = null
        pageCount = 0
        pageSizes.clear()
    }
}
