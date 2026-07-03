package com.easypdfkit.cache

import android.graphics.Bitmap
import android.util.LruCache

/**
 * Memory-bounded page bitmap cache.
 *
 * Sized at 1/6th of the app heap — enough for ~6-10 rendered pages on most
 * devices, which comfortably covers the visible window plus prefetch, while
 * leaving headroom for the host app. Keys combine page index and a zoom
 * bucket so stale-resolution bitmaps are evicted naturally.
 */
internal class PageBitmapCache {

    data class Key(val page: Int, val zoomBucket: Int)

    private val maxKb = (Runtime.getRuntime().maxMemory() / 1024 / 6).toInt()

    private val lru = object : LruCache<Key, Bitmap>(maxKb) {
        override fun sizeOf(key: Key, value: Bitmap) = value.byteCount / 1024
        override fun entryRemoved(evicted: Boolean, key: Key, old: Bitmap, new: Bitmap?) {
            if (evicted) old.recycle()
        }
    }

    operator fun get(key: Key): Bitmap? = lru.get(key)?.takeIf { !it.isRecycled }

    fun put(key: Key, bitmap: Bitmap) { lru.put(key, bitmap) }

    fun clear() = lru.evictAll()
}
