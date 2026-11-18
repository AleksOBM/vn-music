/**
 * @fileName TabsFile.kt
 */
package com.example.vnmusic.tabs

import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*

private val tabsFile = File(System.getProperty("user.home"), ".vnmusic_tabs.txt")

fun saveTabs(tabs: List<PlaylistTab>) {
    try {
        val lines = tabs.map { tab ->
            if (tab.folderPath != null) "FOLDER:${tab.folderPath}"
            else {
                val title = Base64.getEncoder()
                    .encodeToString(tab.title.toByteArray(StandardCharsets.UTF_8))
                val paths = tab.filePaths.map { path ->
                    Base64.getEncoder()
                        .encodeToString(path.toByteArray(StandardCharsets.UTF_8))
                }
                "FILE:$title:${paths.joinToString(",")}"
            }
        }
        tabsFile.writeText(lines.joinToString("\n"))
    } catch (_: Exception) {
    }
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
                    val value = line.removePrefix("FILE:")
                    val parts = value.split(":", limit = 2)
                    if (parts.size == 2) {
                        val title = String(
                            Base64.getDecoder().decode(parts[0]),
                            StandardCharsets.UTF_8
                        ).replace(
                            Regex("""^Новый плейлист (\d+)$"""),
                            "New playlist $1"
                        )
                        val paths = parts[1]
                            .split(",")
                            .filter { it.isNotEmpty() }
                            .map {
                                String(
                                    Base64.getDecoder().decode(it),
                                    StandardCharsets.UTF_8
                                )
                            }
                        if (paths.isEmpty()) {
                            PlaylistTab(
                                title = title,
                                folderPath = null,
                                filePath = null,
                                filePaths = emptyList(),
                                tracks = emptyList()
                            )
                        } else {
                            PlaylistTab.fromLocations(paths, title)
                        }
                    } else {
                        PlaylistTab.fromLocations(
                            listOf(value), File(value).nameWithoutExtension)
                    }
                }

                else -> null
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}