package com.easypdfkit.source

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.easypdfkit.util.PdfDownloader
import java.io.File

/**
 * Every supported PDF input for v1.0.
 *
 * All variants resolve to a [ParcelFileDescriptor] because Pdfium's
 * `newDocument()` consumes an fd. Byte arrays and streams are spooled to a
 * cache file first — this keeps memory flat for large documents.
 */
sealed class PdfSource {

    data class FromFile(val file: File) : PdfSource()
    data class FromUri(val uri: Uri) : PdfSource()
    data class FromAsset(val assetName: String) : PdfSource()
    data class FromBytes(val bytes: ByteArray) : PdfSource()

    /** Remote URL — works with any direct link incl. Firebase Storage download URLs. */
    data class FromUrl(
        val url: String,
        val downloader: PdfDownloader = PdfDownloader.Default
    ) : PdfSource()

    /**
     * Resolves this source to a seekable file descriptor.
     * Runs blocking I/O — always call from a background dispatcher.
     */
    internal fun openFileDescriptor(context: Context): ParcelFileDescriptor = when (this) {
        is FromFile -> ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)

        is FromUri -> context.contentResolver.openFileDescriptor(uri, "r")
            ?: error("Cannot open URI: $uri")

        is FromAsset -> {
            val out = File(context.cacheDir, "easypdf_asset_${assetName.hashCode()}.pdf")
            if (!out.exists() || out.length() == 0L) {
                context.assets.open(assetName).use { input ->
                    out.outputStream().use { input.copyTo(it) }
                }
            }
            ParcelFileDescriptor.open(out, ParcelFileDescriptor.MODE_READ_ONLY)
        }

        is FromBytes -> {
            val out = File(context.cacheDir, "easypdf_bytes_${bytes.contentHashCode()}.pdf")
            if (!out.exists() || out.length() != bytes.size.toLong()) {
                out.writeBytes(bytes)
            }
            ParcelFileDescriptor.open(out, ParcelFileDescriptor.MODE_READ_ONLY)
        }

        is FromUrl -> {
            val out = downloader.download(context, url)
            ParcelFileDescriptor.open(out, ParcelFileDescriptor.MODE_READ_ONLY)
        }
    }
}
