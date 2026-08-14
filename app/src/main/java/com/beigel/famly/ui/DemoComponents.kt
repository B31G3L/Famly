package com.beigel.famly.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beigel.famly.data.DemoPerson

@Composable
fun Avatar(person: DemoPerson, size: Dp = 36.dp, modifier: Modifier = Modifier) {
    val (background, text) = avatarColors(person)
    Box(
        modifier = modifier.size(size).background(background, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = person.initials,
            color = text,
            fontSize = (size.value * 0.33f).sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Überschrift im Karteikarten-Stil: kleine Versalien über dem eigentlichen Wert. */
@Composable
fun Eyebrow(text: String, color: Color = DemoColors.Faint, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = color,
        fontSize = 11.sp,
        letterSpacing = 1.4.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
    )
}

@Composable
fun StatTile(label: String, value: String, caption: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = DemoColors.Surface,
        border = BorderStroke(1.dp, DemoColors.Divider)
    ) {
        Column(Modifier.padding(14.dp)) {
            Eyebrow(label)
            Text(
                text = value,
                fontFamily = FontFamily.Serif,
                fontSize = 26.sp,
                color = DemoColors.Ink,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = caption,
                color = DemoColors.Muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = DemoColors.Surface,
        border = BorderStroke(1.dp, DemoColors.Divider)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontFamily = FontFamily.Serif,
                    fontSize = 17.sp,
                    color = DemoColors.Ink
                )
                action?.invoke()
            }
            Box(Modifier.padding(top = 12.dp)) { content() }
        }
    }
}

/** Auswahl-Pille für Filter und Schalter. */
@Composable
fun TogglePill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) DemoColors.Ink else DemoColors.Surface,
        border = BorderStroke(
            1.dp,
            if (selected) DemoColors.Ink else DemoColors.Divider
        )
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else DemoColors.Muted,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun PersonRow(
    person: DemoPerson,
    subtitle: String,
    trailingTop: String,
    trailingBottom: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Avatar(person, 40.dp)
        Column(Modifier.weight(1f)) {
            Text(
                text = person.fullName,
                fontFamily = FontFamily.Serif,
                fontSize = 16.sp,
                color = DemoColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = DemoColors.Muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = trailingTop,
                color = DemoColors.Muted,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            if (trailingBottom != null) {
                Text(text = trailingBottom, color = DemoColors.Faint, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(text = text, color = DemoColors.Text, fontSize = 11.sp)
    }
}

@Composable
fun OutlinedChip(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .border(1.dp, DemoColors.Divider, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(text = text, fontSize = 13.sp, color = DemoColors.Text)
    }
}
