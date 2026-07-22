package com.beigel.famly.di

import com.beigel.famly.data.auth.AuthRepository
import com.beigel.famly.data.auth.FirebaseAuthRepository
import com.beigel.famly.data.repository.FamilyRepository
import com.beigel.famly.data.repository.FirestoreFamilyRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Einfacher manueller DI-Container statt Hilt.
 *
 * Hintergrund: Das Hilt-Gradle-Plugin hat aktuell (Stand AGP 9.x) ein noch
 * ungelöstes Kompatibilitätsproblem mit dem neuen Android-Gradle-Plugin-DSL
 * ("Android BaseExtension not found", siehe google/dagger#4944). Um nicht von
 * diesem Upstream-Bug abhängig zu sein, wird die App-weite Abhängigkeit hier
 * manuell bereitgestellt – analog zum FakePersonRepository-Ansatz in Genea.
 *
 * Lebt an der Application (siehe FamlyApplication), damit die
 * Firestore-Snapshot-Listener über die gesamte App-Laufzeit aktiv bleiben
 * und nicht bei jeder Activity-Recreation neu aufgebaut werden.
 */
class AppContainer {

    /** Läuft für die gesamte App-Lebensdauer, unabhängig von Activity/Compose-Lifecycle. */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    val authRepository: AuthRepository by lazy {
        FirebaseAuthRepository(auth = firebaseAuth)
    }

    val familyRepository: FamilyRepository by lazy {
        FirestoreFamilyRepository(
            firestore = firestore,
            authRepository = authRepository,
            externalScope = applicationScope
        )
    }
}
