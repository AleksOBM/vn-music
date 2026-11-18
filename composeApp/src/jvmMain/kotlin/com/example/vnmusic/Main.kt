/**
 * @fileName Main.kt
 */
package com.example.vnmusic

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension

// --- НОВЫЕ ИМПОРТЫ ---
import org.jetbrains.compose.resources.painterResource
import vnmusic.composeapp.generated.resources.Res
import vnmusic.composeapp.generated.resources.app_icon

fun main() = application {
    val windowState = rememberWindowState(width = 420.dp, height = 700.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "VN Music",
        state = windowState,
        // Используем сгенерированный ресурс
        icon = painterResource(Res.drawable.app_icon)
    ) {
        window.minimumSize = Dimension(350, 500)

        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                App()
            }
        }
    }
}
