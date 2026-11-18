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

@OptIn(ExperimentalMaterial3Api::class)
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
    onVolumeChange: (Float) -> Unit,
    onChooseFolder: () -> Unit
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
            // ЛЕВАЯ ЧАСТЬ
            Column(modifier = Modifier.weight(1f)) {
                // Верхняя строка: иконка папки + название трека
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Кнопка выбора папки с подсказкой
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Above
                        ),
                        tooltip = {
                            PlainTooltip {
                                Text("Выбрать папку")
                            }
                        },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = onChooseFolder) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Выбрать папку",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = currentTrackName ?: "Не выбран",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                }

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