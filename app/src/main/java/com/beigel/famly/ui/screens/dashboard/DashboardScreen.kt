package com.beigel.famly.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Park
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.beigel.famly.data.model.FamilyTree
import com.beigel.famly.data.model.Person
import com.beigel.famly.ui.components.FamlyAvatar
import com.beigel.famly.ui.components.FamlyCard
import com.beigel.famly.ui.components.FamlyPrimaryButton
import com.beigel.famly.ui.theme.FamlyBackground
import com.beigel.famly.ui.theme.FamlyPetrolPrimary
import com.beigel.famly.ui.theme.FamlySurfaceTint
import com.beigel.famly.ui.theme.FamlyTextSecondary
import com.beigel.famly.ui.theme.FamlyWhite

@Composable
fun DashboardScreen(
    userName: String,
    familyTree: FamilyTree,
    recentlyAdded: List<Person>,
    onOpenTree: () -> Unit,
    onOpenPerson: (Person) -> Unit,
    onAddPerson: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(FamlyBackground),
            contentPadding = PaddingValues(22.dp, 24.dp, 22.dp, 8.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Willkommen zurück", style = MaterialTheme.typography.bodyMedium, color = FamlyTextSecondary)
                        Text("Hallo, $userName", style = MaterialTheme.typography.headlineSmall)
                    }
                    FamlyAvatar(initial = userName.take(1), size = 42)
                }
            }

            item {
                FamlyCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(familyTree.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${familyTree.memberCount} Personen",
                            style = MaterialTheme.typography.bodySmall,
                            color = FamlyTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.padding(top = 7.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(FamlySurfaceTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Park,
                            contentDescription = null,
                            tint = FamlyPetrolPrimary
                        )
                    }
                    Spacer(modifier = Modifier.padding(top = 7.dp))
                    FamlyPrimaryButton(text = "Stammbaum öffnen", onClick = onOpenTree)
                }
            }

            item {
                Text(
                    "Kürzlich hinzugefügt",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(recentlyAdded) { person ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(FamlyWhite)
                        .clickable { onOpenPerson(person) }
                        .padding(16.dp, 13.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FamlyAvatar(initial = person.initial, accentType = person.accent, size = 36, cornerRadius = 12)
                    Column {
                        Text(person.name, style = MaterialTheme.typography.bodyLarge)
                        Text(person.relation, style = MaterialTheme.typography.bodySmall, color = FamlyTextSecondary)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
                .size(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(FamlyPetrolPrimary)
                .clickable(onClick = onAddPerson),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Person hinzufügen", tint = FamlyWhite)
        }
    }
}
