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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beigel.famly.data.model.Person
import com.beigel.famly.ui.components.FamlyAvatar
import com.beigel.famly.ui.components.FamlyCard
import com.beigel.famly.ui.components.FamlyIconTile
import com.beigel.famly.ui.components.FamlySecondaryButton
import com.beigel.famly.ui.theme.FamlyBackground
import com.beigel.famly.ui.theme.FamlyChipBackground
import com.beigel.famly.ui.theme.FamlyChipText
import com.beigel.famly.ui.theme.FamlyPetrolPrimary
import com.beigel.famly.ui.theme.FamlyStatusAlive
import com.beigel.famly.ui.theme.FamlyTextSecondary

@Composable
fun PersonDetailScreen(
    person: Person,
    mother: Person?,
    father: Person?,
    partner: Person?,
    children: List<Person>,
    canInvite: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onInvite: () -> Unit,
    onOpenPerson: (Person) -> Unit,
    onAddMother: () -> Unit,
    onAddFather: () -> Unit,
    onAddPartner: () -> Unit,
    onAddChild: () -> Unit
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
            FamlyIconTile(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", onClick = onBack)
            Text("Person", style = MaterialTheme.typography.labelLarge)
            FamlyIconTile(icon = Icons.Filled.Edit, contentDescription = "Bearbeiten", onClick = onEdit)
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
                if (person.isDeceased && person.deathDate.isNotBlank()) {
                    InfoRow(label = "Todesdatum", value = person.deathDate)
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Über sie", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.padding(top = 5.dp))
                Text(person.bio, style = MaterialTheme.typography.bodyMedium, color = FamlyTextSecondary)
            }

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Familie", style = MaterialTheme.typography.titleSmall)

                FamlyCard {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        RelativeRow(
                            label = "Mama",
                            linkedPerson = mother,
                            onOpen = { mother?.let(onOpenPerson) },
                            onAdd = onAddMother
                        )
                        RelativeRow(
                            label = "Papa",
                            linkedPerson = father,
                            onOpen = { father?.let(onOpenPerson) },
                            onAdd = onAddFather
                        )
                        RelativeRow(
                            label = "Partner:in",
                            linkedPerson = partner,
                            onOpen = { partner?.let(onOpenPerson) },
                            onAdd = onAddPartner
                        )
                    }
                }

                FamlyCard {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Kinder",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FamlyTextSecondary
                        )
                        if (children.isEmpty()) {
                            Text(
                                "Noch keine Kinder eingetragen",
                                style = MaterialTheme.typography.bodySmall,
                                color = FamlyTextSecondary,
                                modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                            )
                        } else {
                            children.forEach { child ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenPerson(child) }
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(child.name, style = MaterialTheme.typography.bodyMedium)
                                    Icon(
                                        imageVector = Icons.Filled.ChevronRight,
                                        contentDescription = null,
                                        tint = FamlyTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            "+ Kind hinzufügen",
                            color = FamlyPetrolPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onAddChild)
                                .padding(vertical = 6.dp)
                        )
                    }
                }
            }

            if (canInvite) {
                FamlySecondaryButton(text = "Einladen", modifier = Modifier.fillMaxWidth(), onClick = onInvite)
            }
        }
    }
}

@Composable
private fun RelativeRow(
    label: String,
    linkedPerson: Person?,
    onOpen: () -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = if (linkedPerson != null) onOpen else onAdd)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = FamlyTextSecondary)
        if (linkedPerson != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(linkedPerson.name, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.padding(end = 2.dp))
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = FamlyTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Text(
                "+ hinzufügen",
                color = FamlyPetrolPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
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
