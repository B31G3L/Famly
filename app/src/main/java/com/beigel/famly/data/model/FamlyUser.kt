package com.beigel.famly.data.model

/**
 * Repräsentiert den aktuell bei Firebase Auth angemeldeten Nutzer.
 * Entkoppelt die App-Schicht bewusst vom konkreten FirebaseUser-Typ.
 */
data class FamlyUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
    val isAnonymous: Boolean
)
