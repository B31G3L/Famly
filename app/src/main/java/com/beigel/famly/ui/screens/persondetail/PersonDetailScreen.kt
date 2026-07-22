package com.beigel.famly.ui.screens.persondetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.beigel.famly.data.model.Person
import com.beigel.famly.ui.components.FamlyAvatar
import com.beigel.famly.ui.components.FamlyCard
import com.beigel.famly.ui.components.FamlyPrimaryButton
import com.beigel.famly.ui.components.FamlySecondaryButton
import com.beigel.famly.ui.theme.FamlyBackground
import com.beigel.famly.ui.theme.FamlyChipBackground
import com.beigel.famly.ui.theme.FamlyChipText
import com.beigel.famly.ui.theme.FamlyIconBackground
import com.beigel.famly.ui.theme.FamlyStatusAlive
import com.beigel.famly.ui.theme.FamlyTextSecondary

@Composable
fun PersonDetailScreen(
    person: Person,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onInvite: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FamlyBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp, 18.dp, 18.dp, 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(icon = Icons.Filled.ArrowBack, contentDescription = "Zurück", onClick = onBack)
            Text("Person", style = MaterialTheme.typography.labelLarge)
            IconTile(icon = Icons.Filled.Edit, contentDescription = "Bearbeiten", onClick = onEdit)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(22.dp, 24.dp, 22.dp, 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            FamlyAvatar(initial = person.initial, accentType = person.accent, size = 100, cornerRadius = 32)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(person.name, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.padding(top = 5.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(FamlyChipBackground)
                        .padding(14.dp, 5.dp)
                ) {
                    Text(person.relation, color = FamlyChipText, style = MaterialTheme.typography.labelSmall)
                }
            }

            FamlyCard {
                InfoRow(label = "Geburtsdatum", value = person.birthDate)
                InfoRow(label = "Geburtsort", value = person.birthPlace)
                InfoRow(
                    label = "Status",
                    value = if (person.isDeceased) "Verstorben" else "Lebt",
                    valueColor = if (person.isDeceased) FamlyTextSecondary else FamlyStatusAlive
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Über sie", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.padding(top = 5.dp))
                Text(person.bio, style = MaterialTheme.typography.bodyMedium, color = FamlyTextSecondary)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FamlyPrimaryButton(text = "Bearbeiten", modifier = Modifier.weight(1f), onClick = onEdit)
                FamlySecondaryButton(text = "Einladen", modifier = Modifier.weight(1f), onClick = onInvite)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = FamlyTextSecondary)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun IconTile(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(FamlyIconBackground)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, modifier = Modifier.size(16.dp))
    }
}
