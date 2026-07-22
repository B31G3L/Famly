package com.beigel.famly.data.repository

import com.beigel.famly.data.auth.AuthRepository
import com.beigel.famly.data.model.AvatarAccent
import com.beigel.famly.data.model.FamilyTree
import com.beigel.famly.data.model.Person
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
import kotlinx.coroutines.tasks.await
import java.util.UUID

private const val COLLECTION_USERS = "users"
private const val COLLECTION_FAMILIES = "families"
private const val COLLECTION_PERSONS = "persons"
private const val SELF_PERSON_ID = "ich"
private const val FIELD_FAMILY_ID = "familyId"
private const val FIELD_INVITE_CODE = "inviteCode"

/**
 * Firestore-Datenmodell:
 *
 *  users/{uid}                     -> { familyId, displayName }
 *  families/{familyId}             -> { name, ownerId, inviteCode, memberIds: [uid] }
 *  families/{familyId}/persons/{id} -> Person-Felder (siehe [PersonMapper])
 *
 * Jeder Nutzer gehört genau einer Familie an. Die eigene Person im Baum
 * bekommt konventionsgemäß die feste ID "ich" (wie schon im
 * FakeFamilyRepository), damit z. B. die Baum-Darstellung unverändert
 * funktioniert.
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
            treePosition = TreePosition(generation = 2, slot = 1)
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
    }

    override suspend fun addPerson(
        name: String,
        relation: String,
        birthDate: String,
        birthPlace: String,
        isDeceased: Boolean,
        bio: String,
        connections: List<String>
    ): Result<Person> = runCatching {
        val familyId = _familyId.value ?: error("Keine Familie zugeordnet")
        val members = _familyTree.value.members
        val id = UUID.randomUUID().toString()

        val person = Person(
            id = id,
            name = name,
            initial = name.trim().firstOrNull()?.uppercase() ?: "?",
            relation = relation,
            accent = nextAccent(members),
            birthDate = birthDate,
            birthPlace = birthPlace,
            isDeceased = isDeceased,
            bio = bio,
            connections = connections,
            treePosition = nextTreePosition(members, connections)
        )

        firestore.collection(COLLECTION_FAMILIES).document(familyId)
            .collection(COLLECTION_PERSONS).document(id)
            .set(person.toFirestoreMap())
            .await()

        person
    }

    override suspend fun updatePerson(
        id: String,
        name: String,
        relation: String,
        birthDate: String,
        birthPlace: String,
        isDeceased: Boolean,
        bio: String,
        connections: List<String>
    ): Result<Unit> = runCatching {
        val familyId = _familyId.value ?: error("Keine Familie zugeordnet")
        val existing = _familyTree.value.members.find { it.id == id } ?: error("Person nicht gefunden")

        val updated = existing.copy(
            name = name,
            initial = name.trim().firstOrNull()?.uppercase() ?: existing.initial,
            relation = relation,
            birthDate = birthDate,
            birthPlace = birthPlace,
            isDeceased = isDeceased,
            bio = bio,
            connections = connections
        )

        firestore.collection(COLLECTION_FAMILIES).document(familyId)
            .collection(COLLECTION_PERSONS).document(id)
            .set(updated.toFirestoreMap())
            .await()
    }

    override suspend fun deletePerson(id: String): Result<Unit> = runCatching {
        val familyId = _familyId.value ?: error("Keine Familie zugeordnet")
        firestore.collection(COLLECTION_FAMILIES).document(familyId)
            .collection(COLLECTION_PERSONS).document(id)
            .delete()
            .await()
    }

    private fun nextAccent(members: List<Person>): AvatarAccent {
        val rotation = AvatarAccent.entries
        return rotation[members.size % rotation.size]
    }

    /**
     * Ordnet eine neue Person heuristisch im Baum ein: eine Generation unter
     * der am höchsten stehenden gewählten Verbindung, im nächsten freien
     * Slot dieser Generation. Ohne Verbindung landet sie auf Höhe von "Ich".
     * (Gleiche Logik wie zuvor im FakeFamilyRepository.)
     */
    private fun nextTreePosition(members: List<Person>, connectionNames: List<String>): TreePosition {
        val connectedGenerations = connectionNames.mapNotNull { name ->
            members.find { it.name.equals(name, ignoreCase = true) }?.treePosition?.generation
        }
        val targetGeneration = if (connectedGenerations.isNotEmpty()) {
            connectedGenerations.max() + 1
        } else {
            members.find { it.id == SELF_PERSON_ID }?.treePosition?.generation ?: 0
        }
        val usedSlots = members
            .filter { it.treePosition?.generation == targetGeneration }
            .mapNotNull { it.treePosition?.slot }
        val nextSlot = (usedSlots.maxOrNull() ?: -1) + 1
        return TreePosition(targetGeneration, nextSlot)
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val raw = (1..8).map { chars.random() }.joinToString("")
        return "${raw.take(4)}-${raw.takeLast(4)}"
    }

    private fun emptyFamilyTree() = FamilyTree(id = "", name = "Familie", memberCount = 0, members = emptyList())
}