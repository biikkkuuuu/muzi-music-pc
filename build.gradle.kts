plugins {
    kotlin("jvm") version "2.1.10"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
}

group = "com.biikkkuuuu.muzi"
version = "1.0.0"

repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.1")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    
    // HTTP & Networking (Ktor)
    implementation("io.ktor:ktor-client-core:3.0.3")
    implementation("io.ktor:ktor-client-okhttp:3.0.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.0.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.3")
    implementation("io.ktor:ktor-client-encoding:3.0.3")
    
    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // JavaFX Media for native m4a, aac, and mp3 playback on Windows
    implementation("org.openjfx:javafx-media:21.0.2:win")
    implementation("org.openjfx:javafx-base:21.0.2:win")
    implementation("org.openjfx:javafx-graphics:21.0.2:win")
    
    // Image Loading in Compose Desktop (Coil 3 Multiplatform)
    implementation("io.coil-kt.coil3:coil-compose:3.1.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.1.0")
}

compose.desktop {
    application {
        mainClass = "com.muzi.desktop.MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe)
            packageName = "Muzi"
            packageVersion = "1.0.0"
            description = "Muzi Music - Beautiful Windows Desktop Music Player"
            copyright = "© 2026 biikkkuuuu"
            windows {
                menuGroup = "Muzi"
                upgradeUuid = "4d6d601b-9f93-4a6c-a45e-998811223344"
            }
        }
    }
}
