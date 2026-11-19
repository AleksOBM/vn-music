/**
 * @fileName App.kt
 */
package io.github.aleksobm.vnmusic

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.unit.dp
import io.github.aleksobm.vnmusic.controls.PlayerControls
import io.github.aleksobm.vnmusic.tabs.PlaylistTab
import io.github.aleksobm.vnmusic.tabs.PlaylistTabs
import io.github.aleksobm.vnmusic.tabs.loadTabs
import io.github.aleksobm.vnmusic.tabs.saveTabs
import io.github.aleksobm.vnmusic.track.Track
import io.github.aleksobm.vnmusic.track.TrackList
import io.github.aleksobm.vnmusic.track.audioExtensions
import io.github.aleksobm.vnmusic.track.loadTracksFromFolder
import io.github.kdroidfilter.composemediaplayer.audio.AudioPlayerState
import io.github.kdroidfilter.composemediaplayer.audio.rememberAudioPlayerLiveState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.io.InputStream
import java.io.Reader
import java.net.URL
import kotlin.time.Duration.Companion.milliseconds

private enum class PlaybackMode {
    REPEAT_PLAYLIST,
    REPEAT_TRACK,
    SHUFFLE
}

private data class DroppedMedia(
    val files: List<File>,
    val urls: List<String>
)

@OptIn(ExperimentalComposeUiApi::class)
private fun extractDroppedMedia(event: DragAndDropEvent): DroppedMedia {
    val transferable = event.awtTransferable
    val files = if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        runCatching {
            (transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>)
                ?.filterIsInstance<File>()
                .orEmpty()
        }.getOrDefault(emptyList())
    } else {
        emptyList()
    }

    val urlRegex = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
    val urls = transferable.transferDataFlavors
        .filter { flavor ->
            flavor.isMimeTypeEqual("text/uri-list") ||
                    flavor.isMimeTypeEqual("text/plain") ||
                    flavor.isMimeTypeEqual("text/x-moz-url") ||
                    flavor.isMimeTypeEqual("application/x-moz-url") ||
                    flavor.isMimeTypeEqual("application/x-java-url")
        }
        .flatMap { flavor ->
            val text = runCatching {
                when (val value = transferable.getTransferData(flavor)) {
                    is String -> value
                    is URL -> value.toExternalForm()
                    is InputStream -> value.bufferedReader().use { it.readText() }
                    is Reader -> value.use { it.readText() }
                    else -> value.toString()
                }
            }.getOrNull().orEmpty()
            urlRegex.findAll(text).map { match ->
                match.value.trimEnd('.', ',', ';', ')', ']', '}')
            }.toList()
        }
        .distinct()

    return DroppedMedia(files, urls)
}

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
    var selectedTrack by remember { mutableStateOf<Track?>(null) }
    var sliderPosition by remember { mutableStateOf(0f) }
    var playbackMode by remember { mutableStateOf(PlaybackMode.REPEAT_PLAYLIST) }
    var manuallyStopped by remember { mutableStateOf(false) }
    var awaitingTrackRestart by remember { mutableStateOf(false) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekDuration by remember { mutableStateOf(0L) }
    val seekScope = rememberCoroutineScope()
    val playerState = audioState.state
    val isSeekingState = rememberUpdatedState(isSeeking)
    val awaitingTrackRestartState = rememberUpdatedState(awaitingTrackRestart)

    val activeTab = tabs.getOrNull(activeTabIndex)

    fun playTrack(track: Track) {
        val isSameTrack = currentTrack?.path == track.path
        currentTrack = track
        selectedTrack = track
        sliderPosition = 0f
        if (!isSameTrack) {
            seekDuration = 0L
        }
        awaitingTrackRestart = true
        isSeeking = false
        manuallyStopped = false
        audioState.player.play(track.path)
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
                var attempts = 0
                while (attempts < 30) {
                    val state = audioState.player.currentPlayerState()
                    if (state == AudioPlayerState.PLAYING || state == AudioPlayerState.PAUSED) {
                        break
                    }
                    delay(100.milliseconds)
                    attempts++
                }
                audioState.player.seekTo(target)
                delay(80.milliseconds)
                audioState.player.seekTo(target)
                if (wasPaused) {
                    audioState.player.pause()
                } else {
                    audioState.player.play()
                }
            } else {
                audioState.player.seekTo(target)
                delay(80.milliseconds)
                audioState.player.seekTo(target)
            }
            sliderPosition = target.toFloat()
            delay(120.milliseconds)
            isSeeking = false
        }
    }

    fun playNext() {
        val tracks = activeTab?.tracks ?: return
        if (playbackMode == PlaybackMode.SHUFFLE) {
            val candidates = tracks.filter { it.path != currentTrack?.path }.ifEmpty { tracks }
            candidates.randomOrNull()?.let(::playTrack)
            return
        }
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

    LaunchedEffect(currentTrack?.path, activeTab?.id, playbackMode, manuallyStopped) {
        var playbackStarted = false
        var endHandled = false
        while (true) {
            val state = audioState.player.currentPlayerState()
            val position = audioState.player.currentPosition() ?: 0L
            val duration = audioState.player.currentDuration() ?: 0L

            if (duration > 0) {
                seekDuration = duration
            }
            if (state == AudioPlayerState.PLAYING) {
                playbackStarted = true
                if (awaitingTrackRestartState.value && position <= 1_000L) {
                    sliderPosition = position.toFloat()
                    awaitingTrackRestart = false
                } else if (!awaitingTrackRestartState.value && !isSeekingState.value) {
                    sliderPosition = position.toFloat()
                }
                if (duration > 0 && position < duration - 1_000) {
                    endHandled = false
                }
            } else if (state == AudioPlayerState.PAUSED &&
                !awaitingTrackRestartState.value && !isSeekingState.value
            ) {
                sliderPosition = position.toFloat()
            }

            val reachedEnd = duration > 0 && position >= duration - 150
            val playbackFinished = state == AudioPlayerState.IDLE && playbackStarted
            if (!manuallyStopped && playbackStarted && !endHandled &&
                (reachedEnd || playbackFinished)
            ) {
                endHandled = true
                val tracks = activeTab?.tracks.orEmpty()
                val currentIndex = tracks.indexOfFirst { it.path == currentTrack?.path }
                val nextTrack = when {
                    playbackMode == PlaybackMode.REPEAT_TRACK -> currentTrack
                    playbackMode == PlaybackMode.SHUFFLE ->
                        tracks.filter { it.path != currentTrack?.path }.ifEmpty { tracks }.randomOrNull()

                    currentIndex >= 0 && currentIndex < tracks.lastIndex -> tracks[currentIndex + 1]
                    playbackMode == PlaybackMode.REPEAT_PLAYLIST -> tracks.firstOrNull()
                    else -> null
                }
                if (nextTrack != null) {
                    playbackStarted = false
                    if (playbackMode == PlaybackMode.REPEAT_TRACK) {
                        sliderPosition = 0f
                        awaitingTrackRestart = true
                    }
                    playTrack(nextTrack)
                }
            }
            delay(100.milliseconds)
        }
    }

    fun nextPlaylistNumber(): Int = tabs
        .mapNotNull { tab ->
            Regex("""New playlist (\d+)""")
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
                title = "New playlist $playlistNumber"
            )
        }
        if (newTab != null) {
            tabs = if (addToStart) listOf(newTab) + tabs else tabs + newTab
            activeTabIndex = if (addToStart) 0 else tabs.size - 1
        }
    }

    fun addMediaAsPlaylist(files: List<File>, urls: List<String>) {
        val locations = files
            .filter { it.isFile && it.extension.lowercase() in audioExtensions }
            .map { it.absolutePath } + urls
        val newTab = PlaylistTab.fromLocations(
            locations = locations,
            title = "New playlist ${nextPlaylistNumber()}"
        )
        if (newTab != null) {
            tabs = listOf(newTab) + tabs
            activeTabIndex = 0
        }
    }

    fun addMediaToActivePlaylist(files: List<File>, urls: List<String>): Boolean {
        val tab = tabs.getOrNull(activeTabIndex) ?: return false
        val droppedTracks = files.flatMap { file ->
            if (file.isDirectory) {
                loadTracksFromFolder(file).orEmpty()
            } else if (file.isFile && file.extension.lowercase() in audioExtensions) {
                listOf(Track(file))
            } else {
                emptyList()
            }
        } + urls.mapNotNull { Track.fromUrl(it) }
        if (droppedTracks.isEmpty()) return false

        val tracks = tab.tracks.toMutableList()
        val paths = tracks.mapTo(mutableSetOf()) { it.path }
        var existingTrack: Track? = null
        for (droppedTrack in droppedTracks) {
            val path = droppedTrack.path
            val duplicateIndex = tracks.indexOfFirst { it.path == path }
            if (duplicateIndex >= 0) {
                if (existingTrack == null) existingTrack = tracks[duplicateIndex]
            } else if (paths.add(path)) {
                tracks += droppedTrack
            }
        }

        if (tracks.size != tab.tracks.size) {
            val updatedTab = tab.copy(
                folderPath = null,
                filePath = tracks.firstOrNull()?.path,
                filePaths = tracks.map { it.path },
                tracks = tracks
            )
            tabs = tabs.toMutableList().also { it[activeTabIndex] = updatedTab }
        }
        existingTrack?.let { selectedTrack = it }
        return true
    }

    fun removeTrackFromActivePlaylist(track: Track) {
        val tab = tabs.getOrNull(activeTabIndex) ?: return
        val remainingTracks = tab.tracks.filterNot { it.path == track.path }
        if (remainingTracks.size == tab.tracks.size) return

        tabs = tabs.toMutableList().also { updatedTabs ->
            updatedTabs[activeTabIndex] = tab.copy(
                folderPath = null,
                filePath = remainingTracks.firstOrNull()?.path,
                filePaths = remainingTracks.map { it.path },
                tracks = remainingTracks
            )
        }
        if (selectedTrack?.path == track.path) {
            selectedTrack = null
        }
    }

    val tabsDropHandler = rememberUpdatedState(newValue = { media: DroppedMedia ->
        val droppedFolders = media.files.filter { it.isDirectory }
        droppedFolders.asReversed().forEach { addTabFromFile(it, addToStart = true) }

        val droppedFiles = media.files.filter { it.isFile }
        if (droppedFiles.isNotEmpty() || media.urls.isNotEmpty()) {
            addMediaAsPlaylist(droppedFiles, media.urls)
        }
        droppedFolders.isNotEmpty() || droppedFiles.isNotEmpty() || media.urls.isNotEmpty()
    })
    val tracksDropHandler = rememberUpdatedState(newValue = { media: DroppedMedia ->
        addMediaToActivePlaylist(media.files, media.urls)
    })
    val tabsDropTarget = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val media = extractDroppedMedia(event)
                return tabsDropHandler.value(media)
            }
        }
    }
    val tracksDropTarget = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val media = extractDroppedMedia(event)
                return tracksDropHandler.value(media)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Панель управления
        PlayerControls(
            currentTrackName = currentTrack?.name,
            duration = seekDuration,
            position = sliderPosition,
            playerState = playerState,
            volume = volume,
            isRadio = currentTrack?.streamUrl != null,
            shuffleEnabled = playbackMode == PlaybackMode.SHUFFLE,
            repeatTrack = playbackMode == PlaybackMode.REPEAT_TRACK,
            repeatPlaylist = playbackMode == PlaybackMode.REPEAT_PLAYLIST,
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
                    else -> currentTrack?.let { playTrack(it) }
                }
            },
            onNext = { playNext() },
            onPrevious = { playPrevious() },
            onStop = {
                manuallyStopped = true
                audioState.player.stop()
                sliderPosition = 0f
            },
            onVolumeChange = { value ->
                volume = value
                audioState.player.setVolume(value)
            },
            onToggleShuffle = { playbackMode = PlaybackMode.SHUFFLE },
            onToggleRepeatTrack = { playbackMode = PlaybackMode.REPEAT_TRACK },
            onToggleRepeatPlaylist = { playbackMode = PlaybackMode.REPEAT_PLAYLIST }
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { true },
                    target = tabsDropTarget
                ),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            if (tabs.isNotEmpty()) {
                PlaylistTabs(
                    tabs = tabs,
                    activeTabIndex = activeTabIndex,
                    onTabSelected = { activeTabIndex = it },
                    onTabClosed = { index ->
                        tabs = tabs.filterIndexed { i, _ -> i != index }
                        if (activeTabIndex >= tabs.size) {
                            activeTabIndex = tabs.size - 1
                        } else if (index < activeTabIndex) {
                            activeTabIndex--
                        }
                    },
                    onTabRenamed = { index, title ->
                        tabs = tabs.mapIndexed { i, tab ->
                            if (i == index) tab.copy(title = title) else tab
                        }
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Drop folders or files here",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { true },
                    target = tracksDropTarget
                ),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            TrackList(
                tracks = activeTab?.tracks ?: emptyList(),
                selectedTrack = selectedTrack,
                onTrackClick = { playTrack(it) },
                onTrackRemove = ::removeTrackFromActivePlaylist
            )
        }
    }
}