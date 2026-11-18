package com.example.vnmusic.tabs

import com.example.vnmusic.track.Track
import com.example.vnmusic.track.audioExtensions
import com.example.vnmusic.track.loadTracksFromFolder
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

        fun fromFiles(files: List<File>, title: String): PlaylistTab? {
            val audioFiles = files
                .filter { it.isFile && it.extension.lowercase() in audioExtensions }
            if (audioFiles.isEmpty()) return null

            val paths = audioFiles.map { it.absolutePath }
            return PlaylistTab(
                title = title,
                folderPath = null,
                filePath = paths.first(),
                filePaths = paths,
                tracks = audioFiles.map { Track(it) }
            )
        }
    }
}