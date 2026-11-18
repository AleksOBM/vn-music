/**
 * @fileName TimeUtils.kt
 */
package com.example.vnmusic

/**
 * Форматирует миллисекунды в строку формата "мм:сс" (например, 125000 -> "02:05").
 * Если значение меньше или равно нулю, возвращает "00:00".
 */
fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

/**
 * Форматирует миллисекунды в строку формата "ч:мм:сс", если длительность больше часа.
 * Полезно для длинных подкастов или аудиокниг.
 */
fun formatTimeLong(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}