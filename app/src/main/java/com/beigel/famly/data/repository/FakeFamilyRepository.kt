package com.beigel.famly.data.repository

import com.beigel.famly.data.model.AvatarAccent
import com.beigel.famly.data.model.FamilyTree
import com.beigel.famly.data.model.Person
import com.beigel.famly.data.model.RelationType
import com.beigel.famly.data.model.TreePosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * In-Memory-Repository, das dasselbe Interface wie [FirestoreFamilyRepository]
 * implementiert. Wird nicht mehr in [com.beigel.famly.di.AppContainer]
 * verwendet, ist aber weiterhin praktisch für Compose-Previews und Tests,
 * die ohne Firebase-Anbindung auskommen sollen.
 */
class FakeFamilyRepository : FamilyRepository {

    private val oma = Person(
        id = "oma", name = "Oma Grete", initial = "O", relation = "Großmutter",
        accent = AvatarAccent.YELLOW, birthDate = "3. Mai 1945", birthPlace = "Stuttgart",
        bio = "Grete führte über 40 Jahre lang die Familiengärtnerei und liebt es, samstags zu backen.",
        treePosition = TreePosition(0, 0)
    )
    private val opa = Person(
        id = "opa", name = "Opa Heinz", initial = "O", relation = "Großvater",
        accent = AvatarAccent.YELLOW, birthDate = "17. Januar 1943", birthPlace = "Ulm",
        bio = "Heinz war Schreiner und baut heute noch kleine Holzspielzeuge für die Enkel.",
        treePosition = TreePosition(0, 1)
    )
    private val mama = Person(
        id = "mama", name = "Mama", initial = "M", relation = "Mutter",
        accent = AvatarAccent.PETROL, birthDate = "22. April 1968", birthPlace = "Stuttgart",
        bio = "Mama arbeitet als Krankenschwester und liebt lange Spaziergänge im Wald.",
        treePosition = TreePosition(1, 1)
    )
    private val ich = Person(
        id = "ich", name = "Ich", initial = "I", relation = "Ich",
        accent = AvatarAccent.PETROL, treePosition = TreePosition(2, 1), uid = "ich"
    )

    private val _familyTree = MutableStateFlow(
        FamilyTree(
            id = "familie_mueller",
            name = "Familie Müller",
            memberCount = 4,
            members = listOf(oma, opa, mama, ich)
        )
    )
    override val familyTree: StateFlow<FamilyTree> = _familyTree.asStateFlow()

    override val currentUserName: StateFlow<String> = MutableStateFlow("Anna").asStateFlow()
    override val inviteCode: StateFlow<String> = MutableStateFlow("OFFSHOOT-7F3K2").asStateFlow()
    override val isLoading: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    override suspend fun ensureFamilyForCurrentUser() = Unit

    override suspend fun joinFamilyWithCode(code: String): Result<Unit> = Result.success(Unit)

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
        val members = _familyTree.value.members
        val relativeOf = members.find { it.id == relativeOfId } ?: error("Ausgangsperson nicht gefunden")
        val targetGeneration = (relativeOf.treePosition?.generation ?: 0) + relationType.generationOffset
        val usedSlots = members.filter { it.treePosition?.generation == targetGeneration }.mapNotNull { it.treePosition?.slot }
        val treePosition = TreePosition(targetGeneration, (usedSlots.maxOrNull() ?: -1) + 1)
        val relation = inferRelationToSelf(relativeOf.relation, relationType)

        val newPersonIsParent = relationType.generationOffset < 0
        val isPartnerRelation = relationType == RelationType.PARTNER
        val partnerOfRelativeOf = relativeOf.partnerId?.let { pid -> members.find { it.id == pid } }

        val newPersonParentIds = when {
            isPartnerRelation || newPersonIsParent -> emptyList()
            else -> listOfNotNull(relativeOf.id, partnerOfRelativeOf?.id).distinct()
        }

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

        val id = UUID.randomUUID().toString()
        val person = Person(
            id = id,
            name = name,
            initial = name.trim().firstOrNull()?.uppercase() ?: "?",
            relation = relation,
            accent = AvatarAccent.entries.random(),
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
        _familyTree.update { tree ->
            val updatedMembers = tree.members.map { existing ->
                when {
                    newPersonIsParent && existing.id == relativeOf.id -> existing.copy(
                        parentIds = existing.parentIds + id,
                        motherId = if (relationType == RelationType.MOTHER) id else existing.motherId,
                        fatherId = if (relationType == RelationType.FATHER) id else existing.fatherId
                    )
                    isPartnerRelation && existing.id == relativeOf.id -> existing.copy(partnerId = id)
                    else -> existing
                }
            }
            tree.copy(members = updatedMembers + person, memberCount = updatedMembers.size + 1)
        }
        person
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
        _familyTree.update { tree ->
            tree.copy(
                members = tree.members.map {
                    if (it.id == id) {
                        it.copy(
                            name = name, birthDate = birthDate, birthPlace = birthPlace,
                            isDeceased = isDeceased, deathDate = deathDate, bio = bio
                        )
                    } else it
                }
            )
        }
    }

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

    override suspend fun deletePerson(id: String): Result<Unit> = runCatching {
        if (id == "ich") {
            error("Du kannst dich selbst nicht aus dem Baum löschen")
        }
        _familyTree.update { tree ->
            val remaining = tree.members.filterNot { it.id == id }
            tree.copy(members = remaining, memberCount = remaining.size)
        }
    }
}