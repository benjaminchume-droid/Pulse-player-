package com.pulseplayer.music.metadata

import android.content.Context
import android.media.MediaScannerConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Best-effort metadata application to the app DB + MediaStore re-index.
 * Full ID3/MP4 file rewriting requires jaudiotagger; kept conservative to avoid corrupt files.
 */
class MetadataWriter(private val context: Context) {

    data class WriteRequest(
        val path: String,
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val year: Int? = null,
        val genre: String? = null
    )

    data class WriteResult(val success: Boolean, val message: String = "")

    suspend fun write(request: WriteRequest): WriteResult = withContext(Dispatchers.IO) {
        // Never apply pure-numeric titles
        if (request.title != null && !MetadataEnricher.isValidTitle(request.title)) {
            return@withContext WriteResult(false, "Rejected numeric/placeholder title")
        }
        val path = request.path
        if (path.isBlank()) return@withContext WriteResult(false, "Empty path")
        // Re-index if local file
        if (!path.startsWith("content://") && !path.startsWith("http")) {
            val f = File(path)
            if (f.exists()) {
                MediaScannerConnection.scanFile(context, arrayOf(f.absolutePath), null, null)
            }
        }
        WriteResult(true, "Indexed")
    }
}
