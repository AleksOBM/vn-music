package io.github.aleksobm.vnmusic.tabs

import io.github.aleksobm.vnmusic.track.Track
import io.github.aleksobm.vnmusic.track.audioExtensions
import io.github.aleksobm.vnmusic.track.loadTracksFromFolder
import java.io.File

data class PlaylistTab(
    val id: String = System.currentTimeMillis().toString(), // Уникальный ID
    val title: String,
    val folderPath: String?, // Если это папка
    val filePath: String?,   // Если это одиночный файл
    val filePaths: List<String> = filePath?.let { listOf(it) }.orEmpty(),
    val tracks: List<Track>
) {
    companion object {
        fun fromFolder(folder: File): PlaylistTab? {
            val tracks = loadTracksFromFolder(folder) ?: return null
            return PlaylistTab(
                title = folder.name,
                folderPath = folder.absolutePath,
                filePath = null,
                tracks = tracks
            )
        }

        fun fromFile(file: File): PlaylistTab? {
            if (file.extension.lowercase() !in audioExtensions) return null
            return PlaylistTab(
                title = file.nameWithoutExtension,
                folderPath = null,
                filePath = file.absolutePath,
                filePaths = listOf(file.absolutePath),
                tracks = listOf(Track(file))
            )
        }

        fun fromLocations(locations: List<String>, title: String): PlaylistTab? {
            val tracks = locations.mapNotNull { location ->
                if (location.startsWith("http://", ignoreCase = true) ||
                    location.startsWith("https://", ignoreCase = true)
                ) {
                    Track.fromUrl(location)
                } else {
                    val file = File(location)
                    if (file.extension.lowercase() in audioExtensions) Track(file) else null
                }
            }
            if (tracks.isEmpty()) return null

            return PlaylistTab(
                title = title,
                folderPath = null,
                filePath = tracks.first().path,
                filePaths = tracks.map { it.path },
                tracks = tracks
            )
        }
    }
}