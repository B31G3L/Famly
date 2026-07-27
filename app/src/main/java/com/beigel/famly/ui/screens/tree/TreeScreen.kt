package com.beigel.famly.ui.screens.tree

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beigel.famly.data.model.Person
import com.beigel.famly.ui.components.FamlyAvatar
import com.beigel.famly.ui.theme.FamlyAccentOrange
import com.beigel.famly.ui.theme.FamlyGenColors
import com.beigel.famly.ui.theme.FamlyPetrolPrimary
import com.beigel.famly.ui.theme.FamlyTextPrimary
import com.beigel.famly.ui.theme.FamlyTextSecondary
import com.beigel.famly.ui.theme.FamlyTreeLine
import com.beigel.famly.ui.theme.FamlyWhite
import kotlin.math.max
import kotlin.math.min

private val TreeCanvasBackground = Color(0xFFFAF6EF)
private val CardW = 148.dp
private val CardH = 138.dp
private val GapX = 28.dp
private val GapY = 90.dp
private const val MIN_SCALE = 0.4f
private const val MAX_SCALE = 1.4f

private data class TreeNodeLayout(
    val person: Person,
    val left: androidx.compose.ui.unit.Dp,
    val top: androidx.compose.ui.unit.Dp,
    val color: Color
)

private data class TreeSegment(
    val from: Offset,
    val to: Offset
)

/**
 * Baum-Darstellung als frei verschieb- und zoombarer Canvas (Pan + Pinch),
 * analog zum "Famly_dc"-Handoff. Personen werden anhand von generation/slot
 * aus [Person.treePosition] platziert. Verbindungslinien folgen klassischer
 * Stammbaum-Optik: Elternpaare werden waagrecht verbunden, darunter hängen
 * die Kinder mittig an einer gemeinsamen Sammel-Linie (abgeleitet aus
 * [Person.parentIds]). Für ältere Einträge ohne parentIds greift ein
 * Fallback über das generische [Person.connections]-Feld.
 */
@Composable
fun TreeScreen(
    members: List<Person>,
    onPersonClick: (Person) -> Unit,
    onOpenSelf: () -> Unit,
    focusPersonId: String? = null,
    selfPersonId: String = "ich"
) {
    val placed = members.filter { it.treePosition != null }
    val byGeneration = placed.groupBy { it.treePosition!!.generation }.toSortedMap()
    val maxSlotsInRow = byGeneration.values.maxOfOrNull { row -> row.size } ?: 1
    val rowWidthDp = CardW * maxSlotsInRow + GapX * max(0, maxSlotsInRow - 1)

    val nodes = remember(placed) {
        byGeneration.flatMap { (generation, row) ->
            val sorted = row.sortedBy { it.treePosition!!.slot }
            val thisRowWidth = CardW * sorted.size + GapX * max(0, sorted.size - 1)
            val startX = (rowWidthDp - thisRowWidth) / 2
            sorted.mapIndexed { index, person ->
                TreeNodeLayout(
                    person = person,
                    left = startX + (CardW + GapX) * index,
                    top = (CardH + GapY) * generation,
                    color = FamlyGenColors[generation.mod(FamlyGenColors.size)]
                )
            }
        }
    }

    // Klassische Stammbaum-Optik:
    // - Eltern(paare) werden auf Höhe der Kartenmitte waagrecht verbunden.
    // - Von der Mitte dieser Paar-Linie (bzw. direkt vom einzelnen Elternteil,
    //   falls nur einer bekannt ist) geht ein Stamm nach unten zu einer
    //   Sammel-Linie auf halber Höhe zwischen Eltern- und Kinder-Reihe.
    // - Von der Sammel-Linie zweigen senkrechte Linien zu jedem Kind ab.
    // Gruppiert wird über [Person.parentIds] (Geschwister mit demselben
    // Eltern-Set landen automatisch am selben Stamm).
    val segments = remember(nodes) {
        val nodesById = nodes.associateBy { it.person.id }
        val result = mutableListOf<TreeSegment>()

        val groups = nodes
            .mapNotNull { child ->
                val parentIds = child.person.parentIds.filter { nodesById.containsKey(it) }
                if (parentIds.isEmpty()) null else parentIds.sorted().joinToString("|") to child
            }
            .groupBy({ it.first }, { it.second })

        groups.forEach { (key, children) ->
            val parents = key.split("|").mapNotNull { nodesById[it] }
            if (parents.isEmpty()) return@forEach

            val parentCenterY = parents.map { (it.top + CardH / 2).value }.average().toFloat()
            val parentBottomY = parents.maxOf { (it.top + CardH).value }
            val childTopY = children.minOf { it.top.value }
            val busY = parentBottomY + (childTopY - parentBottomY) / 2f

            val stemX = if (parents.size >= 2) {
                val sortedParents = parents.sortedBy { it.left.value }
                val a = sortedParents.first()
                val b = sortedParents.last()
                val aX = (a.left + CardW / 2).value
                val bX = (b.left + CardW / 2).value
                // Waagrechte Paar-Verbindung auf Höhe der Kartenmitte.
                result += TreeSegment(Offset(aX, parentCenterY), Offset(bX, parentCenterY))
                (aX + bX) / 2f
            } else {
                (parents.first().left + CardW / 2).value
            }

            // Stamm von der Paar-Mitte (bzw. dem einzelnen Elternteil) nach
            // unten zur Sammel-Linie.
            val stemStartY = if (parents.size >= 2) parentCenterY else parentBottomY
            result += TreeSegment(Offset(stemX, stemStartY), Offset(stemX, busY))

            // Sammel-Linie IMMER zeichnen (auch bei nur einem Kind) und dabei
            // den Stamm mit einbeziehen - sonst hängt die Verbindung "in der
            // Luft", falls das Kind nicht exakt unter dem Elternteil/der
            // Paar-Mitte platziert ist (der Baum zentriert Kinder aktuell
            // nicht automatisch unter ihren Eltern).
            val busXs = children.map { (it.left + CardW / 2).value } + stemX
            result += TreeSegment(Offset(busXs.min(), busY), Offset(busXs.max(), busY))
            children.forEach { childNode ->
                val childX = (childNode.left + CardW / 2).value
                result += TreeSegment(Offset(childX, busY), Offset(childX, childNode.top.value))
            }
        }

        // Fallback für ältere Baum-Einträge ohne gespeicherte parentIds (nur
        // das generische connections-Feld vorhanden): einfache Verbindung wie
        // bisher, damit bestehende Bäume nicht plötzlich ohne Linien dastehen.
        val handledIds = groups.values.flatten().map { it.person.id }.toHashSet()
        val nodesByName = nodes.associateBy { it.person.name }
        val seen = HashSet<String>()
        nodes.forEach { node ->
            if (node.person.id in handledIds) return@forEach
            node.person.connections.forEach { connectionName ->
                val target = nodesByName[connectionName] ?: return@forEach
                val pairKey = listOf(node.person.id, target.person.id).sorted().joinToString("-")
                if (seen.add(pairKey)) {
                    val (upper, lower) = if (node.top.value <= target.top.value) node to target else target to node
                    val upperX = (upper.left + CardW / 2).value
                    val lowerX = (lower.left + CardW / 2).value
                    val upperBottomY = (upper.top + CardH).value
                    val midY = upperBottomY + (lower.top.value - upperBottomY) / 2f
                    result += TreeSegment(Offset(upperX, upperBottomY), Offset(upperX, midY))
                    result += TreeSegment(Offset(upperX, midY), Offset(lowerX, midY))
                    result += TreeSegment(Offset(lowerX, midY), Offset(lowerX, lower.top.value))
                }
            }
        }

        result
    }

    val canvasWidth = rowWidthDp + 80.dp
    val canvasHeight = (CardH + GapY) * (byGeneration.keys.maxOrNull() ?: 0) + CardH + 80.dp

    var scale by remember { mutableFloatStateOf(0.85f) }
    var offsetX by remember { mutableFloatStateOf(24f) }
    var offsetY by remember { mutableFloatStateOf(24f) }
    var hasCenteredOnFocus by remember(focusPersonId) { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(22.dp, 22.dp, 22.dp, 10.dp)) {
                Text("Stammbaum", style = MaterialTheme.typography.titleLarge)
                Text(
                    "${placed.size} Personen · ${byGeneration.size} Generationen",
                    style = MaterialTheme.typography.bodySmall,
                    color = FamlyTextSecondary
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TreeCanvasBackground)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    }
            ) {
                val density = LocalDensity.current
                val viewportWidthPx = with(density) { maxWidth.toPx() }
                val viewportHeightPx = with(density) { maxHeight.toPx() }

                LaunchedEffect(focusPersonId, nodes) {
                    if (hasCenteredOnFocus) return@LaunchedEffect
                    val target = nodes.find { it.person.id == focusPersonId } ?: return@LaunchedEffect
                    val centerXPx = with(density) { (target.left + CardW / 2).toPx() }
                    val centerYPx = with(density) { (target.top + CardH / 2).toPx() }
                    offsetX = viewportWidthPx / 2f - centerXPx * scale
                    offsetY = viewportHeightPx / 2f - centerYPx * scale
                    hasCenteredOnFocus = true
                }

                Box(
                    modifier = Modifier
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY,
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                        )
                        .size(canvasWidth, canvasHeight)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        segments.forEach { segment ->
                            val fromPx = Offset(segment.from.x.dp.toPx(), segment.from.y.dp.toPx())
                            val toPx = Offset(segment.to.x.dp.toPx(), segment.to.y.dp.toPx())
                            drawLine(
                                color = FamlyTreeLine,
                                start = fromPx,
                                end = toPx,
                                strokeWidth = 3f,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }

                    nodes.forEach { node ->
                        TreeCard(
                            node = node,
                            onClick = { onPersonClick(node.person) },
                            highlighted = node.person.id == focusPersonId,
                            isSelf = node.person.id == selfPersonId,
                            modifier = Modifier.offset(x = node.left, y = node.top)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ZoomButton(icon = Icons.Filled.Add, onClick = { scale = min(MAX_SCALE, scale + 0.15f) })
            ZoomButton(icon = Icons.Filled.Remove, onClick = { scale = max(MIN_SCALE, scale - 0.15f) })
        }

        // Immer erreichbarer Weg zurück zu "Ich", unabhängig davon, wohin man
        // im Baum gepannt/gezoomt hat.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 14.dp)
                .size(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(FamlyPetrolPrimary)
                .clickable(onClick = onOpenSelf),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Filled.Person, contentDescription = "Zu mir", tint = FamlyWhite)
        }
    }
}

@Composable
private fun TreeCard(node: TreeNodeLayout, onClick: () -> Unit, highlighted: Boolean = false, isSelf: Boolean = false, modifier: Modifier = Modifier) {
    val person = node.person
    val sub = if (person.isDeceased) person.birthDate.ifBlank { "verstorben" } else person.birthDate
    // "Ich" bekommt eine dauerhafte Umrandung, damit man sich im Baum immer
    // sofort wiederfindet - unabhängig vom temporären Fokus-Highlight (z. B.
    // nach dem Anlegen einer neuen Person), das weiterhin Vorrang/eigene
    // Farbe hat, falls beides gleichzeitig zutrifft.
    val borderColor = when {
        highlighted -> FamlyAccentOrange
        isSelf -> FamlyPetrolPrimary
        else -> null
    }
    Column(
        modifier = modifier
            .width(CardW)
            .clip(RoundedCornerShape(18.dp))
            .then(
                if (borderColor != null) {
                    Modifier.border(2.5.dp, borderColor, RoundedCornerShape(18.dp))
                } else {
                    Modifier
                }
            )
            .background(FamlyWhite)
            .clickable(onClick = onClick)
            .padding(top = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FamlyAvatar(initial = person.initial, accent = node.color, size = 48, cornerRadius = 24)
        Text(
            person.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = FamlyTextPrimary,
            maxLines = 1
        )
        if (sub.isNotBlank()) {
            Text(sub, fontSize = 11.5.sp, color = FamlyTextSecondary, fontWeight = FontWeight.Bold)
        }
        Box(modifier = Modifier.padding(bottom = 12.dp))
    }
}

@Composable
private fun ZoomButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(50))
            .background(FamlyWhite)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = FamlyPetrolPrimary, modifier = Modifier.size(18.dp))
    }
}