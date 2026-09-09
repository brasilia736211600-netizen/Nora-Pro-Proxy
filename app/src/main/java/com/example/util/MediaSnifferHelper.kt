package com.example.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.example.model.MediaType
import com.example.model.SniffedMedia

object MediaSnifferHelper {

    private val VIDEO_EXTENSIONS = listOf(".mp4", ".m3u8", ".webm", ".ts", ".flv", ".mov", ".m4v", ".3gp", ".avi", ".mkv")
    private val AUDIO_EXTENSIONS = listOf(".mp3", ".m4a", ".aac", ".ogg", ".wav", ".flac")

    fun isMediaUrl(url: String): Boolean {
        if (url.isBlank()) return false
        val lower = url.lowercase().substringBefore("?")

        for (ext in VIDEO_EXTENSIONS) {
            if (lower.endsWith(ext) || lower.contains("$ext?")) return true
        }

        for (ext in AUDIO_EXTENSIONS) {
            if (lower.endsWith(ext) || lower.contains("$ext?")) return true
        }

        if (lower.contains("manifest.mpd") || lower.contains("master.m3u8") || lower.contains("/videoplayback")) {
            return true
        }

        return false
    }

    fun parseMediaType(url: String): Pair<MediaType, String> {
        val lower = url.lowercase()
        return when {
            lower.contains(".m3u8") -> MediaType.STREAM_HLS to "application/vnd.apple.mpegurl"
            lower.contains(".mpd") -> MediaType.STREAM_DASH to "application/dash+xml"
            lower.contains(".mp4") -> MediaType.VIDEO to "video/mp4"
            lower.contains(".webm") -> MediaType.VIDEO to "video/webm"
            lower.contains(".mp3") -> MediaType.AUDIO to "audio/mpeg"
            lower.contains(".m4a") -> MediaType.AUDIO to "audio/mp4"
            else -> MediaType.VIDEO to "video/mp4"
        }
    }

    const val DOM_SNIFF_SCRIPT = """
        (function() {
            var results = [];
            var elements = document.querySelectorAll('video, audio, source');
            elements.forEach(function(el) {
                var src = el.src || el.currentSrc || el.getAttribute('src');
                if (src && src.startsWith('http')) {
                    results.push(src);
                }
            });
            return JSON.stringify(results);
        })();
    """

    fun downloadMedia(context: Context, media: SniffedMedia) {
        try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return
            val uri = Uri.parse(media.url)
            val fileName = run {
                val candidate = uri.lastPathSegment ?: "video_${System.currentTimeMillis()}.mp4"
                if (candidate.contains(".")) candidate else "$candidate.mp4"
            }

            val request = DownloadManager.Request(uri).apply {
                setTitle(media.title.ifBlank { fileName })
                setDescription("تنزيل عبر Nora Browser")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setMimeType(media.mimeType)
            }
            dm.enqueue(request)
            Toast.makeText(context, "بدأ التنزيل: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر بدء التنزيل: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun openInExternalPlayer(context: Context, media: SniffedMedia) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(media.url), media.mimeType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "تشغيل عبر"))
        } catch (e: Exception) {
            Toast.makeText(context, "لا يوجد مشغل فيديو متوافق: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
