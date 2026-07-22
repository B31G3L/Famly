package com.beigel.famly.data.repository

import com.beigel.famly.data.model.AvatarAccent
import com.beigel.famly.data.model.Person
import com.beigel.famly.data.model.TreePosition
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Manuelles Mapping statt automatischer POJO-Deserialisierung: [Person]
 * enthält mit [AvatarAccent] ein Enum mit einer Compose-Color-Property,
 * die Firestore nicht sinnvoll automatisch abbilden kann. Manuelles
 * Mapping ist außerdem robuster gegen fehlende/alte Felder in bestehenden
 * Dokumenten (z. B. nach künftigen Modelländerungen).
 */
internal fun Person.toFirestoreMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "initial" to initial,
    "relation" to relation,
    "accent" to accent.name,
    "birthDate" to birthDate,
    "birthPlace" to birthPlace,
    "isDeceased" to isDeceased,
    "bio" to bio,
    "connections" to connections,
    "treeGeneration" to treePosition?.generation,
    "treeSlot" to treePosition?.slot
)

internal fun DocumentSnapshot.toPerson(): Person? {
    val name = getString("name") ?: return null
    val generation = getLong("treeGeneration")?.toInt()
    val slot = getLong("treeSlot")?.toInt()
    val accent = runCatching { AvatarAccent.valueOf(getString("accent").orEmpty()) }
        .getOrDefault(AvatarAccent.PETROL)

    return Person(
        id = id,
        name = name,
        initial = getString("initial") ?: name.trim().firstOrNull()?.uppercase().orEmpty(),
        relation = getString("relation").orEmpty(),
        accent = accent,
        birthDate = getString("birthDate").orEmpty(),
        birthPlace = getString("birthPlace").orEmpty(),
        isDeceased = getBoolean("isDeceased") ?: false,
        bio = getString("bio").orEmpty(),
        connections = (get("connections") as? List<*>)?.filterIsInstance<String>().orEmpty(),
        treePosition = if (generation != null && slot != null) TreePosition(generation, slot) else null
    )
}
