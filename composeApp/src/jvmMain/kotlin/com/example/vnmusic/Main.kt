package com.example.vnmusic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Slider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.kdroidfilter.composemediaplayer.audio.AudioPlayerState
import io.github.kdroidfilter.composemediaplayer.audio.rememberAudioPlayerLiveState
import java.awt.datatransfer.DataFlavor
import java.io.File
import javax.swing.JFileChooser

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "VN Music",
        state = rememberWindowState(width = 420.dp, height = 700.dp)
    ) {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                App()
            }
        }
    }
}

fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App() {
    val audioState = rememberAudioPlayerLiveState()

    var volume by remember { mutableStateOf(1f) }
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var currentTrack by remember { mutableStateOf<Track?>(null) }
    var sliderPosition by remember { mutableStateOf(0f) }
    val audioExtensions = listOf("mp3", "flac", "ogg", "wav")
    val playerState = audioState.state

    // Синхронизация слайдера
    LaunchedEffect(audioState.position) {
        if (playerState == AudioPlayerState.PLAYING || playerState == AudioPlayerState.PAUSED) {
            sliderPosition = audioState.position.toFloat()
        }
    }

    // Загрузка последней папки
    LaunchedEffect(Unit) {
        loadLastFolder()?.let { lastPath ->
            val folder = File(lastPath)
            if (folder.exists() && folder.isDirectory) {
                tracks = folder.walkTopDown()
                    .filter { it.isFile && it.extension.lowercase() in audioExtensions }
                    .map { Track(it) }
                    .sortedBy { it.name.lowercase() }
                    .toList()
            }
        }
    }

    // ✅ ФУНКЦИИ ВЫНЕСЕНЫ НА УРОВЕНЬ App(), А НЕ ВНУТРЬ Column
    fun playNext() {
        val currentIndex = tracks.indexOfFirst { it.path == currentTrack?.path }
        if (currentIndex != -1 && currentIndex < tracks.size - 1) {
            val nextTrack = tracks[currentIndex + 1]
            currentTrack = nextTrack
            audioState.player.play(nextTrack.path)
            sliderPosition = 0f
        }
    }

    fun playPrevious() {
        val currentIndex = tracks.indexOfFirst { it.path == currentTrack?.path }
        if (currentIndex > 0) {
            val prevTrack = tracks[currentIndex - 1]
            currentTrack = prevTrack
            audioState.player.play(prevTrack.path)
            sliderPosition = 0f
        }
    }

    // Автопереход
    var autoAdvanced by remember { mutableStateOf(false) }

    LaunchedEffect(audioState.position, audioState.duration) {
        val nearEnd = audioState.duration > 0 &&
                audioState.position >= audioState.duration - 500
        if (nearEnd && playerState == AudioPlayerState.PLAYING && !autoAdvanced) {
            autoAdvanced = true
            playNext()
        } else if (!nearEnd) {
            autoAdvanced = false
        }
    }


    fun loadTracksFromFolder(folder: File) {
        if (folder.exists() && folder.isDirectory) {
            tracks = folder.walkTopDown()
                .filter { it.isFile && it.extension.lowercase() in audioExtensions }
                .map { Track(it) }
                .sortedBy { it.name.lowercase() }
                .toList()
            saveLastFolder(folder.absolutePath)
        }
    }

    val dndTarget = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val transferable = event.awtTransferable
                if (!transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    return false
                }

                val files = try {
                    (transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>)
                        ?.filterIsInstance<File>()
                        ?: emptyList()
                } catch (_: Exception) {
                    emptyList()
                }


                val file = files.firstOrNull() ?: return false

                return when {
                    file.isDirectory -> {
                        loadTracksFromFolder(file)
                        true
                    }

                    file.extension.lowercase() in audioExtensions -> {
                        currentTrack = Track(file)
                        audioState.player.play(file.absolutePath)
                        sliderPosition = 0f
                        true
                    }

                    else -> false
                }
            }
        }
    }

    // ✅ ОДИН Column, БЕЗ ВЛОЖЕННОСТИ
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .dragAndDropTarget(
                shouldStartDragAndDrop = { true },
                target = dndTarget
            )
    ) {
        // --- ПАНЕЛЬ УПРАВЛЕНИЯ (СВЕРХУ) ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = currentTrack?.name ?: "Не выбран",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )

                if (audioState.duration > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatTime(sliderPosition.toLong()),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Slider(
                            value = sliderPosition,
                            onValueChange = { sliderPosition = it },
                            onValueChangeFinished = {
                                audioState.player.seekTo(sliderPosition.toLong())
                            },
                            valueRange = 0f..audioState.duration.toFloat(),
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        Text(
                            text = formatTime(audioState.duration),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { playPrevious() },
                        enabled = tracks.isNotEmpty() && currentTrack != null
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Предыдущий")
                    }

                    IconButton(
                        onClick = {
                            when (playerState) {
                                AudioPlayerState.PLAYING -> audioState.player.pause()
                                AudioPlayerState.PAUSED -> audioState.player.play()
                                else -> currentTrack?.let { audioState.player.play(it.path) }
                            }
                        },
                        enabled = currentTrack != null
                    ) {
                        val icon = if (playerState == AudioPlayerState.PLAYING) Icons.Default.Pause
                        else Icons.Default.PlayArrow
                        Icon(icon, contentDescription = "Играть/Пауза")
                    }

                    IconButton(
                        onClick = { playNext() },
                        enabled = tracks.isNotEmpty() && currentTrack != null
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Следующий")
                    }

                    IconButton(
                        onClick = { audioState.player.stop(); sliderPosition = 0f },
                        enabled = playerState == AudioPlayerState.PLAYING
                                || playerState == AudioPlayerState.PAUSED
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Стоп")
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Громкость: ", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = volume,
                        onValueChange = { volume = it; audioState.player.setVolume(it) },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- СПИСОК ПЕСЕН (СНИЗУ) ---
        Button(
            onClick = {
                val chooser = JFileChooser().apply {
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    dialogTitle = "Выберите папку с музыкой"
                }
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    loadTracksFromFolder(chooser.selectedFile)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Выбрать папку")
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(tracks) { track ->
                val isSelected = currentTrack?.path == track.path
                Text(
                    text = track.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            currentTrack = track
                            audioState.player.play(track.path)
                            sliderPosition = 0f
                        }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    style = if (isSelected) MaterialTheme.typography.bodyLarge
                    else MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}