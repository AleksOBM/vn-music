/**
 * @fileName Track.kt
 */
package io.github.aleksobm.vnmusic.track

import java.io.File
import java.net.URI

val audioExtensions = listOf("mp3", "flac", "ogg", "wav")

data class Track(
    val file: File,
    val streamUrl: String? = null,
    val streamName: String? = null
) {
    val name: String
        get() = streamName ?: file.nameWithoutExtension
    val path: String
        get() = streamUrl ?: file.absolutePath
    val folderName: String
        get() = if (streamUrl != null) "Internet radio" else file.parentFile?.name.orEmpty()
    val folderKey: String
        get() = if (streamUrl != null) "internet-radio" else file.parentFile?.absolutePath.orEmpty()

    companion object {
        fun fromUrl(url: String): Track? {
            val normalizedUrl = url.trim()
            val uri = runCatching { URI(normalizedUrl) }.getOrNull() ?: return null
            if (uri.scheme?.lowercase() !in listOf("http", "https") || uri.host.isNullOrBlank()) return null

            val stationPath = uri.path
                ?.takeIf { it.isNotBlank() && it != "/" }
                ?.substringAfterLast('/')
                ?.takeIf { it.isNotBlank() }
                ?.let { " — $it" }
                .orEmpty()
            return Track(
                file = File(uri.host),
                streamUrl = uri.toASCIIString(),
                streamName = uri.host + stationPath
            )
        }
    }
}

/** Возвращает список треков из папки (с подкаталогами) либо null, если папка не годится. */
fun loadTracksFromFolder(folder: File): List<Track>? {
    if (!folder.exists() || !folder.isDirectory) return null
    return folder.walkTopDown()
        .filter { it.isFile && it.extension.lowercase() in audioExtensions }
        .map { Track(it) }
        .sortedBy { it.name.lowercase() }
        .toList()
}