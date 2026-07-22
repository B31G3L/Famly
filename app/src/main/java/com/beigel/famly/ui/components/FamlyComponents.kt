package com.beigel.famly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beigel.famly.data.model.AvatarAccent
import com.beigel.famly.ui.theme.FamlyPetrolPrimary
import com.beigel.famly.ui.theme.FamlyWhite

@Composable
fun FamlyAvatar(
    initial: String,
    accent: Color = FamlyPetrolPrimary,
    size: Int = 42,
    cornerRadius: Int = 16,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape((cornerRadius * size / 42).dp))
            .background(accent),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = FamlyWhite,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size * 0.36).sp
        )
    }
}

@Composable
fun FamlyAvatar(
    initial: String,
    accentType: AvatarAccent,
    size: Int = 42,
    cornerRadius: Int = 16,
    modifier: Modifier = Modifier
) {
    FamlyAvatar(initial = initial, accent = accentType.color, size = size, cornerRadius = cornerRadius, modifier = modifier)
}

@Composable
fun FamlyPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(100.dp))
            .background(FamlyPetrolPrimary)
            .clickable(onClick = onClick)
            .padding(vertical = 17.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = FamlyWhite, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
fun FamlySecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(100.dp))
            .border(1.dp, FamlyPetrolPrimary, RoundedCornerShape(100.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = FamlyPetrolPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
    }
}

@Composable
fun FamlyCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScopeAlias.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(FamlyWhite)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
            .padding(padding),
        content = content
    )
}

typealias ColumnScopeAlias = androidx.compose.foundation.layout.ColumnScope
