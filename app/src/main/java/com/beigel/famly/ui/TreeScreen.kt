package com.beigel.famly.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beigel.famly.R
import com.beigel.famly.data.DemoPerson
import com.beigel.famly.data.FamilyIndex
import com.beigel.famly.tree.EdgeStyle
import com.beigel.famly.tree.TreeLine
import com.beigel.famly.tree.TreeMetrics
import com.beigel.famly.tree.TreeNode
import com.beigel.famly.tree.buildTreeLayout
import com.beigel.famly.tree.downKey
import com.beigel.famly.tree.upKey

private const val MIN_SCALE = 0.3f
private const val MAX_SCALE = 2.2f
private const val START_SCALE = 0.85f

/**
 * Der Baum geht von einer Ausgangsperson aus: nach unten die Nachkommen, nach
 * oben Eltern und Großeltern. Alles Weitere hängt an den Klapp-Knöpfen.
 */
@Composable
fun TreeScreen(
    index: FamilyIndex,
    egoId: String,
    onEgoChange: (String) -> Unit,
    onOpenPerson: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember(egoId) { mutableStateOf(emptySet<String>()) }
    var showSiblings by remember { mutableStateOf(true) }
    var showPartners by remember { mutableStateOf(true) }

    var scale by remember { mutableFloatStateOf(START_SCALE) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    // Hochzählen heißt: neu auf die Ausgangsperson zentrieren.
    var recenterTick by remember { mutableIntStateOf(0) }

    val layout = remember(index, egoId, expanded, showPartners, showSiblings) {
        buildTreeLayout(index, egoId, expanded, showPartners, showSiblings)
    }

    fun toggle(key: String) {
        expanded = if (key in expanded) expanded - key else expanded + key
    }

    Column(modifier.fillMaxSize().background(DemoColors.Background)) {
        TreeToolbar(
            index = index,
            egoId = egoId,
            onEgoChange = onEgoChange,
            showSiblings = showSiblings,
            onToggleSiblings = { showSiblings = !showSiblings },
            showPartners = showPartners,
            onTogglePartners = { showPartners = !showPartners },
            scale = scale,
            onZoom = { factor -> scale = (scale * factor).coerceIn(MIN_SCALE, MAX_SCALE) },
            onRecenter = { recenterTick++ }
        )

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(DemoColors.Background)
                .clipToBounds()
        ) {
            val density = LocalDensity.current
            val viewportWidth = with(density) { maxWidth.toPx() }
            val viewportHeight = with(density) { maxHeight.toPx() }

            fun center() {
                val target = START_SCALE
                val egoX = with(density) { layout.egoX.dp.toPx() }
                val egoY = with(density) { (layout.egoY + TreeMetrics.NODE_HEIGHT / 2f).dp.toPx() }
                scale = target
                offset = Offset(
                    x = viewportWidth / 2f - egoX * target,
                    y = viewportHeight * 0.42f - egoY * target
                )
            }

            // Beim ersten Aufbau, beim Wechsel der Ausgangsperson und auf Knopfdruck.
            LaunchedEffect(egoId, viewportWidth, viewportHeight, recenterTick) { center() }

            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val next = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                            offset = centroid - (centroid - offset) * (next / scale) + pan
                            scale = next
                        }
                    }
            ) {
                Box(
                    Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                        .requiredSize(layout.width.dp, layout.height.dp)
                ) {
                    Canvas(Modifier.matchParentSize()) {
                        layout.edges.forEach { edge ->
                            val color = edgeColor(edge.style)
                            val width = if (edge.style == EdgeStyle.COUPLE) {
                                2.dp.toPx()
                            } else {
                                1.5.dp.toPx()
                            }
                            for (i in 1 until edge.points.size) {
                                drawLine(
                                    color = color.copy(alpha = 0.7f),
                                    start = Offset(edge.points[i - 1].x.dp.toPx(), edge.points[i - 1].y.dp.toPx()),
                                    end = Offset(edge.points[i].x.dp.toPx(), edge.points[i].y.dp.toPx()),
                                    strokeWidth = width,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }

                    layout.nodes.forEach { node ->
                        TreeNodeGroup(
                            node = node,
                            index = index,
                            onOpenPerson = onOpenPerson,
                            onEgoChange = onEgoChange,
                            onToggleUp = { toggle(upKey(node.personId)) },
                            onToggleDown = { toggle(downKey(node.personId)) }
                        )
                    }
                }
            }

            Legend(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            )
        }
    }
}

@Composable
private fun TreeToolbar(
    index: FamilyIndex,
    egoId: String,
    onEgoChange: (String) -> Unit,
    showSiblings: Boolean,
    onToggleSiblings: () -> Unit,
    showPartners: Boolean,
    onTogglePartners: () -> Unit,
    scale: Float,
    onZoom: (Float) -> Unit,
    onRecenter: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val sorted = remember(index) { index.people.sortedBy { it.fullName } }

    Surface(color = DemoColors.Surface, shadowElevation = 0.dp) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DemoColors.Surface,
                        border = BorderStroke(1.dp, DemoColors.Divider),
                        modifier = Modifier.clickable { menuOpen = true }
                    ) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Eyebrow(stringResource(R.string.demo_tree_ego))
                            Text(
                                text = index.require(egoId).fullName,
                                fontSize = 14.sp,
                                color = DemoColors.Ink
                            )
                        }
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        sorted.forEach { person ->
                            DropdownMenuItem(
                                text = { Text("${person.fullName} (${person.birthYear})") },
                                onClick = {
                                    menuOpen = false
                                    onEgoChange(person.id)
                                }
                            )
                        }
                    }
                }

                TogglePill(
                    text = stringResource(R.string.demo_tree_siblings),
                    selected = showSiblings,
                    onClick = onToggleSiblings
                )
                TogglePill(
                    text = stringResource(R.string.demo_tree_partners),
                    selected = showPartners,
                    onClick = onTogglePartners
                )

                IconButton(onClick = { onZoom(0.85f) }) {
                    Icon(Icons.Filled.ZoomOut, stringResource(R.string.demo_zoom_out), tint = DemoColors.Muted)
                }
                Text(
                    text = "${(scale * 100).toInt()}%",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = DemoColors.Faint
                )
                IconButton(onClick = { onZoom(1.18f) }) {
                    Icon(Icons.Filled.ZoomIn, stringResource(R.string.demo_zoom_in), tint = DemoColors.Muted)
                }
                IconButton(onClick = onRecenter) {
                    Icon(Icons.Filled.MyLocation, stringResource(R.string.demo_center), tint = DemoColors.Muted)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(DemoColors.Divider))
        }
    }
}

/** Karte (plus Partnerkarte) samt Klapp-Knöpfen an ihrer Position im Baum. */
@Composable
private fun TreeNodeGroup(
    node: TreeNode,
    index: FamilyIndex,
    onOpenPerson: (String) -> Unit,
    onEgoChange: (String) -> Unit,
    onToggleUp: () -> Unit,
    onToggleDown: () -> Unit
) {
    val person = index.require(node.personId)
    val partner = node.partnerId?.let { index[it] }

    TreePersonCard(
        person = person,
        line = node.line,
        isEgo = node.isEgo,
        onClick = { onOpenPerson(person.id) },
        modifier = Modifier.offset(node.left.dp, node.y.dp)
    )

    if (partner != null) {
        TreePersonCard(
            person = partner,
            line = TreeLine.IN_LAW,
            isEgo = false,
            onClick = { onOpenPerson(partner.id) },
            modifier = Modifier.offset((node.x + TreeMetrics.PARTNER_GAP / 2f).dp, node.y.dp)
        )
    }

    if (node.parentCount > 0) {
        ExpandButton(
            expanded = node.parentsExpanded,
            count = node.parentCount,
            pointsUp = true,
            onClick = onToggleUp,
            modifier = Modifier.offset((node.x - 22f).dp, (node.y - 26f).dp)
        )
    }
    if (node.childCount > 0) {
        ExpandButton(
            expanded = node.childrenExpanded,
            count = node.childCount,
            pointsUp = false,
            onClick = onToggleDown,
            modifier = Modifier.offset(
                (node.x - 22f).dp,
                (node.y + TreeMetrics.NODE_HEIGHT + 4f).dp
            )
        )
    }
    if (node.siblingChildCount != null) {
        val label = if (node.siblingChildCount > 0) {
            stringResource(R.string.demo_sibling_children, node.siblingChildCount)
        } else {
            stringResource(R.string.demo_sibling_focus)
        }
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = DemoColors.Surface,
            border = BorderStroke(1.dp, DemoColors.Divider),
            modifier = Modifier
                .offset((node.x - 60f).dp, (node.y + TreeMetrics.NODE_HEIGHT + 4f).dp)
                .requiredSize(120.dp, 24.dp)
                .clickable { onEgoChange(node.personId) }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = label, fontSize = 11.sp, color = DemoColors.Muted, maxLines = 1)
            }
        }
    }
}

@Composable
private fun TreePersonCard(
    person: DemoPerson,
    line: TreeLine,
    isEgo: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = cardStyleFor(line, isEgo)
    Box(modifier.requiredSize(TreeMetrics.NODE_WIDTH.dp, TreeMetrics.NODE_HEIGHT.dp)) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = style.background,
            border = BorderStroke(1.dp, style.border),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxSize().clickable(onClick = onClick)
        ) {
            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Box(
                        Modifier.size(34.dp).background(style.avatarBackground, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = person.initials,
                            color = style.avatarText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Column {
                        Text(
                            text = person.fullName,
                            fontFamily = FontFamily.Serif,
                            fontSize = 14.sp,
                            color = style.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = person.lifeSpan(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = style.subtitle,
                            maxLines = 1
                        )
                    }
                }
                val caption = person.birthName?.let { stringResource(R.string.demo_birth_name, it) }
                    ?: person.job.ifBlank { person.birthPlace }
                Text(
                    text = caption,
                    fontSize = 11.sp,
                    color = style.subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        if (isEgo) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = DemoColors.Ink,
                border = BorderStroke(1.dp, Color.White),
                modifier = Modifier.align(Alignment.TopStart).offset(10.dp, (-8).dp)
            ) {
                Text(
                    text = stringResource(R.string.demo_badge_me),
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpandButton(
    expanded: Boolean,
    count: Int,
    pointsUp: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when {
        expanded && pointsUp -> Icons.Filled.ExpandMore
        expanded -> Icons.Filled.ExpandLess
        pointsUp -> Icons.Filled.ExpandLess
        else -> Icons.Filled.ExpandMore
    }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = DemoColors.Surface,
        border = BorderStroke(1.dp, DemoColors.Divider),
        shadowElevation = 1.dp,
        modifier = modifier.requiredSize(44.dp, 22.dp).clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = if (expanded) {
                    stringResource(R.string.demo_collapse)
                } else {
                    stringResource(R.string.demo_expand_more, count)
                },
                tint = DemoColors.Muted,
                modifier = Modifier.size(14.dp)
            )
            if (!expanded) {
                Text(
                    text = count.toString(),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = DemoColors.Muted
                )
            }
        }
    }
}

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = DemoColors.Surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, DemoColors.Divider)
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Eyebrow(stringResource(R.string.demo_legend))
            LegendDot(DemoColors.Paternal, stringResource(R.string.demo_legend_paternal))
            LegendDot(DemoColors.Maternal, stringResource(R.string.demo_legend_maternal))
            LegendDot(DemoColors.Descendant, stringResource(R.string.demo_legend_descendants))
            LegendDot(DemoColors.Neutral, stringResource(R.string.demo_legend_inlaw))
        }
    }
}
