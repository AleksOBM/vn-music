package com.example.vnmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun VerticalVolumeSlider(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var localVolume by remember { mutableStateOf(volume) }

    // НОВОЕ: Храним последнее ненулевое значение громкости
    var lastNonZeroVolume by remember { mutableStateOf(if (volume > 0f) volume else 1f) }

    LaunchedEffect(volume, isDragging) {
        if (!isDragging) {
            localVolume = volume
            // Если громкость стала больше нуля (не через кнопку Mute), обновляем lastNonZeroVolume
            if (volume > 0f) {
                lastNonZeroVolume = volume
            }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(80.dp) // Уменьшили высоту
                .pointerInput(true) {
                    detectVerticalDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            // Инвертируем: верх — 1.0, низ — 0.0
                            val newVolume = (1f - (change.position.y / size.height)).coerceIn(0f, 1f)
                            localVolume = newVolume
                            onVolumeChange(newVolume)
                            // Обновляем lastNonZeroVolume при перетаскивании
                            if (newVolume > 0f) {
                                lastNonZeroVolume = newVolume
                            }
                        }
                    )
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            // Фон
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.LightGray.copy(alpha = 0.3f))
            )
            // Заполнение
            Box(
                modifier = Modifier
                    .fillMaxHeight(localVolume)
                    .width(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            // Бегунок
            Box(
                modifier = Modifier
                    .fillMaxHeight(localVolume)
                    .wrapContentHeight(Alignment.Top)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp)) // Уменьшили отступ

        // Кнопка Mute
        IconButton(
            onClick = {
                if (localVolume > 0f) {
                    // Выключаем звук, запоминая текущее значение
                    lastNonZeroVolume = localVolume
                    onVolumeChange(0f)
                } else {
                    // Включаем звук, восстанавливая предыдущее значение
                    onVolumeChange(lastNonZeroVolume)
                }
            }
        ) {
            Icon(
                imageVector = if (localVolume > 0f) Icons.AutoMirrored.Filled.VolumeUp
                else Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = if (localVolume > 0f) "Mute" else "Unmute",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}