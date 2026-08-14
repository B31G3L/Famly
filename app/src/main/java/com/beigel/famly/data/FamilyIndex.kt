package com.beigel.famly.data

import kotlin.math.abs

/**
 * Vorberechneter Index über den Personenbestand: Nachschlagen nach Id, Kinder,
 * Geschwister, Vorfahren-Tiefen und die Verwandtschaftsbezeichnung relativ zu
 * einer Ausgangsperson.
 *
 * Wird einmal pro Datenstand gebaut (in der UI über `remember`), damit die
 * Baum-Berechnung bei jedem Auf-/Zuklappen nur noch Listen liest.
 */
class FamilyIndex(source: List<DemoPerson>) {

    val people: List<DemoPerson>

    private val byIdMap: Map<String, DemoPerson>
    private val childrenMap: Map<String, List<String>>

    init {
        // Partnerschaften beidseitig ergänzen: es reicht, sie an einer Person zu notieren.
        val partners = mutableMapOf<String, MutableSet<String>>()
        source.forEach { p ->
            p.partnerIds.forEach { other ->
                partners.getOrPut(p.id) { mutableSetOf() }.add(other)
                partners.getOrPut(other) { mutableSetOf() }.add(p.id)
            }
        }
        people = source.map { p ->
            val all = partners[p.id].orEmpty().filter { it != p.id }
            if (all.size == p.partnerIds.size && all.toSet() == p.partnerIds.toSet()) p
            else p.copy(partnerIds = all.toList())
        }
        byIdMap = people.associateBy { it.id }

        val kids = mutableMapOf<String, MutableList<String>>()
        people.forEach { p ->
            p.parentIds.forEach { parent -> kids.getOrPut(parent) { mutableListOf() }.add(p.id) }
        }
        childrenMap = kids.mapValues { (_, list) -> list.sortedBy { byIdMap[it]?.birth ?: "" } }
    }

    operator fun get(id: String): DemoPerson? = byIdMap[id]

    fun require(id: String): DemoPerson =
        byIdMap[id] ?: error("Unbekannte Person: $id")

    fun childrenOf(id: String): List<String> = childrenMap[id].orEmpty()

    /** Kinder eines Paares: beide Seiten zusammengeführt, nach Geburtsdatum sortiert. */
    fun childrenOfUnit(id: String, partnerId: String?): List<String> {
        if (partnerId == null) return childrenOf(id)
        val combined = LinkedHashSet(childrenOf(id))
        combined.addAll(childrenOf(partnerId))
        return combined.sortedBy { byIdMap[it]?.birth ?: "" }
    }

    /** Geschwister: mindestens ein gemeinsamer Elternteil. */
    fun siblingsOf(id: String): List<String> {
        val person = byIdMap[id] ?: return emptyList()
        if (person.parentIds.isEmpty()) return emptyList()
        val result = LinkedHashSet<String>()
        person.parentIds.forEach { parent ->
            childrenOf(parent).forEach { child -> if (child != id) result.add(child) }
        }
        return result.sortedBy { byIdMap[it]?.birth ?: "" }
    }

    /** Eltern, Vater zuerst - damit die Reihenfolge im Baum stabil bleibt. */
    fun parentsOf(id: String): List<String> =
        byIdMap[id]?.parentIds.orEmpty().sortedBy { if (byIdMap[it]?.isFemale == false) 0 else 1 }

    /** Alle Vorfahren inklusive der Person selbst, mit ihrer Generationstiefe. */
    fun ancestorDepths(id: String): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        fun walk(current: String, depth: Int) {
            val known = result[current]
            if (known != null && known <= depth) return
            result[current] = depth
            byIdMap[current]?.parentIds?.forEach { walk(it, depth + 1) }
        }
        walk(id, 0)
        return result
    }

    /** Generationen insgesamt: längste Vorfahrenkette im Bestand. */
    fun generationCount(): Int =
        (people.maxOfOrNull { ancestorDepths(it.id).values.max() } ?: 0) + 1

    /**
     * Verwandtschaft von [egoId] aus gesehen. Sucht den nächsten gemeinsamen
     * Vorfahren und leitet daraus die Bezeichnung ab; greift sonst auf die
     * Partnerschaft zurück ("Bruders Partnerin").
     */
    fun relationTo(egoId: String, otherId: String): Relation {
        if (egoId == otherId) return Relation.Self
        val other = byIdMap[otherId] ?: return Relation.Unrelated
        val ego = byIdMap[egoId] ?: return Relation.Unrelated
        if (egoId in other.partnerIds || otherId in ego.partnerIds) {
            return Relation.Partner(other.isFemale)
        }
        bloodRelation(egoId, otherId, other.isFemale)?.let { return it }
        other.partnerIds.forEach { partnerId ->
            val viaPartner = bloodRelation(egoId, partnerId, byIdMap[partnerId]?.isFemale ?: false)
            if (viaPartner != null) return Relation.InLaw(viaPartner, other.isFemale)
        }
        return Relation.Unrelated
    }

    private fun bloodRelation(egoId: String, otherId: String, female: Boolean): Relation? {
        val a = ancestorDepths(egoId)
        val b = ancestorDepths(otherId)
        var best: Pair<Int, Int>? = null
        a.forEach { (ancestor, depthEgo) ->
            val depthOther = b[ancestor] ?: return@forEach
            if (best == null || depthEgo + depthOther < best!!.first + best!!.second) {
                best = depthEgo to depthOther
            }
        }
        val (up, down) = best ?: return null
        return when {
            up == 0 -> Relation.Descendant(female, down)
            down == 0 -> Relation.Ancestor(female, up)
            up == 1 && down == 1 -> Relation.Sibling(female)
            up == 1 -> Relation.NieceNephew(female, great = down >= 3)
            down == 1 -> Relation.AuntUncle(female, great = up >= 3)
            else -> Relation.Cousin(female, degree = minOf(up, down) - 1, removed = abs(up - down))
        }
    }
}
