package com.beigel.famly.tree

import com.beigel.famly.data.FamilyIndex

/**
 * Maße des Baums in dp. Die Layout-Berechnung arbeitet komplett in dp, die UI
 * setzt die Karten per `offset(x.dp, y.dp)` und rechnet für die Linien im
 * Canvas mit `x.dp.toPx()` um.
 */
object TreeMetrics {
    const val NODE_WIDTH = 178f
    const val NODE_HEIGHT = 86f
    /** Abstand von Reihenoberkante zu Reihenoberkante. */
    const val ROW_HEIGHT = 186f
    const val H_GAP = 26f
    /** Lücke zwischen Person und Partner:in innerhalb eines Paar-Blocks. */
    const val PARTNER_GAP = 24f
    const val PADDING = 90f
    const val MAX_DEPTH = 8
}

/** Linienzugehörigkeit - trägt Information, nicht Dekoration. */
enum class TreeLine { EGO, PATERNAL, MATERNAL, DESCENDANT, IN_LAW }

/** Rolle einer gezeichneten Verbindung, die UI mappt das auf Farben. */
enum class EdgeStyle { COUPLE, EGO, DESCENDANT, PATERNAL, MATERNAL, NEUTRAL }

data class TreePoint(val x: Float, val y: Float)

/** Polylinie: die UI verbindet die Punkte der Reihe nach. */
data class TreeEdge(val points: List<TreePoint>, val style: EdgeStyle)

/**
 * Eine Karte im Baum. Bei gesetztem [partnerId] steht die Partnerkarte rechts
 * daneben, [x] ist dann die Mitte des gesamten Paar-Blocks.
 */
data class TreeNode(
    val key: String,
    val personId: String,
    val partnerId: String?,
    val x: Float,
    val y: Float,
    val unitWidth: Float,
    val line: TreeLine,
    val isEgo: Boolean = false,
    /** Anzahl noch nicht gezeigter Elternteile (0 = kein Knopf). */
    val parentCount: Int = 0,
    val parentsExpanded: Boolean = false,
    val childCount: Int = 0,
    val childrenExpanded: Boolean = false,
    /** Geschwisterkarte: Anzahl eigener Kinder, sichtbar nach Fokuswechsel. */
    val siblingChildCount: Int? = null
) {
    val left: Float get() = x - unitWidth / 2f
}

data class TreeLayout(
    val nodes: List<TreeNode>,
    val edges: List<TreeEdge>,
    val width: Float,
    val height: Float,
    val egoX: Float,
    val egoY: Float
)

/** Schlüssel für den Auf-/Zuklapp-Zustand einer Person. */
fun upKey(id: String) = "u:$id"
fun downKey(id: String) = "d:$id"

/**
 * Baut den ego-zentrierten Baum:
 *
 * - nach unten die Nachkommen (Kinder standardmäßig offen, tiefer auf Klick),
 * - nach oben die Vorfahren (Eltern und Großeltern standardmäßig offen),
 * - optional die Geschwister links in der Ego-Reihe.
 *
 * [expanded] enthält die Personen, deren Standardzustand umgedreht wurde -
 * ein Eintrag klappt also je nach Ebene auf oder zu.
 */
fun buildTreeLayout(
    index: FamilyIndex,
    egoId: String,
    expanded: Set<String> = emptySet(),
    showPartners: Boolean = true,
    showSiblings: Boolean = true
): TreeLayout {

    fun isOpen(key: String, default: Boolean) = if (key in expanded) !default else default

    // ── Phase 1: Teilbaumbreiten bestimmen ───────────────────────────────────

    class DownBranch(
        val id: String,
        val partnerId: String?,
        val depth: Int,
        val unitWidth: Float,
        val childrenWidth: Float,
        val width: Float,
        val children: List<DownBranch>,
        val childCount: Int,
        val open: Boolean
    )

    fun buildDown(id: String, depth: Int): DownBranch {
        val partnerId = if (showPartners) index.require(id).partnerIds.firstOrNull() else null
        val unitWidth =
            if (partnerId != null) TreeMetrics.NODE_WIDTH * 2 + TreeMetrics.PARTNER_GAP
            else TreeMetrics.NODE_WIDTH
        val childIds = index.childrenOfUnit(id, partnerId)
        val open = isOpen(downKey(id), depth == 0) &&
            childIds.isNotEmpty() &&
            depth < TreeMetrics.MAX_DEPTH
        val children = if (open) childIds.map { buildDown(it, depth + 1) } else emptyList()
        val childrenWidth =
            children.sumOf { it.width.toDouble() }.toFloat() +
                maxOf(0, children.size - 1) * TreeMetrics.H_GAP
        return DownBranch(
            id = id,
            partnerId = partnerId,
            depth = depth,
            unitWidth = unitWidth,
            childrenWidth = childrenWidth,
            width = maxOf(unitWidth, childrenWidth),
            children = children,
            childCount = childIds.size,
            open = open
        )
    }

    class UpBranch(
        val id: String,
        val depth: Int,
        val childrenWidth: Float,
        val width: Float,
        val parents: List<UpBranch>,
        val parentCount: Int,
        val open: Boolean
    )

    fun buildUp(id: String, depth: Int): UpBranch {
        val parentIds = index.parentsOf(id)
        val open = isOpen(upKey(id), depth < 2) &&
            parentIds.isNotEmpty() &&
            depth < TreeMetrics.MAX_DEPTH
        val parents = if (open) parentIds.map { buildUp(it, depth + 1) } else emptyList()
        val childrenWidth =
            parents.sumOf { it.width.toDouble() }.toFloat() +
                maxOf(0, parents.size - 1) * TreeMetrics.H_GAP
        return UpBranch(
            id = id,
            depth = depth,
            childrenWidth = childrenWidth,
            width = maxOf(TreeMetrics.NODE_WIDTH, childrenWidth),
            parents = parents,
            parentCount = parentIds.size,
            open = open
        )
    }

    val downRoot = buildDown(egoId, 0)
    val upRoot = buildUp(egoId, 0)

    // ── Phase 2: absolute Koordinaten vergeben ───────────────────────────────

    val nodes = mutableListOf<TreeNode>()
    val edges = mutableListOf<TreeEdge>()

    fun placeDown(branch: DownBranch, startX: Float) {
        val centerX = startX + branch.width / 2f
        val y = branch.depth * TreeMetrics.ROW_HEIGHT
        nodes += TreeNode(
            key = downKey(branch.id),
            personId = branch.id,
            partnerId = branch.partnerId,
            x = centerX,
            y = y,
            unitWidth = branch.unitWidth,
            line = if (branch.depth == 0) TreeLine.EGO else TreeLine.DESCENDANT,
            isEgo = branch.depth == 0,
            parentCount = if (branch.depth == 0) index.parentsOf(branch.id).size else 0,
            parentsExpanded = branch.depth == 0 && upRoot.open,
            childCount = branch.childCount,
            childrenExpanded = branch.open
        )

        if (branch.partnerId != null) {
            edges += TreeEdge(
                listOf(
                    TreePoint(centerX - TreeMetrics.PARTNER_GAP / 2f - 1f, y + TreeMetrics.NODE_HEIGHT / 2f),
                    TreePoint(centerX + TreeMetrics.PARTNER_GAP / 2f + 1f, y + TreeMetrics.NODE_HEIGHT / 2f)
                ),
                EdgeStyle.COUPLE
            )
        }
        if (branch.children.isEmpty()) return

        val stemStart =
            if (branch.partnerId != null) y + TreeMetrics.NODE_HEIGHT / 2f
            else y + TreeMetrics.NODE_HEIGHT
        val midY = y + TreeMetrics.NODE_HEIGHT +
            (TreeMetrics.ROW_HEIGHT - TreeMetrics.NODE_HEIGHT) / 2f
        val childY = (branch.depth + 1) * TreeMetrics.ROW_HEIGHT
        val style = if (branch.depth == 0) EdgeStyle.EGO else EdgeStyle.DESCENDANT

        var cursor = startX + (branch.width - branch.childrenWidth) / 2f
        val childCenters = mutableListOf<Float>()
        branch.children.forEach { child ->
            placeDown(child, cursor)
            childCenters += cursor + child.width / 2f
            cursor += child.width + TreeMetrics.H_GAP
        }

        edges += TreeEdge(listOf(TreePoint(centerX, stemStart), TreePoint(centerX, midY)), style)
        if (childCenters.size > 1) {
            edges += TreeEdge(
                listOf(
                    TreePoint(childCenters.min(), midY),
                    TreePoint(childCenters.max(), midY)
                ),
                style
            )
        }
        childCenters.forEach { cx ->
            edges += TreeEdge(listOf(TreePoint(cx, midY), TreePoint(cx, childY)), style)
        }
    }

    fun placeUp(branch: UpBranch, startX: Float, line: TreeLine) {
        val centerX = startX + branch.width / 2f
        val y = -branch.depth * TreeMetrics.ROW_HEIGHT
        if (branch.depth > 0) {
            nodes += TreeNode(
                key = upKey(branch.id),
                personId = branch.id,
                partnerId = null,
                x = centerX,
                y = y,
                unitWidth = TreeMetrics.NODE_WIDTH,
                line = line,
                parentCount = branch.parentCount,
                parentsExpanded = branch.open
            )
        }
        if (branch.parents.isEmpty()) return

        val parentY = -(branch.depth + 1) * TreeMetrics.ROW_HEIGHT
        val midY = parentY + TreeMetrics.NODE_HEIGHT +
            (TreeMetrics.ROW_HEIGHT - TreeMetrics.NODE_HEIGHT) / 2f

        var cursor = startX + (branch.width - branch.childrenWidth) / 2f
        val parentCenters = mutableListOf<Float>()
        branch.parents.forEach { parent ->
            val parentLine = if (branch.depth == 0) {
                if (index[parent.id]?.isFemale == false) TreeLine.PATERNAL else TreeLine.MATERNAL
            } else {
                line
            }
            placeUp(parent, cursor, parentLine)
            parentCenters += cursor + parent.width / 2f
            cursor += parent.width + TreeMetrics.H_GAP
        }

        val style = when {
            branch.depth == 0 -> EdgeStyle.NEUTRAL
            line == TreeLine.PATERNAL -> EdgeStyle.PATERNAL
            line == TreeLine.MATERNAL -> EdgeStyle.MATERNAL
            else -> EdgeStyle.NEUTRAL
        }

        if (parentCenters.size > 1) {
            val leftX = parentCenters.min()
            val rightX = parentCenters.max()
            val coupleY = parentY + TreeMetrics.NODE_HEIGHT / 2f
            edges += TreeEdge(
                listOf(TreePoint(leftX, coupleY), TreePoint(rightX, coupleY)),
                EdgeStyle.COUPLE
            )
            val midX = (leftX + rightX) / 2f
            edges += TreeEdge(
                listOf(
                    TreePoint(midX, coupleY),
                    TreePoint(midX, midY),
                    TreePoint(centerX, midY),
                    TreePoint(centerX, y)
                ),
                style
            )
        } else {
            edges += TreeEdge(
                listOf(
                    TreePoint(parentCenters.first(), parentY + TreeMetrics.NODE_HEIGHT),
                    TreePoint(parentCenters.first(), midY),
                    TreePoint(centerX, midY),
                    TreePoint(centerX, y)
                ),
                style
            )
        }
    }

    placeDown(downRoot, 0f)
    val egoNode = nodes.first { it.key == downKey(egoId) }
    placeUp(upRoot, egoNode.x - upRoot.width / 2f, TreeLine.EGO)

    // ── Geschwister links neben der Ausgangsperson, gleiche Reihe ────────────

    if (showSiblings) {
        val parentNodes = index.parentsOf(egoId).mapNotNull { parentId ->
            nodes.firstOrNull { it.key == upKey(parentId) }
        }
        var x = egoNode.x - egoNode.unitWidth / 2f - TreeMetrics.H_GAP - TreeMetrics.NODE_WIDTH / 2f
        index.siblingsOf(egoId).forEach { siblingId ->
            nodes += TreeNode(
                key = "s:$siblingId",
                personId = siblingId,
                partnerId = null,
                x = x,
                y = 0f,
                unitWidth = TreeMetrics.NODE_WIDTH,
                line = TreeLine.IN_LAW,
                siblingChildCount = index.childrenOf(siblingId).size
            )
            if (parentNodes.isNotEmpty()) {
                val midX =
                    if (parentNodes.size > 1) (parentNodes.minOf { it.x } + parentNodes.maxOf { it.x }) / 2f
                    else parentNodes.first().x
                val midY = -TreeMetrics.ROW_HEIGHT + TreeMetrics.NODE_HEIGHT +
                    (TreeMetrics.ROW_HEIGHT - TreeMetrics.NODE_HEIGHT) / 2f
                edges += TreeEdge(
                    listOf(TreePoint(midX, midY), TreePoint(x, midY), TreePoint(x, 0f)),
                    EdgeStyle.NEUTRAL
                )
            }
            x -= TreeMetrics.NODE_WIDTH + TreeMetrics.H_GAP
        }
    }

    // ── Phase 3: in den positiven Bereich schieben ───────────────────────────

    val minX = nodes.minOf { it.x - it.unitWidth / 2f } - TreeMetrics.PADDING
    val maxX = nodes.maxOf { it.x + it.unitWidth / 2f } + TreeMetrics.PADDING
    val minY = nodes.minOf { it.y } - TreeMetrics.PADDING
    val maxY = nodes.maxOf { it.y + TreeMetrics.NODE_HEIGHT } + TreeMetrics.PADDING
    val dx = -minX
    val dy = -minY

    val shiftedNodes = nodes.map { it.copy(x = it.x + dx, y = it.y + dy) }
    val shiftedEdges = edges.map { edge ->
        edge.copy(points = edge.points.map { TreePoint(it.x + dx, it.y + dy) })
    }
    val ego = shiftedNodes.first { it.key == downKey(egoId) }

    return TreeLayout(
        nodes = shiftedNodes,
        edges = shiftedEdges,
        width = maxX - minX,
        height = maxY - minY,
        egoX = ego.x,
        egoY = ego.y
    )
}
