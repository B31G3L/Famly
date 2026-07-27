package com.beigel.famly.data.model

import androidx.compose.ui.graphics.Color
import com.beigel.famly.ui.theme.FamlyAvatarGreen
import com.beigel.famly.ui.theme.FamlyAvatarOrange
import com.beigel.famly.ui.theme.FamlyAvatarPetrol
import com.beigel.famly.ui.theme.FamlyAvatarYellow

enum class AvatarAccent(val color: Color) {
    YELLOW(FamlyAvatarYellow),
    ORANGE(FamlyAvatarOrange),
    GREEN(FamlyAvatarGreen),
    PETROL(FamlyAvatarPetrol)
}

data class Person(
    val id: String,
    val name: String,
    val initial: String,
    val relation: String,
    val accent: AvatarAccent,
    val birthDate: String = "",
    val birthPlace: String = "",
    val isDeceased: Boolean = false,
    val deathDate: String = "",
    val bio: String = "",
    val connections: List<String> = emptyList(),
    val parentIds: List<String> = emptyList(),
    val treePosition: TreePosition? = null
)

/**
 * Relative Position im Stammbaum-Diagramm (generation = Reihe, slot = Spalte).
 * generation 0 = älteste bekannte Generation, aufsteigend nach unten.
 */
data class TreePosition(
    val generation: Int,
    val slot: Int
)

data class FamilyTree(
    val id: String,
    val name: String,
    val memberCount: Int,
    val members: List<Person>
)

data class FamilyMember(
    val person: Person,
    val role: String,
    val status: MemberStatus
)

enum class MemberStatus {
    OWNER, MEMBER, PENDING
}

/**
 * Beziehungstyp beim Anlegen einer Person direkt ausgehend von einer
 * bestehenden Person (PersonDetailScreen -> "Verwandte hinzufügen").
 * Bestimmt sowohl die Generation der neuen Person relativ zur Ausgangsperson
 * (generationOffset) als auch - zusammen mit der Beziehung der Ausgangsperson
 * zu "Ich" - die automatisch ermittelte Bezeichnung (siehe
 * [FamilyRepository]/[FirestoreFamilyRepository]).
 */
enum class RelationType(val label: String, val generationOffset: Int, val isFemale: Boolean) {
    MOTHER("Mama", -1, true),
    FATHER("Papa", -1, false),
    DAUGHTER("Tochter", +1, true),
    SON("Sohn", +1, false)
}