package com.beigel.famly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.beigel.famly.ui.navigation.FamlyNavHost
import com.beigel.famly.ui.theme.FamlyTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val appContainer by lazy { (application as FamlyApplication).appContainer }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Anonyme Anmeldung + Familie sicherstellen, noch bevor die erste
        // Compose-Frame Firestore-Daten braucht. Läuft im Hintergrund weiter,
        // die UI zeigt bis dahin einfach einen leeren/kurz nachladenden Zustand.
        lifecycleScope.launch {
            appContainer.authRepository.signInAnonymouslyIfNeeded()
            appContainer.familyRepository.ensureFamilyForCurrentUser()
        }

        setContent {
            FamlyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FamlyNavHost(
                        familyRepository = appContainer.familyRepository,
                        authRepository = appContainer.authRepository,
                        onSignInWithGoogle = { triggerGoogleSignIn() }
                    )
                }
            }
        }
    }

    private fun triggerGoogleSignIn() {
        lifecycleScope.launch {
            val idToken = requestGoogleIdToken() ?: return@launch
            appContainer.authRepository.signInWithGoogleIdToken(idToken)
            // Bereits vorhandene (ggf. anonym angelegte) Familie bleibt durch
            // das Verknüpfen des Kontos erhalten – kein neues ensureFamily nötig,
            // aber schadet nicht, falls doch noch keine Familie existiert.
            appContainer.familyRepository.ensureFamilyForCurrentUser()
        }
    }

    /**
     * Fragt über den Credential Manager (aktueller Standard-Weg, ersetzt das
     * deprecated GoogleSignInClient/GoogleSignInApi) ein Google-ID-Token ab.
     *
     * Voraussetzung: In der Firebase Console muss unter Authentication der
     * Google-Provider aktiviert sein. Erst dadurch generiert das
     * google-services-Plugin die Ressource R.string.default_web_client_id,
     * die hier verwendet wird.
     */
    private suspend fun requestGoogleIdToken(): String? {
        val credentialManager = CredentialManager.create(this)
        val webClientId = getString(R.string.default_web_client_id)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val response = credentialManager.getCredential(this, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            } else {
                null
            }
        } catch (e: GetCredentialException) {
            // Nutzer hat abgebrochen oder es ist kein Google-Konto verfügbar.
            null
        }
    }
}
