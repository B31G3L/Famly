package com.beigel.famly.ui.screens.tree

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlinx.coroutines.launch
import kotlin.math.min

private val TreeCanvasBackground = Color(0xFFFAF6EF)
private val LineWidth = 1.6.dp
private val PartnerLineWidth = 2.2.dp
private val LineCornerRadius = 12.dp
private const val MIN_SCALE = 0.25f
private const val MAX_SCALE = 1.6f
private const val FOCUS_SCALE = 0.9f
private const val FOCUS_ANIMATION_MS = 480
private const val REFLOW_ANIMATION_MS = 420

/**
 * Baum-Darstellung als frei verschieb- und zoombarer Canvas (Pan + Pinch).
 * Positionen und Verbindungslinien kommen komplett aus [buildTreeLayout] -
 * dieser Screen ist reine Darstellung.
 *
 * [focusPersonId] darf eine Person benennen, die noch NICHT in [members]
 * enthalten ist (z. B. direkt nach dem Speichern, bevor der Firestore-Snapshot
 * durch ist). Der Screen wartet in dem Fall und fährt die Person an, sobald sie
 * auftaucht - statt den Fokus stillschweigend zu verwerfen.
 */
@Composable
fun TreeScreen(
    members: List<Person>,
    onPersonClick: (Person) -> Unit,
    onOpenSelf: () -> Unit,
    focusPersonId: String? = null,
    selfPersonId: String = "ich"
) {
    val layout = remember(members) { buildTreeLayout(members) }
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(FOCUS_SCALE) }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    // Merkt sich, auf wen zuletzt scharfgestellt wurde. Verhindert, dass der
    // Baum bei jedem Rücksprung aus der Detailansicht erneut wegspringt.
    var centeredFor by rememberSaveable { mutableStateOf<String?>(null) }
    var hasSettled by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(22.dp, 22.dp, 22.dp, 10.dp)) {
                Text("Stammbaum", style = MaterialTheme.typography.titleLarge)
                Text(
                    "${layout.nodes.size} Personen · ${layout.generationCount} Generationen",
                    style = MaterialTheme.typography.bodySmall,
                    color = FamlyTextSecondary
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TreeCanvasBackground)
            ) {
                val density = LocalDensity.current
                val viewportWidth = with(density) { maxWidth.toPx() }
                val viewportHeight = with(density) { maxHeight.toPx() }
                val contentWidth = with(density) { layout.width.dp.toPx() }
                val contentHeight = with(density) { layout.height.dp.toPx() }

                fun clampX(value: Float, currentScale: Float) =
                    clampOffset(value, contentWidth * currentScale, viewportWidth)

                fun clampY(value: Float, currentScale: Float) =
                    clampOffset(value, contentHeight * currentScale, viewportHeight)

                /** Offset, bei dem [node] mittig im Viewport liegt. */
                fun offsetCenteredOn(node: TreeNode, targetScale: Float): Offset {
                    val centerX = with(density) { (node.x + CARD_W / 2f).dp.toPx() }
                    val centerY = with(density) { (node.y + CARD_H / 2f).dp.toPx() }
                    return Offset(
                        clampX(viewportWidth / 2f - centerX * targetScale, targetScale),
                        clampY(viewportHeight / 2f - centerY * targetScale, targetScale)
                    )
                }

                fun applyZoom(newScale: Float, focalX: Float, focalY: Float, animate: Boolean) {
                    val current = scale.value
                    val target = newScale.coerceIn(MIN_SCALE, MAX_SCALE)
                    if (target == current) return
                    val factor = target / current
                    val targetX = clampX(focalX - (focalX - offsetX.value) * factor, target)
                    val targetY = clampY(focalY - (focalY - offsetY.value) * factor, target)
                    scope.launch {
                        if (animate) {
                            launch { scale.animateTo(target, tween(180)) }
                            launch { offsetX.animateTo(targetX, tween(180)) }
                            launch { offsetY.animateTo(targetY, tween(180)) }
                        } else {
                            scale.snapTo(target)
                            offsetX.snapTo(targetX)
                            offsetY.snapTo(targetY)
                        }
                    }
                }

                LaunchedEffect(focusPersonId, layout, viewportWidth, viewportHeight) {
                    if (layout.nodes.isEmpty() || viewportWidth <= 0f || viewportHeight <= 0f) {
                        return@LaunchedEffect
                    }

                    val target = focusPersonId?.let { id -> layout.nodes.find { it.person.id == id } }
                    if (target != null) {
                        // Schon dort? Dann nicht erneut wegspringen.
                        if (centeredFor == focusPersonId) return@LaunchedEffect
                        val destination = offsetCenteredOn(target, FOCUS_SCALE)
                        if (hasSettled) {
                            // Sichtbar hinfahren, damit klar wird, wo die Person
                            // im Baum gelandet ist.
                            val spec = tween<Float>(FOCUS_ANIMATION_MS, easing = FastOutSlowInEasing)
                            launch { scale.animateTo(FOCUS_SCALE, spec) }
                            launch { offsetX.animateTo(destination.x, spec) }
                            launch { offsetY.animateTo(destination.y, spec) }
                        } else {
                            scale.snapTo(FOCUS_SCALE)
                            offsetX.snapTo(destination.x)
                            offsetY.snapTo(destination.y)
                        }
                        centeredFor = focusPersonId
                        hasSettled = true
                        return@LaunchedEffect
                    }

                    // Fokus-Person (noch) nicht da: erst einmal eine sinnvolle
                    // Startansicht zeigen. Sobald der Snapshot die Person
                    // nachliefert, läuft dieser Effekt erneut und fährt hin.
                    if (hasSettled) return@LaunchedEffect
                    val self = layout.nodes.find { it.person.id == selfPersonId }
                    if (self != null) {
                        val destination = offsetCenteredOn(self, FOCUS_SCALE)
                        scale.snapTo(FOCUS_SCALE)
                        offsetX.snapTo(destination.x)
                        offsetY.snapTo(destination.y)
                    } else {
                        val fitted = min(
                            viewportWidth / contentWidth,
                            viewportHeight / contentHeight
                        ).coerceIn(MIN_SCALE, 1f)
                        scale.snapTo(fitted)
                        offsetX.snapTo((viewportWidth - contentWidth * fitted) / 2f)
                        offsetY.snapTo((viewportHeight - contentHeight * fitted) / 2f)
                    }
                    hasSettled = true
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(layout, viewportWidth, viewportHeight) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                val current = scale.value
                                val target = (current * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                                val factor = target / current
                                // Punkt unter dem Finger bleibt stehen - vorher
                                // zoomte der Baum immer zur linken oberen Ecke.
                                val nextX = centroid.x - (centroid.x - offsetX.value) * factor + pan.x
                                val nextY = centroid.y - (centroid.y - offsetY.value) * factor + pan.y
                                scope.launch {
                                    scale.snapTo(target)
                                    offsetX.snapTo(clampX(nextX, target))
                                    offsetY.snapTo(clampY(nextY, target))
                                }
                                // Manuelles Verschieben hebt den Auto-Fokus auf.
                                if (centeredFor != null) centeredFor = null
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            // Block-Form: die Animatable-Werte werden erst in der
                            // Draw-Phase gelesen, das erspart eine Recomposition
                            // pro Frame beim Pannen/Zoomen.
                            .graphicsLayer {
                                scaleX = scale.value
                                scaleY = scale.value
                                translationX = offsetX.value
                                translationY = offsetY.value
                                transformOrigin = TransformOrigin(0f, 0f)
                            }
                            .size(layout.width.dp, layout.height.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val radius = LineCornerRadius.toPx()
                            layout.links.forEach { link ->
                                drawTreeLink(
                                    points = link.points.map {
                                        Offset(it.x.dp.toPx(), it.y.dp.toPx())
                                    },
                                    color = FamlyTreeLine,
                                    strokeWidth = if (link.isPartner) {
                                        PartnerLineWidth.toPx()
                                    } else {
                                        LineWidth.toPx()
                                    },
                                    cornerRadius = radius
                                )
                            }
                        }

                        layout.nodes.forEach { node ->
                            // key() haelt die Composable-Identitaet ueber
                            // Person.id fest, statt ueber die Iterationsreihenfolge -
                            // sonst wuerde animateDpAsState bei jeder
                            // Baum-Umsortierung faelschlich von vorne anfangen
                            // (oder die falsche Karte animieren), weil Compose
                            // sie sonst nur positionell wiederverwendet.
                            key(node.person.id) {
                                val animatedX by animateDpAsState(
                                    targetValue = node.x.dp,
                                    animationSpec = tween(REFLOW_ANIMATION_MS, easing = FastOutSlowInEasing),
                                    label = "treeCardX"
                                )
                                val animatedY by animateDpAsState(
                                    targetValue = node.y.dp,
                                    animationSpec = tween(REFLOW_ANIMATION_MS, easing = FastOutSlowInEasing),
                                    label = "treeCardY"
                                )
                                TreeCard(
                                    person = node.person,
                                    accent = FamlyGenColors[node.generation.mod(FamlyGenColors.size)],
                                    onClick = { onPersonClick(node.person) },
                                    highlighted = node.person.id == focusPersonId,
                                    isSelf = node.person.id == selfPersonId,
                                    modifier = Modifier.offset(x = animatedX, y = animatedY)
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
                    ZoomButton(
                        icon = Icons.Filled.Add,
                        onClick = {
                            applyZoom(
                                scale.value + 0.2f,
                                viewportWidth / 2f,
                                viewportHeight / 2f,
                                animate = true
                            )
                        }
                    )
                    ZoomButton(
                        icon = Icons.Filled.Remove,
                        onClick = {
                            applyZoom(
                                scale.value - 0.2f,
                                viewportWidth / 2f,
                                viewportHeight / 2f,
                                animate = true
                            )
                        }
                    )
                }

                // Immer erreichbarer Weg zurück zu "Ich", unabhängig davon,
                // wohin man im Baum gepannt/gezoomt hat.
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
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Zu mir",
                        tint = FamlyWhite
                    )
                }
            }
        }
    }
}

/**
 * Begrenzt das Verschieben so, dass der Baum nicht komplett aus dem Bild
 * gezogen werden kann. Passt der Inhalt komplett in den Viewport, wird er
 * zentriert.
 */
private fun clampOffset(value: Float, scaledContent: Float, viewport: Float): Float =
    if (scaledContent <= viewport) {
        (viewport - scaledContent) / 2f
    } else {
        value.coerceIn(viewport - scaledContent, 0f)
    }

/** Zeichnet eine rechtwinklige Polyline mit abgerundeten Ecken. */
private fun DrawScope.drawTreeLink(
    points: List<Offset>,
    color: Color,
    strokeWidth: Float,
    cornerRadius: Float
) {
    if (points.size < 2) return
    val path = Path()
    path.moveTo(points.first().x, points.first().y)
    for (i in 1 until points.size - 1) {
        val previous = points[i - 1]
        val current = points[i]
        val next = points[i + 1]
        val inLength = (current - previous).getDistance()
        val outLength = (next - current).getDistance()
        if (inLength < 0.01f || outLength < 0.01f) continue
        val radius = min(cornerRadius, min(inLength, outLength) / 2f)
        val start = current + (previous - current) * (radius / inLength)
        val end = current + (next - current) * (radius / outLength)
        path.lineTo(start.x, start.y)
        path.quadraticBezierTo(current.x, current.y, end.x, end.y)
    }
    path.lineTo(points.last().x, points.last().y)
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

@Composable
private fun TreeCard(
    person: Person,
    accent: Color,
    onClick: () -> Unit,
    highlighted: Boolean = false,
    isSelf: Boolean = false,
    modifier: Modifier = Modifier
) {
    val sub = if (person.isDeceased) person.birthDate.ifBlank { "verstorben" } else person.birthDate
    // "Ich" bekommt eine dauerhafte Umrandung, damit man sich im Baum immer
    // sofort wiederfindet - unabhängig vom temporären Fokus-Highlight.
    val borderColor = when {
        highlighted -> FamlyAccentOrange
        isSelf -> FamlyPetrolPrimary
        else -> null
    }
    Column(
        modifier = modifier
            .width(CARD_W.dp)
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
        FamlyAvatar(initial = person.initial, accent = accent, size = 48, cornerRadius = 24)
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