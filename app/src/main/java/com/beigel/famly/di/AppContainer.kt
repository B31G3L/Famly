package com.beigel.famly.di

import com.beigel.famly.data.repository.FakeFamilyRepository
import com.beigel.famly.data.repository.FamilyRepository

/**
 * Einfacher manueller DI-Container statt Hilt.
 *
 * Hintergrund: Das Hilt-Gradle-Plugin hat aktuell (Stand AGP 9.x) ein noch
 * ungelöstes Kompatibilitätsproblem mit dem neuen Android-Gradle-Plugin-DSL
 * ("Android BaseExtension not found", siehe google/dagger#4944). Um nicht von
 * diesem Upstream-Bug abhängig zu sein, wird die App-weite Abhängigkeit hier
 * manuell bereitgestellt – analog zum FakePersonRepository-Ansatz in Genea.
 */
object AppContainer {
    val familyRepository: FamilyRepository by lazy { FakeFamilyRepository() }
}
