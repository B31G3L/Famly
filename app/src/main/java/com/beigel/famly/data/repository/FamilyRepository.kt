package com.beigel.famly.data.repository

import com.beigel.famly.data.model.FamilyTree
import com.beigel.famly.data.model.Person
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository-Schicht für den Familienstammbaum.
 *
 * Lesezugriffe sind reaktiv (StateFlow), weil die Daten per Firestore-Snapshot-Listener
 * hereinkommen und sich jederzeit von einem anderen Gerät aus ändern
 * können. Schreibzugriffe sind suspend-Funktionen, da es echte
 * Netzwerk-Roundtrips sind, die auch fehlschlagen können (Result<T>).
 */
interface FamilyRepository {
    /** Aktueller Stammbaum (Name, Mitgliederzahl, alle Personen) der Familie des angemeldeten Nutzers. */
    val familyTree: StateFlow<FamilyTree>

    /** Anzeigename für "Hallo, …"-Begrüßungen im Dashboard. */
    val currentUserName: StateFlow<String>

    /** Einladungscode der aktuellen Familie, leer solange noch keine Familie zugeordnet ist. */
    val inviteCode: StateFlow<String>

    /** true, solange die initialen Firestore-Daten noch nicht geladen sind. */
    val isLoading: StateFlow<Boolean>

    /**
     * Legt bei der ersten Anmeldung automatisch eine neue Familie an
     * (inkl. einer Person "Ich" für den Nutzer selbst), falls der Nutzer
     * noch keiner Familie zugeordnet ist. Mehrfacher Aufruf ist sicher (No-Op).
     */
    suspend fun ensureFamilyForCurrentUser()

    /** Tritt einer bestehenden Familie über deren Einladungscode bei. */
    suspend fun joinFamilyWithCode(code: String): Result<Unit>

    /**
     * Legt eine neue Person an, ausgehend von einer bestehenden Person
     * ([relativeOfId]) und einem [relationType] (Mama/Papa/Tochter/Sohn aus
     * Sicht dieser Person). Baum-Position UND die Bezeichnung relativ zu
     * "Ich" (z. B. "Großmutter", "Onkel") werden daraus automatisch
     * abgeleitet - siehe [FirestoreFamilyRepository].
     */
    suspend fun addPerson(
        name: String,
        birthDate: String,
        birthPlace: String,
        isDeceased: Boolean,
        deathDate: String,
        bio: String,
        relativeOfId: String,
        relationType: com.beigel.famly.data.model.RelationType
    ): Result<Person>

    /** Aktualisiert eine bestehende Person (Position, Beziehung, Verknüpfungen bleiben erhalten). */
    suspend fun updatePerson(
        id: String,
        name: String,
        birthDate: String,
        birthPlace: String,
        isDeceased: Boolean,
        deathDate: String,
        bio: String
    ): Result<Unit>

    suspend fun deletePerson(id: String): Result<Unit>
}
