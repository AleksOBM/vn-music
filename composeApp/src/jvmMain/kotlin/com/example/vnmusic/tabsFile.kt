package com.example.vnmusic

import java.io.File

private val tabsFile = File(System.getProperty("user.home"), ".vnmusic_tabs.txt")

fun saveTabs(tabs: List<PlaylistTab>) {
    try {
        val lines = tabs.map { tab ->
            if (tab.folderPath != null) "FOLDER:${tab.folderPath}"
            else "FILE:${tab.filePath}"
        }
        tabsFile.writeText(lines.joinToString("\n"))
    } catch (_: Exception) {}
}

fun loadTabs(): List<PlaylistTab> {
    return try {
        if (!tabsFile.exists()) return emptyList()
        tabsFile.readLines().mapNotNull { line ->
            when {
                line.startsWith("FOLDER:") -> {
                    val path = line.removePrefix("FOLDER:")
                    PlaylistTab.fromFolder(File(path))
                }
                line.startsWith("FILE:") -> {
                    val path = line.removePrefix("FILE:")
                    PlaylistTab.fromFile(File(path))
                }
                else -> null
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}