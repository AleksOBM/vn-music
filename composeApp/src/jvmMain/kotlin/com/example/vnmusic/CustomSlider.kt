/**
 * @fileName CustomSlider.kt
 */
package com.example.vnmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun CustomSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (Float) -> Unit, // <-- ИЗМЕНЕНО
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val range = valueRange.endInclusive - valueRange.start
    val fraction = if (range > 0f) ((value - valueRange.start) / range).coerceIn(0f, 1f) else 0f

    var isDragging by remember { mutableStateOf(false) }
    var localFraction by remember { mutableStateOf(fraction) }

    LaunchedEffect(fraction, isDragging) {
        if (!isDragging) {
            localFraction = fraction
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val newFraction = (offset.x / size.width).coerceIn(0f, 1f)
                    localFraction = newFraction
                    val newValue = valueRange.start + newFraction * range
                    onValueChange(newValue)
                    onValueChangeFinished(newValue) // <-- передаём значение
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        localFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onValueChange(valueRange.start + localFraction * range)
                    },
                    onDragEnd = {
                        isDragging = false
                        onValueChangeFinished(valueRange.start + localFraction * range) // <-- передаём значение
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        localFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        onValueChange(valueRange.start + localFraction * range)
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Фоновая дорожка
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.LightGray.copy(alpha = 0.4f))
        )

        // Активная дорожка
        Box(
            modifier = Modifier
                .fillMaxWidth(localFraction)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF6750A4))
        )

        // Бегунок
        Box(
            modifier = Modifier
                .fillMaxWidth(localFraction)
                .wrapContentWidth(Alignment.End)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6750A4))
            )
        }
    }
}