package com.example.vnmusic

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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

    var tabs by remember { mutableStateOf<List<PlaylistTab>>(emptyList()) }
    var activeTabIndex by remember { mutableStateOf(-1) }

    LaunchedEffect(Unit) {
        val loadedTabs = loadTabs()
        if (loadedTabs.isNotEmpty()) {
            tabs = loadedTabs
            activeTabIndex = 0
        }
    }

    LaunchedEffect(tabs) {
        saveTabs(tabs)
    }

    var volume by remember { mutableStateOf(1f) }
    var currentTrack by remember { mutableStateOf<Track?>(null) }
    var sliderPosition by remember { mutableStateOf(0f) }
    var autoAdvanced by remember { mutableStateOf(false) }
    val playerState = audioState.state

    val activeTab = tabs.getOrNull(activeTabIndex)

    LaunchedEffect(audioState.position) {
        if (playerState == AudioPlayerState.PLAYING || playerState == AudioPlayerState.PAUSED) {
            sliderPosition = audioState.position.toFloat()
        }
    }

    fun playTrack(track: Track) {
        currentTrack = track
        audioState.player.play(track.path)
        sliderPosition = 0f
        autoAdvanced = false
    }

    fun playNext() {
        val tracks = activeTab?.tracks ?: return
        val currentIndex = tracks.indexOfFirst { it.path == currentTrack?.path }
        if (currentIndex != -1 && currentIndex < tracks.size - 1) {
            playTrack(tracks[currentIndex + 1])
        }
    }

    fun playPrevious() {
        val tracks = activeTab?.tracks ?: return
        val currentIndex = tracks.indexOfFirst { it.path == currentTrack?.path }
        if (currentIndex > 0) {
            playTrack(tracks[currentIndex - 1])
        }
    }

    LaunchedEffect(audioState.position, audioState.duration) {
        val nearEnd = audioState.duration > 0 && audioState.position >= audioState.duration - 500
        if (nearEnd && playerState == AudioPlayerState.PLAYING && !autoAdvanced) {
            autoAdvanced = true
            playNext()
        } else if (!nearEnd) {
            autoAdvanced = false
        }
    }

    fun addTabFromFile(file: File) {
        val newTab = if (file.isDirectory) PlaylistTab.fromFolder(file)
        else PlaylistTab.fromFile(file)
        if (newTab != null) {
            tabs = tabs + newTab
            activeTabIndex = tabs.size - 1
        }
    }

    fun chooseAndLoadFolder() {
        val chooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
            dialogTitle = "Выберите папку или файл"
        }
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            addTabFromFile(chooser.selectedFile)
        }
    }

    val dndTarget = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val transferable = event.awtTransferable
                if (!transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return false
                val files = try {
                    (transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>)
                        ?.filterIsInstance<File>() ?: emptyList()
                } catch (_: Exception) { emptyList() }
                val file = files.firstOrNull() ?: return false
                addTabFromFile(file)
                return true
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
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Панель управления
        PlayerControls(
            currentTrackName = currentTrack?.name,
            duration = audioState.duration,
            position = sliderPosition,
            playerState = playerState,
            volume = volume,
            hasTracks = activeTab?.tracks?.isNotEmpty() == true,
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

        // Рамка для вкладок и списка песен
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Вкладки
                PlaylistTabs(
                    tabs = tabs,
                    activeTabIndex = activeTabIndex,
                    onTabSelected = { activeTabIndex = it },
                    onTabClosed = { index ->
                        tabs = tabs.filterIndexed { i, _ -> i != index }
                        if (activeTabIndex >= tabs.size) {
                            activeTabIndex = tabs.size - 1
                        }
                    }
                )

                // Разделитель между вкладками и списком песен
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 15.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Список треков активной вкладки
                TrackList(
                    tracks = activeTab?.tracks ?: emptyList(),
                    currentTrack = currentTrack,
                    onTrackClick = { playTrack(it) }
                )
            }
        }
    }
}