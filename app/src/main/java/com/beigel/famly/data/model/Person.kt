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
    val bio: String = "",
    val connections: List<String> = emptyList(),
    val treePosition: TreePosition? = null
)

/**
 * Relative Position im Stammbaum-Diagramm (generation = Reihe, slot = Spalte).
 * generation 0 = Großeltern-Ebene, aufsteigend nach unten.
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
