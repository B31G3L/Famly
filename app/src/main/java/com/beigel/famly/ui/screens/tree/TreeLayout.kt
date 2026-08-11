package com.beigel.famly.ui.screens.tree

import androidx.compose.ui.geometry.Offset
import com.beigel.famly.data.model.Person
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Geometrie des Stammbaum-Canvas. Alle Werte in dp (als Float, damit die
 * Layout-Berechnung frei von Compose-/Android-Abhängigkeiten bleibt und in
 * reinen JVM-Unit-Tests laufen kann).
 */
internal const val CARD_W = 148f
internal const val CARD_H = 138f
internal const val GAP_X = 28f
internal const val GAP_Y = 96f
internal const val SLOT_W = CARD_W + GAP_X
internal const val ROW_H = CARD_H + GAP_Y
internal const val CANVAS_PADDING = 48f

/** Abstand zwischen zwei Geschwister-Teilbäumen, in Slot-Einheiten. */
private const val SIBLING_GAP = 0.35f

internal data class TreeNode(
    val person: Person,
    val generation: Int,
    /** Linke obere Ecke der Karte, in dp. */
    val x: Float,
    val y: Float
) {
    val centerX: Float get() = x + CARD_W / 2f
    val centerY: Float get() = y + CARD_H / 2f
    val bottom: Float get() = y + CARD_H
}

/**
 * Verbindungslinie als Polyline aus rechtwinkligen Segmenten (dp).
 * [isPartner] wird nur für die waagrechte Paar-Verbindung gesetzt und darf
 * optisch abweichen (z. B. etwas kräftiger).
 */
internal data class TreeLink(
    val points: List<Offset>,
    val isPartner: Boolean = false
)

internal data class TreeLayout(
    val nodes: List<TreeNode>,
    val links: List<TreeLink>,
    val width: Float,
    val height: Float,
    val generationCount: Int
)

/**
 * Berechnet ein klassisches Stammbaum-Layout aus den Verwandtschafts-Daten.
 *
 * Wichtig: [Person.treePosition]`.slot` wird bewusst IGNORIERT. Der Slot ist im
 * Repository nur ein fortlaufender Zähler ("nächster freier Slot") und hat
 * keinerlei strukturelle Bedeutung - genau daher kamen die scheinbar wirren
 * Linien: Kinder standen irgendwo in ihrer Reihe statt unter ihren Eltern,
 * wodurch die Sammel-Linien quer durch die ganze Generation liefen.
 *
 * Stattdessen:
 *  1. Eltern-Beziehungen normalisieren (parentIds, Fallback über connections).
 *  2. "Unions" bilden: gleiches Eltern-Set = ein Familienzweig.
 *  3. Eltern, die gemeinsame Kinder haben, zu einem Block (Paar) zusammenfassen.
 *  4. Generationen aus der Struktur ableiten (Kind = Eltern + 1, Partner gleich).
 *  5. Breiten bottom-up bestimmen, Positionen top-down verteilen -> Eltern
 *     sitzen immer mittig über ihren Kindern, Geschwister stehen nebeneinander.
 */
internal fun buildTreeLayout(members: List<Person>): TreeLayout {
    val people = members.filter { it.treePosition != null }
    if (people.isEmpty()) return TreeLayout(emptyList(), emptyList(), 0f, 0f, 0)

    val byId = people.associateBy { it.id }
    val byName = people.associateBy { it.name }

    // --- 1) Effektive Eltern -------------------------------------------------
    val parentsOf: Map<String, List<String>> = people.associate { person ->
        val direct = person.parentIds.filter { it != person.id && byId.containsKey(it) }
        val effective = direct.ifEmpty {
            // Altbestand ohne parentIds: aus dem richtungslosen connections-Feld
            // nur die Einträge aus einer HÖHEREN Generation als Eltern werten.
            person.connections
                .mapNotNull { byName[it] }
                .filter { it.id != person.id && it.treePosition!!.generation < person.treePosition!!.generation }
                .map { it.id }
        }
        person.id to effective.distinct().sorted()
    }

    val order = compareBy<Person>({ it.treePosition!!.generation }, { it.treePosition!!.slot }, { it.name })
    val ordered = people.sortedWith(order)

    // --- 2) Unions (Eltern-Set -> Kinder) ------------------------------------
    val unions = LinkedHashMap<List<String>, MutableList<String>>()
    ordered.forEach { child ->
        val parents = parentsOf.getValue(child.id)
        if (parents.isNotEmpty()) unions.getOrPut(parents) { mutableListOf() }.add(child.id)
    }

    // --- 3) Blöcke: Eltern mit gemeinsamen Kindern stehen nebeneinander ------
    val parentIds = unions.keys.flatten().toSet()
    val dsu = Dsu()
    unions.keys.forEach { key -> key.drop(1).forEach { dsu.union(key.first(), it) } }
    // Partner:innen ohne gemeinsame Kinder (partnerId gesetzt, aber kein
    // eigener Eintrag in `unions`) trotzdem als Block zusammenfassen, damit
    // sie im Baum nebeneinander stehen und eine Paar-Linie bekommen.
    val partnerOnlyPairs = people.mapNotNull { person ->
        val partnerId = person.partnerId?.takeIf { byId.containsKey(it) } ?: return@mapNotNull null
        if (person.id < partnerId) person.id to partnerId else null
    }
    partnerOnlyPairs.forEach { (a, b) -> dsu.union(a, b) }

    val blockParticipantIds = parentIds + partnerOnlyPairs.flatMap { (a, b) -> listOf(a, b) }
    val blockOf = HashMap<String, String>()
    ordered.forEach { person ->
        blockOf[person.id] = if (person.id in blockParticipantIds) "u:${dsu.find(person.id)}" else "p:${person.id}"
    }

    val blockMembers = LinkedHashMap<String, MutableList<Person>>()
    ordered.forEach { blockMembers.getOrPut(blockOf.getValue(it.id)) { mutableListOf() }.add(it) }

    // Kind-Blöcke zuordnen. Ein Block kann nur EINEN Elternblock als Anker
    // haben (Partner bringen ihre eigene Herkunftsfamilie mit) - der erste
    // Anspruch gewinnt, die übrige Verbindung wird später als längere Linie
    // gezeichnet.
    val childBlocks = LinkedHashMap<String, MutableList<String>>()
    val claimed = HashSet<String>()
    unions.entries
        .sortedBy { entry -> entry.key.minOf { byId.getValue(it).treePosition!!.generation } }
        .forEach { (parents, children) ->
            val block = blockOf.getValue(parents.first())
            children.forEach { childId ->
                val childBlock = blockOf.getValue(childId)
                if (childBlock != block && claimed.add(childBlock)) {
                    childBlocks.getOrPut(block) { mutableListOf() }.add(childBlock)
                }
            }
        }

    val roots = blockMembers.keys.filter { it !in claimed }

    // --- 4) Generationen aus der Struktur ableiten ---------------------------
    val generation = HashMap<String, Int>()
    people.forEach { generation[it.id] = it.treePosition!!.generation }
    for (pass in 0 until min(people.size + 2, 40)) {
        var changed = false
        ordered.forEach { person ->
            val parents = parentsOf.getValue(person.id)
            if (parents.isNotEmpty()) {
                val target = parents.maxOf { generation.getValue(it) } + 1
                if (generation.getValue(person.id) < target) {
                    generation[person.id] = target
                    changed = true
                }
            }
        }
        blockMembers.values.forEach { group ->
            val target = group.maxOf { generation.getValue(it.id) }
            group.forEach {
                if (generation.getValue(it.id) < target) {
                    generation[it.id] = target
                    changed = true
                }
            }
        }
        if (!changed) break
    }
    val minGeneration = generation.values.min()

    // --- 5) Breiten (bottom-up) ----------------------------------------------
    val widths = HashMap<String, Float>()
    roots.forEach { measure(it, blockMembers, childBlocks, widths, HashSet()) }
    blockMembers.keys.forEach { measure(it, blockMembers, childBlocks, widths, HashSet()) }

    // --- 5b) Positionen (top-down) -------------------------------------------
    val blockLeft = HashMap<String, Float>()
    var cursor = 0f
    roots.forEach { root ->
        place(root, cursor, blockMembers, childBlocks, widths, blockLeft, HashSet())
        cursor += widths.getValue(root) + SIBLING_GAP * 2f
    }
    // Sicherheitsnetz: falls ein Block durch zyklische Daten nicht erreicht
    // wurde, hinten anhängen statt ihn auf 0/0 stapeln zu lassen.
    blockMembers.keys.forEach { block ->
        if (block !in blockLeft) {
            place(block, cursor, blockMembers, childBlocks, widths, blockLeft, HashSet())
            cursor += widths.getValue(block) + SIBLING_GAP * 2f
        }
    }

    val nodes = blockMembers.flatMap { (block, group) ->
        val left = blockLeft[block] ?: 0f
        group.mapIndexed { index, person ->
            val gen = generation.getValue(person.id) - minGeneration
            TreeNode(
                person = person,
                generation = gen,
                x = (left + index) * SLOT_W,
                y = gen * ROW_H
            )
        }
    }

    // --- 6) Linien ------------------------------------------------------------
    val nodeById = nodes.associateBy { it.person.id }
    val links = mutableListOf<TreeLink>()
    partnerOnlyPairs.forEach { (a, b) ->
        val first = nodeById[a] ?: return@forEach
        val second = nodeById[b] ?: return@forEach
        links += TreeLink(
            points = listOf(
                Offset(first.centerX, first.centerY),
                Offset(second.centerX, second.centerY)
            ),
            isPartner = true
        )
    }
    unions.forEach { (parents, children) ->
        val parentNodes = parents.mapNotNull { nodeById[it] }.sortedBy { it.x }
        val childNodes = children.mapNotNull { nodeById[it] }.sortedBy { it.x }
        if (parentNodes.isEmpty() || childNodes.isEmpty()) return@forEach

        val parentBottom = parentNodes.maxOf { it.bottom }
        val childTop = childNodes.minOf { it.y }
        val busY = if (childTop > parentBottom) {
            parentBottom + (childTop - parentBottom) / 2f
        } else {
            parentBottom + GAP_Y / 2f
        }

        val stemX: Float
        val stemY: Float
        if (parentNodes.size >= 2) {
            val first = parentNodes.first()
            val last = parentNodes.last()
            stemX = (first.centerX + last.centerX) / 2f
            val sideBySide = abs(first.y - last.y) < 0.5f &&
                    (last.centerX - first.centerX) <= SLOT_W * 1.05f
            if (sideBySide) {
                // Klassische Paar-Verbindung auf Höhe der Kartenmitte.
                links += TreeLink(
                    points = listOf(
                        Offset(first.centerX, first.centerY),
                        Offset(last.centerX, last.centerY)
                    ),
                    isPartner = true
                )
                stemY = first.centerY
            } else {
                // Eltern stehen nicht nebeneinander (Partner aus einer anderen
                // Herkunftsfamilie): unterhalb der Karten herumführen, damit
                // die Linie keine fremden Karten durchschneidet.
                val linkY = parentBottom + GAP_Y * 0.22f
                links += TreeLink(
                    points = listOf(
                        Offset(first.centerX, first.bottom),
                        Offset(first.centerX, linkY),
                        Offset(last.centerX, linkY),
                        Offset(last.centerX, last.bottom)
                    ),
                    isPartner = true
                )
                stemY = linkY
            }
        } else {
            stemX = parentNodes.first().centerX
            stemY = parentNodes.first().bottom
        }

        childNodes.forEach { child ->
            links += TreeLink(
                simplify(
                    listOf(
                        Offset(stemX, stemY),
                        Offset(stemX, busY),
                        Offset(child.centerX, busY),
                        Offset(child.centerX, child.y)
                    )
                )
            )
        }
    }

    val width = (nodes.maxOf { it.x } + CARD_W) + CANVAS_PADDING * 2
    val height = (nodes.maxOf { it.y } + CARD_H) + CANVAS_PADDING * 2
    val shifted = nodes.map { it.copy(x = it.x + CANVAS_PADDING, y = it.y + CANVAS_PADDING) }
    val shiftedLinks = links.map { link ->
        link.copy(points = link.points.map { Offset(it.x + CANVAS_PADDING, it.y + CANVAS_PADDING) })
    }

    return TreeLayout(
        nodes = shifted,
        links = shiftedLinks,
        width = width,
        height = height,
        generationCount = generation.values.distinct().size
    )
}

private fun measure(
    block: String,
    blockMembers: Map<String, List<Person>>,
    childBlocks: Map<String, List<String>>,
    widths: MutableMap<String, Float>,
    guard: MutableSet<String>
): Float {
    widths[block]?.let { return it }
    val ownWidth = (blockMembers[block]?.size ?: 1).toFloat()
    if (!guard.add(block)) return ownWidth
    val children = childBlocks[block].orEmpty()
    val childrenWidth = if (children.isEmpty()) {
        0f
    } else {
        children.fold(0f) { acc, child ->
            acc + measure(child, blockMembers, childBlocks, widths, guard)
        } + SIBLING_GAP * (children.size - 1)
    }
    val width = max(ownWidth, childrenWidth)
    widths[block] = width
    guard.remove(block)
    return width
}

private fun place(
    block: String,
    bandLeft: Float,
    blockMembers: Map<String, List<Person>>,
    childBlocks: Map<String, List<String>>,
    widths: Map<String, Float>,
    result: MutableMap<String, Float>,
    guard: MutableSet<String>
) {
    if (!guard.add(block)) return
    val width = widths[block] ?: 1f
    val ownWidth = (blockMembers[block]?.size ?: 1).toFloat()
    result[block] = bandLeft + (width - ownWidth) / 2f

    val children = childBlocks[block].orEmpty()
    if (children.isEmpty()) return
    val childrenWidth = children.fold(0f) { acc, child -> acc + (widths[child] ?: 1f) } +
            SIBLING_GAP * (children.size - 1)
    var cursor = bandLeft + (width - childrenWidth) / 2f
    children.forEach { child ->
        place(child, cursor, blockMembers, childBlocks, widths, result, guard)
        cursor += (widths[child] ?: 1f) + SIBLING_GAP
    }
}

/** Entfernt doppelte und auf einer Geraden liegende Zwischenpunkte. */
private fun simplify(points: List<Offset>): List<Offset> {
    val cleaned = mutableListOf<Offset>()
    points.forEach { point ->
        val last = cleaned.lastOrNull()
        if (last == null || abs(last.x - point.x) > 0.01f || abs(last.y - point.y) > 0.01f) {
            cleaned += point
        }
    }
    if (cleaned.size < 3) return cleaned
    val result = mutableListOf(cleaned.first())
    for (i in 1 until cleaned.size - 1) {
        val previous = cleaned[i - 1]
        val current = cleaned[i]
        val next = cleaned[i + 1]
        val collinear = (abs(previous.x - current.x) < 0.01f && abs(current.x - next.x) < 0.01f) ||
                (abs(previous.y - current.y) < 0.01f && abs(current.y - next.y) < 0.01f)
        if (!collinear) result += current
    }
    result += cleaned.last()
    return result
}

private class Dsu {
    private val parent = HashMap<String, String>()

    fun find(node: String): String {
        parent.getOrPut(node) { node }
        var root = node
        while (parent.getValue(root) != root) root = parent.getValue(root)
        var current = node
        while (current != root) {
            val next = parent.getValue(current)
            parent[current] = root
            current = next
        }
        return root
    }

    fun union(a: String, b: String) {
        val rootA = find(a)
        val rootB = find(b)
        if (rootA != rootB) parent[rootA] = rootB
    }
}