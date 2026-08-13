package com.beigel.famly.ui.screens.members

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.beigel.famly.data.model.Person
import com.beigel.famly.ui.components.FamlyAvatar
import com.beigel.famly.ui.components.FamlyIconTile
import com.beigel.famly.ui.theme.FamlyBackground
import com.beigel.famly.ui.theme.FamlyDivider
import com.beigel.famly.ui.theme.FamlyTextSecondary
import com.beigel.famly.ui.theme.FamlyWhite

/**
 * Flache Liste ALLER Personen der Familie, alphabetisch sortiert - zum
 * schnellen Finden/Öffnen, unabhängig von der Baum-Struktur. Erreichbar
 * über das Profil-Menü ("Mitglieder verwalten").
 */
@Composable
fun MembersScreen(
    members: List<Person>,
    onBack: () -> Unit,
    onOpenPerson: (Person) -> Unit
) {
    val sorted = remember(members) { members.sortedBy { it.name.lowercase() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FamlyBackground)
    ) {
        Row(
            modifier = Modifier.padding(18.dp, 18.dp, 18.dp, 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FamlyIconTile(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", onClick = onBack)
            Text("Alle Personen (${sorted.size})", style = MaterialTheme.typography.labelLarge)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp, 4.dp, 18.dp, 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(FamlyWhite)
            ) {
                sorted.forEachIndexed { index, person ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenPerson(person) }
                            .padding(16.dp, 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FamlyAvatar(initial = person.initial, accentType = person.accent, size = 34, cornerRadius = 12)
                        Text(person.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = FamlyTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (index != sorted.lastIndex) {
                        androidx.compose.material3.HorizontalDivider(color = FamlyDivider)
                    }
                }
            }
        }
    }
}
