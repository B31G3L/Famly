package com.beigel.famly.data.auth

import com.beigel.famly.data.model.FamlyUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Kapselt die Anmeldung. Zwei Modi werden unterstützt:
 *  - Anonym: wird beim App-Start automatisch ausgeführt, damit sofort ein
 *    stabiler Nutzer (und damit eine Firestore-UID) existiert.
 *  - Google (über Credential Manager): "hebt" das anonyme Konto an ein
 *    echtes Google-Konto, sodass die Daten geräteübergreifend erhalten
 *    bleiben (analog zum Vorgehen bei Dotlist).
 */
interface AuthRepository {
    val currentUser: StateFlow<FamlyUser?>
    val isSignedIn: Boolean

    suspend fun signInAnonymouslyIfNeeded(): Result<FamlyUser>
    suspend fun signInWithGoogleIdToken(idToken: String): Result<FamlyUser>
    fun signOut()
}

class FirebaseAuthRepository(
    private val auth: FirebaseAuth
) : AuthRepository {

    private val _currentUser = MutableStateFlow(auth.currentUser?.toFamlyUser())
    override val currentUser: StateFlow<FamlyUser?> = _currentUser.asStateFlow()

    override val isSignedIn: Boolean
        get() = auth.currentUser != null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser?.toFamlyUser()
        }
    }

    override suspend fun signInAnonymouslyIfNeeded(): Result<FamlyUser> = runCatching {
        auth.currentUser?.toFamlyUser()?.let { return@runCatching it }
        val result = auth.signInAnonymously().await()
        result.user?.toFamlyUser() ?: error("Anonyme Anmeldung fehlgeschlagen")
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<FamlyUser> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val current = auth.currentUser
        val authResult = if (current != null && current.isAnonymous) {
            // Anonymes Konto wird mit dem Google-Konto verknüpft, damit
            // die bisher angelegte Familie/die Personen erhalten bleiben.
            current.linkWithCredential(credential).await()
        } else {
            auth.signInWithCredential(credential).await()
        }
        authResult.user?.toFamlyUser() ?: error("Google-Anmeldung fehlgeschlagen")
    }

    override fun signOut() {
        auth.signOut()
    }
}

private fun FirebaseUser.toFamlyUser() = FamlyUser(
    uid = uid,
    displayName = displayName,
    email = email,
    photoUrl = photoUrl?.toString(),
    isAnonymous = isAnonymous
)
