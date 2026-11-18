package com.example.vnmusic

import java.io.File

data class PlaylistTab(
    val id: String = System.currentTimeMillis().toString(), // Уникальный ID
    val title: String,
    val folderPath: String?, // Если это папка
    val filePath: String?,   // Если это одиночный файл
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
                tracks = listOf(Track(file))
            )
        }
    }
}