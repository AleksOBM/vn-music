import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.compose.runtime:runtime:1.12.2")
            implementation("org.jetbrains.compose.foundation:foundation:1.12.2")
            implementation("org.jetbrains.compose.material3:material3:1.9.0")
            implementation("org.jetbrains.compose.ui:ui:1.12.2")
            implementation("org.jetbrains.compose.components:components-resources:1.12.2")
            implementation("org.jetbrains.compose.ui:ui-tooling-preview:1.12.2")
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        jvmMain.dependencies {
            implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
            implementation("io.github.kdroidfilter:composemediaplayer-audio:0.11.4")
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)

            val composeVersion = libs.versions.composeMultiplatform.get()
            listOf(
                "linux-x64",
                "linux-arm64",
                "windows-x64",
                "windows-arm64",
                "macos-x64",
                "macos-arm64"
            ).forEach { platform ->
                runtimeOnly("org.jetbrains.compose.desktop:desktop-jvm-$platform:$composeVersion")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "io.github.aleksobm.vnmusic.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "VN Music"
            packageVersion = "1.0.0"
            description = "Desktop audio player for local music and Internet radio"
            vendor = "Aleks O."
            copyright = "Copyright (c) 2026 Aleks O."
            licenseFile.set(project.file("../LICENSE.md"))

            linux {
                packageName = "vnmusic"
                appCategory = "AudioVideo"
                shortcut = true
                iconFile.set(project.file("src/jvmMain/composeResources/drawable/app_icon.png"))
            }

            windows {
                iconFile.set(project.file("src/jvmMain/composeResources/drawable/app_icon.ico"))
                menuGroup = "VN Music"
                menu = true
            }
        }
    }
}
