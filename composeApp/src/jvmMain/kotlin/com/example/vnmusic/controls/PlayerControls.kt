/**
 * @fileName PlayerControls.kt
 */
package com.example.vnmusic.controls

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.vnmusic.sliders.CustomSlider
import com.example.vnmusic.sliders.VerticalVolumeSlider
import com.example.vnmusic.util.formatTime
import io.github.kdroidfilter.composemediaplayer.audio.AudioPlayerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerControls(
    currentTrackName: String?,
    duration: Long,
    position: Float,
    playerState: AudioPlayerState,
    volume: Float,
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = onToggleShuffle,
                    enabled = hasTracks
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Случайный порядок",
                        tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onToggleRepeatTrack,
                    enabled = currentTrackName != null
                ) {
                    Icon(
                        imageVector = Icons.Default.RepeatOne,
                        contentDescription = "Повторять трек",
                        tint = if (repeatTrack) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onToggleRepeatPlaylist,
                    enabled = hasTracks
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Повторять плейлист",
                        tint = if (repeatPlaylist) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Единый блок названия трека, перемотки и кнопок без отдельной рамки.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentTrackName ?: "Не выбран",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Слайдер перемотки (показываем всегда)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatTime(position.toLong()),
                        style = MaterialTheme.typography.bodySmall
                    )
                    CustomSlider(
                        value = position,
                        onValueChange = onPositionChange,
                        onValueChangeFinished = { onSeek(it.toLong()) },
                        valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        enabled = duration > 0
                    )
                    Text(
                        text = formatTime(duration),
                        style = MaterialTheme.typography.bodySmall
                    )
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
}