/**
 * @fileName Track.kt
 */
package com.example.vnmusic

import java.io.File

val audioExtensions = listOf("mp3", "flac", "ogg", "wav")

data class Track(val file: File) {
    val name: String get() = file.nameWithoutExtension
    val path: String get() = file.absolutePath
}

val lastFolderFile = File(System.getProperty("user.home"), ".vnmusic_last_folder.txt")

fun saveLastFolder(path: String) {
    try {
        lastFolderFile.writeText(path)
    } catch (_: Exception) {
    }
}

fun loadLastFolder(): String? {
    return try {
        if (lastFolderFile.exists()) lastFolderFile.readText().trim().takeIf { it.isNotEmpty() } else null
    } catch (_: Exception) {
        null
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