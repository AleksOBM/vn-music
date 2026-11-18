/**
 * @fileName Main.kt
 */
package com.example.vnmusic

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import vnmusic.composeapp.generated.resources.Res
import vnmusic.composeapp.generated.resources.app_icon
import java.awt.Dimension
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

private fun gSettingsValue(schema: String, key: String): String? = try {
    ProcessBuilder("gsettings", "get", schema, key)
        .redirectErrorStream(true)
        .start()
        .inputStream
        .bufferedReader()
        .use { it.readText() }
        .trim()
        .removePrefix("'")
        .removeSuffix("'")
        .takeIf { it.isNotEmpty() }
} catch (_: Exception) {
    null
}

private fun isWindowsDarkTheme(): Boolean? {
    val key = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize"
    for (setting in listOf("AppsUseLightTheme", "SystemUsesLightTheme")) {
        val process = try {
            ProcessBuilder("reg", "query", key, "/v", setting)
                .redirectErrorStream(true)
                .start()
        } catch (_: Exception) {
            return null
        }

        if (!process.waitFor(2, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return null
        }

        val output = process.inputStream.bufferedReader().use { it.readText() }
        val lightThemeValue = Regex(
            """$setting\s+REG_DWORD\s+0x([0-9a-f]+)""",
            RegexOption.IGNORE_CASE
        ).find(output)?.groupValues?.get(1)?.toLongOrNull(16)
        if (lightThemeValue != null) return lightThemeValue == 0L
    }
    return null
}

private fun isSystemDarkTheme(): Boolean {
    if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
        return isWindowsDarkTheme() ?: false
    }

    val colorScheme = gSettingsValue(
        "org.gnome.desktop.interface",
        "color-scheme"
    )?.lowercase()
    if (colorScheme != null) {
        if (colorScheme.contains("dark")) return true
        if (colorScheme.contains("light")) return false
    }

    val gtkTheme = gSettingsValue(
        "org.gnome.desktop.interface",
        "gtk-theme"
    )?.lowercase()
    if (gtkTheme != null) {
        if (gtkTheme.contains("dark") || gtkTheme.contains("black")) return true
        if (gtkTheme.contains("light")) return false
    }

    val gtkConfig = File(System.getProperty("user.home"), ".config/gtk-3.0/settings.ini")
    if (gtkConfig.exists()) {
        val config = gtkConfig.readText().lowercase()
        if (config.contains("gtk-theme-name=adwaita-dark")) return true
        if (config.contains("gtk-theme-name=adwaita")) return false
    }

    return false
}

fun main() = application {
    val windowState = rememberWindowState(
        width = 420.dp,
        height = 700.dp,
        position = WindowPosition(Alignment.Center)
    )
    val useDarkTheme = remember { mutableStateOf(false) }

    Window(
        onCloseRequest = ::exitApplication,
        title = "VN Music",
        state = windowState,
        // Используем сгенерированный ресурс
        icon = painterResource(Res.drawable.app_icon)
    ) {
        window.minimumSize = Dimension(350, 500)

        LaunchedEffect(Unit) {
            while (true) {
                useDarkTheme.value = withContext(Dispatchers.IO) {
                    isSystemDarkTheme()
                }
                delay(1_000.milliseconds)
            }
        }

        MaterialTheme(
            colorScheme = if (useDarkTheme.value) darkColorScheme()
            else lightColorScheme()
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                App()
            }
        }
    }
}
