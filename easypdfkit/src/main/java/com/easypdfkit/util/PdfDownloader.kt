package com.easypdfkit.util

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Pluggable download strategy. Ship your own (OkHttp, Ktor, Firebase SDK)
 * by implementing this interface — the library stays dependency-free.
 */
fun interface PdfDownloader {

    /** Blocking download. Called on a background dispatcher by the library. */
    fun download(context: Context, url: String): File

    companion object {
        @JvmStatic
        var progressListener: ((bytesRead: Long, total: Long) -> Unit)? = null

        /** Zero-dependency default using HttpURLConnection with disk caching. */
        @JvmStatic
        val Default = PdfDownloader { context, url ->
            val cached = File(context.cacheDir, "easypdf_dl_${url.hashCode()}.pdf")
            if (cached.exists() && cached.length() > 0) return@PdfDownloader cached

            val tmp = File(cached.parentFile, cached.name + ".part")
            val conn = URL(url).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 15_000
                conn.readTimeout = 30_000
                conn.instanceFollowRedirects = true
                if (conn.responseCode !in 200..299) {
                    error("HTTP ${conn.responseCode} for $url")
                }
                val total = conn.contentLengthLong
                var read = 0L
                conn.inputStream.use { input ->
                    tmp.outputStream().use { output ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n == -1) break
                            output.write(buf, 0, n)
                            read += n
                            progressListener?.invoke(read, total)
                        }
                    }
                }
                check(tmp.renameTo(cached)) { "Could not finalize download" }
                cached
            } finally {
                conn.disconnect()
                tmp.delete()
            }
        }
    }
}
