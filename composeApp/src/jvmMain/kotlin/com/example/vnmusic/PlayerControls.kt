/**
 * @fileName PlayerControls.kt
 */
package com.example.vnmusic

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.composemediaplayer.audio.AudioPlayerState

@Composable
fun PlayerControls(
    currentTrackName: String?,
    duration: Long,
    position: Float,
    playerState: AudioPlayerState,
    volume: Float,
    hasTracks: Boolean,
    onSeek: (Long) -> Unit,
    onPositionChange: (Float) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onStop: () -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    val isPlaying = playerState == AudioPlayerState.PLAYING
    val isActive = isPlaying || playerState == AudioPlayerState.PAUSED

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = currentTrackName ?: "Не выбран",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )

            if (duration > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatTime(position.toLong()),
                        style = MaterialTheme.typography.bodySmall
                    )
                    CustomSlider(
                        value = position,
                        onValueChange = onPositionChange,
                        onValueChangeFinished = { newValue -> onSeek(newValue.toLong()) },
                        valueRange = 0f..duration.toFloat(),
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )

                    Text(
                        text = formatTime(duration),
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
                    onClick = onPrevious,
                    enabled = hasTracks && currentTrackName != null
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Предыдущий")
                }

                IconButton(onClick = onPlayPause, enabled = currentTrackName != null) {
                    val icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow
                    Icon(icon, contentDescription = "Играть/Пауза")
                }

                IconButton(onClick = onNext, enabled = hasTracks && currentTrackName != null) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Следующий")
                }

                IconButton(onClick = onStop, enabled = isActive) {
                    Icon(Icons.Default.Stop, contentDescription = "Стоп")
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Громкость: ", style = MaterialTheme.typography.bodySmall)
                CustomSlider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    onValueChangeFinished = { /* ничего не делаем */ },
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
            }
        }
    }
}
