import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")           // Compose MPP Plugin
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)

    // JetBrains Compose Multiplatform Material3 (für Desktop & Android gemeinsam)
    implementation("org.jetbrains.compose.material3:material3:1.5.2")  // :contentReference[oaicite:0]{index=0}

    implementation("org.xerial:sqlite-jdbc:3.41.2.2")
    implementation("javazoom:jlayer:1.0.1")
    implementation("org.apache.pdfbox:pdfbox:2.0.30")


    implementation("org.jfree:jfreechart:1.5.4")
    implementation("org.jfree:jcommon:1.0.24")
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "AuftrgsTabelle"
            packageVersion = "1.0.0"
        }
    }
}
