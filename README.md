# VN Music

**Crossplatform desktop audio player**  
This is a Kotlin Multiplatform project targeting Desktop (JVM).  
Successfull testing for ubuntu.

### UI screenshots

<details>

<summary>View</summary>

<img alt="screen-light.png" height="500" src=".img/screen-light.png"/>
<img alt="screen-dark.png" height="500" src=".img/screen-dark.png"/>

</details>

**Use drug-and-drop to add new tracks and playlists**

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
    - [commonMain](./composeApp/src/commonMain/kotlin) is for code that's common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
      For example, if you want to use Apple's CoreCrypto for the iOS part of your Kotlin app,
      the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
      Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
      folder is the appropriate location.

### Build and Run Desktop (JVM) Application

To build and run the development version of the desktop app, use the run configuration from the run widget
in your IDE's toolbar or run it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:run
  ```

### Build a cross-platform desktop Uber JAR

The Uber JAR includes Compose Desktop native libraries for Linux (x64/ARM64), Windows (x64/ARM64),
and macOS (x64/ARM64), so it can be copied between these platforms. Build it on macOS/Linux with:

```shell
./gradlew :composeApp:packageUberJarForCurrentOS
```

On Windows, run:

```shell
.\gradlew.bat :composeApp:packageUberJarForCurrentOS
```

The JAR is written to `composeApp/build/compose/jars/`. It still requires a compatible Java runtime
on the target machine; use a native distribution instead if you need to bundle Java or provide an
OS-specific installer.

---

Learn more about [Compose Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-multiplatform.html).
