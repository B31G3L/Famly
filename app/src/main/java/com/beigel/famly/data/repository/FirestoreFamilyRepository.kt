package com.beigel.famly.data.repository

import android.util.Log
import com.beigel.famly.data.auth.AuthRepository
import com.beigel.famly.data.model.AvatarAccent
import com.beigel.famly.data.model.FamilyTree
import com.beigel.famly.data.model.Person
import com.beigel.famly.data.model.RelationType
import com.beigel.famly.data.model.TreePosition
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

private const val COLLECTION_USERS = "users"
private const val COLLECTION_FAMILIES = "families"
private const val COLLECTION_PERSONS = "persons"
private const val SELF_PERSON_ID = "ich"
private const val FIELD_FAMILY_ID = "familyId"
private const val FIELD_INVITE_CODE = "inviteCode"
private const val TAG = "FirestoreFamilyRepo"

/**
 * Firestore-Datenmodell:
 *
 *  users/{uid}                     -> { familyId, displayName }
 *  families/{familyId}             -> { name, ownerId, inviteCode, memberIds: [uid] }
 *  families/{familyId}/persons/{id} -> Person-Felder (siehe [PersonMapper])
 *
 * Jeder Nutzer gehört genau einer Familie an. Die eigene Person im Baum
 * bekommt konventionsgemäß die feste ID "ich", damit z. B. die
 * Baum-Darstellung unverändert funktioniert.
 */
class FirestoreFamilyRepository(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val externalScope: CoroutineScope
) : FamilyRepository {

    private val _familyId = MutableStateFlow<String?>(null)

    private val _familyTree = MutableStateFlow(emptyFamilyTree())
    override val familyTree: StateFlow<FamilyTree> = _familyTree.asStateFlow()

    private val _inviteCode = MutableStateFlow("")
    override val inviteCode: StateFlow<String> = _inviteCode.asStateFlow()

    private val _currentUserName = MutableStateFlow("Familie")
    override val currentUserName: StateFlow<String> = _currentUserName.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var userDocListener: ListenerRegistration? = null
    private var familyDocListener: ListenerRegistration? = null
    private var personsListener: ListenerRegistration? = null

    init {
        authRepository.currentUser
            .onEach { user ->
                if (user == null) {
                    detachFamilyListeners()
                    _familyId.value = null
                    _familyTree.value = emptyFamilyTree()
                    _isLoading.value = false
                    return@onEach
                }
                user.displayName?.let { name ->
                    if (_familyTree.value.members.none { it.id == SELF_PERSON_ID }) {
                        _currentUserName.value = name.substringBefore(" ")
                    }
                }
                observeUserDocument(user.uid)
            }
            .launchIn(externalScope)
    }

    private fun observeUserDocument(uid: String) {
        userDocListener?.remove()
        userDocListener = firestore.collection(COLLECTION_USERS).document(uid)
            .addSnapshotListener { snapshot, _ ->
                val familyId = snapshot?.getString(FIELD_FAMILY_ID)
                if (familyId != _familyId.value) {
                    _familyId.value = familyId
                    if (familyId != null) {
                        observeFamily(familyId)
                    } else {
                        detachFamilyListeners()
                        _familyTree.value = emptyFamilyTree()
                        _isLoading.value = false
                    }
                }
            }
    }

    private fun observeFamily(familyId: String) {
        familyDocListener?.remove()
        personsListener?.remove()

        familyDocListener = firestore.collection(COLLECTION_FAMILIES).document(familyId)
            .addSnapshotListener { snapshot, _ ->
                _inviteCode.value = snapshot?.getString(FIELD_INVITE_CODE).orEmpty()
                val name = snapshot?.getString("name") ?: _familyTree.value.name
                _familyTree.value = _familyTree.value.copy(id = familyId, name = name)
            }

        personsListener = firestore.collection(COLLECTION_FAMILIES).document(familyId)
            .collection(COLLECTION_PERSONS)
            .addSnapshotListener { snapshot, _ ->
                val members = snapshot?.documents?.mapNotNull { it.toPerson() }.orEmpty()
                _familyTree.value = _familyTree.value.copy(memberCount = members.size, members = members)
                members.find { it.id == SELF_PERSON_ID }?.let {
                    _currentUserName.value = it.name.substringBefore(" ")
                }
                _isLoading.value = false
                // Selbstheilung: "Ich" darf zwar nicht mehr gelöscht werden,
                // falls das Dokument aber trotzdem einmal fehlt (z. B. aus der
                // Zeit vor diesem Schutz), wird es hier automatisch neu angelegt.
                if (snapshot != null && members.none { it.id == SELF_PERSON_ID }) {
                    recreateSelfPersonIfMissing(familyId)
                }
                // Selbstheilung: ältere Personen-Einträge (vor Einführung des
                // parentIds-Felds) kennen nur das richtungslose connections-
                // Feld. Hier wird daraus einmalig die echte Eltern-Kind-
                // Beziehung abgeleitet (per Generationsvergleich: die niedrigere
                // Generation ist der Elternteil) und nachgetragen, damit die
                // klassische Stammbaum-Darstellung greift.
                if (snapshot != null) {
                    backfillParentIdsFromConnections(familyId, members)
                }
            }
    }

    private fun recreateSelfPersonIfMissing(familyId: String) {
        val user = authRepository.currentUser.value ?: return
        externalScope.launch {
            val personsRef = firestore.collection(COLLECTION_FAMILIES).document(familyId)
                .collection(COLLECTION_PERSONS)
            // Doppelt prüfen (nicht nur auf den zuletzt empfangenen Snapshot
            // verlassen), um ein Wettrennen mit einem parallel laufenden
            // zweiten Gerät/Listener zu vermeiden.
            val existing = runCatching { personsRef.document(SELF_PERSON_ID).get().await() }.getOrNull()
            if (existing?.exists() == true) return@launch

            val displayName = user.displayName?.takeIf { it.isNotBlank() }
                ?: _currentUserName.value.takeIf { it.isNotBlank() }
                ?: "Ich"
            val selfPerson = Person(
                id = SELF_PERSON_ID,
                name = displayName,
                initial = displayName.trim().firstOrNull()?.uppercase() ?: "?",
                relation = "Ich",
                accent = AvatarAccent.PETROL,
                treePosition = TreePosition(generation = 2, slot = 0),
                uid = user.uid
            )
            runCatching {
                personsRef.document(SELF_PERSON_ID).set(selfPerson.toFirestoreMap()).await()
            }
        }
    }

    private fun detachFamilyListeners() {
        familyDocListener?.remove()
        personsListener?.remove()
        familyDocListener = null
        personsListener = null
    }

    override suspend fun ensureFamilyForCurrentUser() {
        val user = authRepository.currentUser.value
            ?: authRepository.signInAnonymouslyIfNeeded().getOrThrow()

        val userDocRef = firestore.collection(COLLECTION_USERS).document(user.uid)
        val userSnapshot = userDocRef.get().await()
        if (userSnapshot.getString(FIELD_FAMILY_ID) != null) return

        val familyRef = firestore.collection(COLLECTION_FAMILIES).document()
        val displayName = user.displayName?.takeIf { it.isNotBlank() } ?: "Ich"
        val inviteCode = generateInviteCode()

        // Schritt 1: Familie + memberIds anlegen und den Nutzer darauf verweisen.
        // WICHTIG: bewusst NICHT im selben Batch wie die Person weiter unten,
        // weil die Firestore-Regel für "persons" per get() prüft, ob der
        // Nutzer schon in family.memberIds steht - und innerhalb eines
        // einzelnen atomaren Batches sieht diese Prüfung die anderen,
        // noch nicht committeten Schreibvorgänge desselben Batches nicht
        // (führt sonst zu PERMISSION_DENIED beim Anlegen der ersten Person).
        firestore.runBatch { batch ->
            batch.set(
                familyRef,
                mapOf(
                    "name" to "Familie ${displayName.substringBefore(" ")}",
                    "ownerId" to user.uid,
                    FIELD_INVITE_CODE to inviteCode,
                    "memberIds" to listOf(user.uid)
                )
            )
            batch.set(
                userDocRef,
                mapOf(FIELD_FAMILY_ID to familyRef.id, "displayName" to displayName),
                SetOptions.merge()
            )
        }.await()

        // Schritt 2: jetzt ist die Familie committed, isFamilyMember(familyId)
        // in den Rules kann den Nutzer in memberIds finden.
        val selfPerson = Person(
            id = SELF_PERSON_ID,
            name = displayName,
            initial = displayName.trim().firstOrNull()?.uppercase() ?: "?",
            relation = "Ich",
            accent = AvatarAccent.PETROL,
            treePosition = TreePosition(generation = 2, slot = 1),
            uid = user.uid
        )
        familyRef.collection(COLLECTION_PERSONS).document(SELF_PERSON_ID)
            .set(selfPerson.toFirestoreMap())
            .await()
    }

    override suspend fun joinFamilyWithCode(code: String): Result<Unit> = runCatching {
        val user = authRepository.currentUser.value ?: error("Nicht angemeldet")
        val normalizedCode = code.trim().uppercase()

        val query = firestore.collection(COLLECTION_FAMILIES)
            .whereEqualTo(FIELD_INVITE_CODE, normalizedCode)
            .limit(1)
            .get()
            .await()

        val familyDoc = query.documents.firstOrNull()
            ?: error("Kein Familie mit dem Code \"$code\" gefunden")

        firestore.collection(COLLECTION_FAMILIES).document(familyDoc.id)
            .update("memberIds", FieldValue.arrayUnion(user.uid))
            .await()

        firestore.collection(COLLECTION_USERS).document(user.uid)
            .set(mapOf(FIELD_FAMILY_ID to familyDoc.id), SetOptions.merge())
            .await()

        // Schritt 3: eigenen Personen-Eintrag im Baum der Familie anlegen,
        // damit der Beitritt auch als "echtes" Mitglied (mit uid) sichtbar
        // wird - u. a. in der Mitglieder-Liste von "Liste teilen". Nur
        // schreiben, falls noch keine Person mit dieser uid existiert (z. B.
        // erneutes Beitreten nach Reinstall auf demselben Account).
        val personsRef = firestore.collection(COLLECTION_FAMILIES).document(familyDoc.id)
            .collection(COLLECTION_PERSONS)
        val existingPersons = personsRef.get().await()
        val alreadyLinked = existingPersons.documents.any { it.getString("uid") == user.uid }
        if (!alreadyLinked) {
            val displayName = user.displayName?.takeIf { it.isNotBlank() } ?: "Neues Mitglied"
            val members = existingPersons.documents.mapNotNull { it.toPerson() }
            val id = UUID.randomUUID().toString()
            val newMember = Person(
                id = id,
                name = displayName,
                initial = displayName.trim().firstOrNull()?.uppercase() ?: "?",
                relation = "Mitglied",
                accent = nextAccent(members),
                treePosition = nextJoinerPosition(members),
                uid = user.uid
            )
            personsRef.document(id).set(newMember.toFirestoreMap()).await()
        }
    }

    override suspend fun addPerson(
        name: String,
        birthDate: String,
        birthPlace: String,
        isDeceased: Boolean,
        deathDate: String,
        bio: String,
        relativeOfId: String,
        relationType: RelationType
    ): Result<Person> = runCatching {
        val familyId = _familyId.value ?: error("Keine Familie zugeordnet")
        val members = _familyTree.value.members
        val id = UUID.randomUUID().toString()

        val relativeOf = members.find { it.id == relativeOfId } ?: error("Ausgangsperson nicht gefunden")
        val treePosition = positionRelativeTo(members, relativeOf, relationType)
        val relation = inferRelationToSelf(relativeOf.relation, relationType)

        // Mama/Papa: die neue Person IST ein Elternteil von relativeOf ->
        // die Rück-Referenz kommt auf relativeOf (parentIds += id, plus
        // motherId/fatherId für die direkte Anzeige im Detail-Screen).
        // Tochter/Sohn: die neue Person HAT relativeOf (und ggf. dessen
        // Partner:in) als Elternteil -> die Referenz kommt direkt auf die
        // neue Person, inkl. motherId/fatherId falls das Geschlecht von
        // relativeOf/dessen Partner:in bekannt ist.
        // Partner:in: gleiche Generation, keine Eltern-Kind-Beziehung ->
        // partnerId wird auf beiden Seiten gesetzt.
        val newPersonIsParent = relationType.generationOffset < 0
        val isPartnerRelation = relationType == RelationType.PARTNER

        val partnerOfRelativeOf = relativeOf.partnerId?.let { pid -> members.find { it.id == pid } }

        val newPersonParentIds = when {
            isPartnerRelation -> emptyList()
            newPersonIsParent -> emptyList()
            else -> listOfNotNull(relativeOf.id, partnerOfRelativeOf?.id).distinct()
        }

        // motherId/fatherId der NEUEN Person (nur im Kind-Fall relevant):
        // aus relativeOf und dessen Partner:in ableiten, sofern deren
        // Geschlecht bekannt ist.
        var newPersonMotherId: String? = null
        var newPersonFatherId: String? = null
        if (!newPersonIsParent && !isPartnerRelation) {
            listOfNotNull(relativeOf, partnerOfRelativeOf).forEach { parent ->
                when (parent.isFemale) {
                    true -> newPersonMotherId = newPersonMotherId ?: parent.id
                    false -> newPersonFatherId = newPersonFatherId ?: parent.id
                    null -> Unit
                }
            }
        }

        val person = Person(
            id = id,
            name = name,
            initial = name.trim().firstOrNull()?.uppercase() ?: "?",
            relation = relation,
            accent = nextAccent(members),
            birthDate = birthDate,
            birthPlace = birthPlace,
            isDeceased = isDeceased,
            deathDate = deathDate,
            bio = bio,
            connections = listOf(relativeOf.name),
            parentIds = newPersonParentIds,
            motherId = newPersonMotherId,
            fatherId = newPersonFatherId,
            partnerId = if (isPartnerRelation) relativeOf.id else null,
            isFemale = relationType.isFemale,
            treePosition = treePosition
        )

        val familyPersonsRef = firestore.collection(COLLECTION_FAMILIES).document(familyId)
            .collection(COLLECTION_PERSONS)

        Log.d(TAG, "addPerson: starte Batch-Commit für neue Person $id (relationType=$relationType, relativeOf=${relativeOf.id})")
        firestore.runBatch { batch ->
            batch.set(familyPersonsRef.document(id), person.toFirestoreMap())
            when {
                newPersonIsParent -> {
                    val relativeOfRef = familyPersonsRef.document(relativeOf.id)
                    batch.update(relativeOfRef, "parentIds", FieldValue.arrayUnion(id))
                    when (relationType) {
                        RelationType.MOTHER -> batch.update(relativeOfRef, "motherId", id)
                        RelationType.FATHER -> batch.update(relativeOfRef, "fatherId", id)
                        else -> Unit
                    }
                }
                isPartnerRelation -> {
                    batch.update(familyPersonsRef.document(relativeOf.id), "partnerId", id)
                }
            }
        }.await()
        Log.d(TAG, "addPerson: Batch-Commit für $id erfolgreich abgeschlossen")

        person
    }.also { result ->
        result.onFailure { error -> Log.e(TAG, "addPerson fehlgeschlagen", error) }
    }

    override suspend fun updatePerson(
        id: String,
        name: String,
        birthDate: String,
        birthPlace: String,
        isDeceased: Boolean,
        deathDate: String,
        bio: String
    ): Result<Unit> = runCatching {
        val familyId = _familyId.value ?: error("Keine Familie zugeordnet")
        val existing = _familyTree.value.members.find { it.id == id } ?: error("Person nicht gefunden")

        val updated = existing.copy(
            name = name,
            initial = name.trim().firstOrNull()?.uppercase() ?: existing.initial,
            birthDate = birthDate,
            birthPlace = birthPlace,
            isDeceased = isDeceased,
            deathDate = deathDate,
            bio = bio
        )

        firestore.collection(COLLECTION_FAMILIES).document(familyId)
            .collection(COLLECTION_PERSONS).document(id)
            .set(updated.toFirestoreMap())
            .await()
    }

    override suspend fun deletePerson(id: String): Result<Unit> = runCatching {
        if (id == SELF_PERSON_ID) {
            error("Du kannst dich selbst nicht aus dem Baum löschen")
        }
        val familyId = _familyId.value ?: error("Keine Familie zugeordnet")
        firestore.collection(COLLECTION_FAMILIES).document(familyId)
            .collection(COLLECTION_PERSONS).document(id)
            .delete()
            .await()
    }

    /**
     * Leitet für Personen ohne gespeicherte [Person.parentIds] aus dem alten,
     * richtungslosen [Person.connections]-Feld die echte Eltern-Kind-Beziehung
     * ab: von zwei verbundenen Personen gilt die mit der niedrigeren
     * [TreePosition.generation] als Elternteil. Gleiche Generation (z. B.
     * Partner-artige Verbindungen) wird ignoriert, da hier keine Kind-Richtung
     * bestimmbar ist. Schreibt nur Personen, für die sich tatsächlich neue
     * parentIds ergeben (No-Op, falls schon alles migriert ist).
     */
    private fun backfillParentIdsFromConnections(familyId: String, members: List<Person>) {
        val byName = members.groupBy { it.name }
        val updates = mutableMapOf<String, List<String>>()

        members.forEach { person ->
            if (person.parentIds.isNotEmpty() || person.connections.isEmpty()) return@forEach
            val personGeneration = person.treePosition?.generation ?: return@forEach

            val inferredParentIds = person.connections
                .flatMap { name -> byName[name].orEmpty() }
                .filter { it.id != person.id }
                .filter { candidate ->
                    val candidateGeneration = candidate.treePosition?.generation
                    candidateGeneration != null && candidateGeneration < personGeneration
                }
                .map { it.id }
                .distinct()

            if (inferredParentIds.isNotEmpty()) {
                updates[person.id] = inferredParentIds
            }
        }

        if (updates.isEmpty()) return

        externalScope.launch {
            runCatching {
                val personsRef = firestore.collection(COLLECTION_FAMILIES).document(familyId)
                    .collection(COLLECTION_PERSONS)
                firestore.runBatch { batch ->
                    updates.forEach { (personId, parentIds) ->
                        batch.update(personsRef.document(personId), "parentIds", parentIds)
                    }
                }.await()
            }
        }
    }

    private fun nextAccent(members: List<Person>): AvatarAccent {
        val rotation = AvatarAccent.entries
        return rotation[members.size % rotation.size]
    }

    /**
     * Platziert neu beigetretene Mitglieder (ohne konkrete Verwandtschafts-
     * beziehung, im Gegensatz zu [positionRelativeTo]) generisch in derselben
     * Generation wie die Person "Ich" der Familie, im nächsten freien Slot.
     */
    private fun nextJoinerPosition(members: List<Person>): TreePosition {
        val selfGeneration = members.find { it.id == SELF_PERSON_ID }?.treePosition?.generation ?: 2
        val usedSlots = members
            .filter { it.treePosition?.generation == selfGeneration }
            .mapNotNull { it.treePosition?.slot }
        val nextSlot = (usedSlots.maxOrNull() ?: -1) + 1
        return TreePosition(selfGeneration, nextSlot)
    }

    /**
     * Positioniert eine neue Person direkt relativ zu [relativeOf] anhand
     * des gewählten [RelationType] (Flow: Person anklicken -> "Verwandte
     * hinzufügen"). Elternteil = eine Generation höher, Kind = eine
     * Generation tiefer, Partner/Geschwister = gleiche Generation.
     */
    private fun positionRelativeTo(members: List<Person>, relativeOf: Person, relationType: RelationType): TreePosition {
        val baseGeneration = relativeOf.treePosition?.generation ?: 0
        val targetGeneration = baseGeneration + relationType.generationOffset
        val usedSlots = members
            .filter { it.treePosition?.generation == targetGeneration }
            .mapNotNull { it.treePosition?.slot }
        val nextSlot = (usedSlots.maxOrNull() ?: -1) + 1
        return TreePosition(targetGeneration, nextSlot)
    }

    /**
     * Leitet die Bezeichnung der neuen Person relativ zu "Ich" automatisch her,
     * aus der Beziehung der Ausgangsperson zu "Ich" ([baseRelation]) plus dem
     * gewählten [RelationType] (Mama/Papa/Tochter/Sohn aus Sicht der
     * Ausgangsperson). Deckt die gängigen Verwandtschaftsgrade ab; für
     * exotischere Fälle (z. B. "Tochter" von "Tochter" in väterlicher statt
     * mütterlicher Linie) fällt es auf eine generische Bezeichnung zurück.
     */
    private fun inferRelationToSelf(baseRelation: String, type: RelationType): String {
        if (type == RelationType.PARTNER) {
            return if (baseRelation == "Ich") "Partner:in" else "Partner:in von $baseRelation"
        }
        val addsParent = type.generationOffset < 0
        fun pick(female: String, male: String) = if (type.isFemale == true) female else male

        return when (baseRelation) {
            "Ich" -> if (addsParent) pick("Mutter", "Vater") else pick("Tochter", "Sohn")
            "Mutter", "Vater" -> if (addsParent) pick("Großmutter", "Großvater") else pick("Schwester", "Bruder")
            "Großmutter", "Großvater" -> if (addsParent) pick("Urgroßmutter", "Urgroßvater") else pick("Tante", "Onkel")
            "Schwester", "Bruder" -> if (!addsParent) pick("Nichte", "Neffe") else baseRelation
            "Tante", "Onkel" -> if (!addsParent) pick("Cousine", "Cousin") else baseRelation
            "Tochter", "Sohn" -> if (!addsParent) pick("Enkelin", "Enkel") else "Partner:in"
            else -> type.label
        }
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val raw = (1..8).map { chars.random() }.joinToString("")
        return "${raw.take(4)}-${raw.takeLast(4)}"
    }

    private fun emptyFamilyTree() = FamilyTree(id = "", name = "Familie", memberCount = 0, members = emptyList())
}