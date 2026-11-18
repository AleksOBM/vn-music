package com.example.vnmusic

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.unit.dp
import com.example.vnmusic.controls.PlayerControls
import com.example.vnmusic.track.Track
import com.example.vnmusic.track.TrackList
import com.example.vnmusic.tabs.PlaylistTab
import com.example.vnmusic.tabs.PlaylistTabs
import com.example.vnmusic.tabs.loadTabs
import com.example.vnmusic.tabs.saveTabs
import io.github.kdroidfilter.composemediaplayer.audio.AudioPlayerState
import io.github.kdroidfilter.composemediaplayer.audio.rememberAudioPlayerLiveState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    var isSeeking by remember { mutableStateOf(false) }
    var seekDuration by remember { mutableStateOf(0L) }
    val seekScope = rememberCoroutineScope()
    val playerState = audioState.state

    val activeTab = tabs.getOrNull(activeTabIndex)

    LaunchedEffect(audioState.position) {
        if (!isSeeking && (playerState == AudioPlayerState.PLAYING || playerState == AudioPlayerState.PAUSED)) {
            sliderPosition = audioState.position.toFloat()
        }
    }

    LaunchedEffect(audioState.duration) {
        if (audioState.duration > 0) {
            seekDuration = audioState.duration
        }
    }

    fun playTrack(track: Track) {
        currentTrack = track
        audioState.player.play(track.path)
        sliderPosition = 0f
        seekDuration = 0L
        isSeeking = false
        autoAdvanced = false
    }

    fun seekTrack(ms: Long) {
        val track = currentTrack ?: return
        val duration = if (seekDuration > 0) seekDuration else audioState.duration
        val target = ms.coerceIn(0L, if (duration > 0) duration else ms)
        val wasPaused = audioState.player.currentPlayerState() == AudioPlayerState.PAUSED

        sliderPosition = target.toFloat()
        isSeeking = true

        seekScope.launch {
            if (track.path.substringAfterLast('.', "").lowercase() == "flac") {
                audioState.player.stop()
                audioState.player.play(track.path)
                for (attempt in 0 until 30) {
                    val state = audioState.player.currentPlayerState()
                    if (state == AudioPlayerState.PLAYING || state == AudioPlayerState.PAUSED) {
                        break
                    }
                    delay(100)
                }
                audioState.player.seekTo(target)
                delay(80)
                audioState.player.seekTo(target)
                if (wasPaused) {
                    audioState.player.pause()
                } else {
                    audioState.player.play()
                }
            } else {
                audioState.player.seekTo(target)
                delay(80)
                audioState.player.seekTo(target)
            }
            sliderPosition = target.toFloat()
            delay(120)
            isSeeking = false
        }
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

    fun nextPlaylistNumber(): Int = tabs
        .mapNotNull { tab ->
            Regex("""Новый плейлист (\d+)""")
                .matchEntire(tab.title)
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull()
        }
        .maxOrNull()
        ?.plus(1)
        ?: 1

    fun addTabFromFile(file: File, addToStart: Boolean = false) {
        val newTab = if (file.isDirectory) {
            PlaylistTab.fromFolder(file)
        } else {
            val playlistNumber = nextPlaylistNumber()

            PlaylistTab.fromFile(file)?.copy(
                title = "Новый плейлист $playlistNumber"
            )
        }
        if (newTab != null) {
            tabs = if (addToStart) listOf(newTab) + tabs else tabs + newTab
            activeTabIndex = if (addToStart) 0 else tabs.size - 1
        }
    }

    fun addFilesAsPlaylist(files: List<File>) {
        val newTab = PlaylistTab.fromFiles(
            files = files,
            title = "Новый плейлист ${nextPlaylistNumber()}"
        )
        if (newTab != null) {
            tabs = listOf(newTab) + tabs
            activeTabIndex = 0
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
                val droppedFolders = files.filter { it.isDirectory }
                droppedFolders.asReversed().forEach { addTabFromFile(it, addToStart = true) }

                val droppedFiles = files.filter { it.isFile }
                if (droppedFiles.isNotEmpty()) {
                    addFilesAsPlaylist(droppedFiles)
                }
                if (droppedFolders.isEmpty() && droppedFiles.isEmpty()) return false
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
            duration = seekDuration,
            position = sliderPosition,
            playerState = playerState,
            volume = volume,
            hasTracks = activeTab?.tracks?.isNotEmpty() == true,
            onSeek = ::seekTrack,
            onPositionChange = { position ->
                isSeeking = true
                sliderPosition = position
            },
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

                // Эффектный разделитель между вкладками и списком песен
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        thickness = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    )
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(8.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        tonalElevation = 4.dp
                    ) {}
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        thickness = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    )
                }

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