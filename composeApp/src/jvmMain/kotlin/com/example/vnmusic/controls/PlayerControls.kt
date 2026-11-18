/**
 * @fileName PlayerControls.kt
 */
package com.example.vnmusic.controls

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.vnmusic.sliders.CustomSlider
import com.example.vnmusic.sliders.VerticalVolumeSlider
import com.example.vnmusic.util.formatTime
import com.example.vnmusic.util.formatTimeLong
import io.github.kdroidfilter.composemediaplayer.audio.AudioPlayerState

@Composable
fun PlayerControls(
    currentTrackName: String?,
    duration: Long,
    position: Float,
    playerState: AudioPlayerState,
    volume: Float,
    isRadio: Boolean,
    shuffleEnabled: Boolean,
    repeatTrack: Boolean,
    repeatPlaylist: Boolean,
    hasTracks: Boolean,
    onSeek: (Long) -> Unit,
    onPositionChange: (Float) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onStop: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeatTrack: () -> Unit,
    onToggleRepeatPlaylist: () -> Unit
) {
    val isPlaying = playerState == AudioPlayerState.PLAYING
    val isActive = isPlaying || playerState == AudioPlayerState.PAUSED
    val showRecordingOptions = remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = onToggleShuffle,
                        enabled = hasTracks,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onToggleRepeatTrack,
                        enabled = currentTrackName != null,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RepeatOne,
                            contentDescription = "Repeat track",
                            tint = if (repeatTrack) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onToggleRepeatPlaylist,
                        enabled = hasTracks,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Repeat playlist",
                            tint = if (repeatPlaylist) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Единый блок названия трека, перемотки и кнопок без отдельной рамки.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentTrackName ?: "No track selected",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Слайдер перемотки (показываем всегда)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatTime(position.toLong()),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isRadio) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.onSurface
                    )
                    CustomSlider(
                        value = if (isRadio) 0f else position,
                        onValueChange = onPositionChange,
                        onValueChangeFinished = { onSeek(it.toLong()) },
                        valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        enabled = duration > 0 && !isRadio
                    )
                    if (isRadio) {
                        Text(
                            text = "∞",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    } else {
                        Text(
                            text = formatTimeLong(duration),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Кнопки управления
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPrevious,
                        enabled = hasTracks && currentTrackName != null
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous track")
                    }

                    IconButton(onClick = onPlayPause, enabled = currentTrackName != null) {
                        val icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow
                        Icon(icon, contentDescription = "Play/Pause")
                    }

                    IconButton(onClick = onNext, enabled = hasTracks && currentTrackName != null) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next track")
                    }

                    IconButton(onClick = onStop, enabled = isActive) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop")
                    }

                    IconButton(
                        onClick = { showRecordingOptions.value = true },
                        enabled = isRadio && isPlaying
                    ) {
                        Icon(
                            imageVector = Icons.Default.FiberManualRecord,
                            contentDescription = "Record radio",
                            tint = if (isRadio && isPlaying) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        )
                    }
                }
            }

            // ПРАВАЯ ЧАСТЬ: Вертикальная громкость
            Spacer(modifier = Modifier.width(8.dp))
            VerticalVolumeSlider(
                volume = volume,
                onVolumeChange = onVolumeChange,
                modifier = Modifier.width(32.dp)
            )
        }
    }

    if (showRecordingOptions.value) {
        AlertDialog(
            onDismissRequest = { showRecordingOptions.value = false },
            title = { Text("Radio recording") },
            text = {
                Text(
                    "Recording is not implemented yet. FFmpeg is required to capture and encode the stream."
                )
            },
            confirmButton = {
                TextButton(onClick = { showRecordingOptions.value = false }) {
                    Text("Close")
                }
            }
        )
    }
}