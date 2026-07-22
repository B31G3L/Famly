// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Hinweis: AGP 9.x bringt "built-in Kotlin" mit, das kotlin-android-Plugin
// wird daher bewusst NICHT mehr angewendet (siehe Migrationsleitfaden von Google).
// Auf das Hilt-Gradle-Plugin wird ebenfalls verzichtet, da es aktuell mit dem
// neuen AGP-9-DSL noch nicht kompatibel ist (google/dagger#4944). Stattdessen
// kommt ein einfacher manueller DI-Container (AppContainer) zum Einsatz.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
}
