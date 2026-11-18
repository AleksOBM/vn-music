/**
 * @fileName App.kt
 */
package com.example.vnmusic

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.composemediaplayer.audio.AudioPlayerState
import io.github.kdroidfilter.composemediaplayer.audio.rememberAudioPlayerLiveState
import java.awt.datatransfer.DataFlavor
import java.io.File
import javax.swing.JFileChooser

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App() {
    val audioState = rememberAudioPlayerLiveState()

    var volume by remember { mutableStateOf(1f) }
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var currentTrack by remember { mutableStateOf<Track?>(null) }
    var sliderPosition by remember { mutableStateOf(0f) }
    var autoAdvanced by remember { mutableStateOf(false) }

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
            loadTracksFromFolder(File(lastPath))?.let { loaded ->
                tracks = loaded
            }
        }
    }

    fun playTrack(track: Track) {
        currentTrack = track
        audioState.player.play(track.path)
        sliderPosition = 0f
        autoAdvanced = false
    }

    fun playNext() {
        val currentIndex = tracks.indexOfFirst { it.path == currentTrack?.path }
        if (currentIndex != -1 && currentIndex < tracks.size - 1) {
            playTrack(tracks[currentIndex + 1])
        }
    }

    fun playPrevious() {
        val currentIndex = tracks.indexOfFirst { it.path == currentTrack?.path }
        if (currentIndex > 0) {
            playTrack(tracks[currentIndex - 1])
        }
    }

    // Автопереход с защитой от двойного срабатывания
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

    fun chooseAndLoadFolder() {
        val chooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = "Выберите папку с музыкой"
        }
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            loadTracksFromFolder(chooser.selectedFile)?.let { loaded ->
                tracks = loaded
                saveLastFolder(chooser.selectedFile.absolutePath)
            }
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
                        loadTracksFromFolder(file)?.let { loaded ->
                            tracks = loaded
                            saveLastFolder(file.absolutePath)
                        }
                        true
                    }

                    file.extension.lowercase() in audioExtensions -> {
                        playTrack(Track(file))
                        true
                    }

                    else -> false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .dragAndDropTarget(
                shouldStartDragAndDrop = { true },
                target = dndTarget
            )
    ) {
        PlayerControls(
            currentTrackName = currentTrack?.name,
            duration = audioState.duration,
            position = sliderPosition,
            playerState = playerState,
            volume = volume,
            hasTracks = tracks.isNotEmpty(),
            onSeek = { ms -> audioState.player.seekTo(ms) },
            onPositionChange = { sliderPosition = it },
            onPlayPause = {
                when (playerState) {
                    AudioPlayerState.PLAYING -> audioState.player.pause()
                    AudioPlayerState.PAUSED -> audioState.player.play()
                    else -> currentTrack?.let { audioState.player.play(it.path) }
                }
            },
            onNext = { playNext() },
            onPrevious = { playPrevious() },
            onStop = {
                audioState.player.stop()
                sliderPosition = 0f
            },
            onVolumeChange = { value ->
                volume = value
                audioState.player.setVolume(value)
            },
            onChooseFolder = { chooseAndLoadFolder() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TrackList(
            tracks = tracks,
            currentTrack = currentTrack,
            onTrackClick = { playTrack(it) }
        )
    }
}