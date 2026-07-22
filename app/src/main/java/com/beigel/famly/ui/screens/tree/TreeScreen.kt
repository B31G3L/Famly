package com.beigel.famly.ui.screens.tree

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.beigel.famly.ui.theme.FamlyBackground
import com.beigel.famly.ui.theme.FamlyPetrolPrimary
import com.beigel.famly.ui.theme.FamlyWhite

/**
 * Baum-Darstellung nach Generation/Slot, analog zum Handoff-Screen
 * "Stammbaum". Wächst der Baum (neue Personen), scrollt der Inhalt
 * automatisch vertikal und horizontal mit.
 */
@Composable
fun TreeScreen(
    members: List<Person>,
    onPersonClick: (Person) -> Unit,
    onAddPerson: () -> Unit
) {
    val generations = members.filter { it.treePosition != null }
        .groupBy { it.treePosition!!.generation }
        .toSortedMap()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FamlyBackground)
        ) {
            Text(
                "Stammbaum",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(22.dp, 22.dp, 22.dp, 14.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .horizontalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(56.dp)
                ) {
                    generations.forEach { (_, peopleInGen) ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(28.dp),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            peopleInGen.sortedBy { it.treePosition!!.slot }.forEach { person ->
                                TreeNode(person = person, onClick = { onPersonClick(person) })
                            }
                        }
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

@Composable
private fun TreeNode(person: Person, onClick: () -> Unit) {
    val size = if (person.relation == "Ich") 60 else 52
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FamlyAvatar(initial = person.initial, accentType = person.accent, size = size)
        Text(
            text = person.relation.takeIf { it != "Ich" && it.length <= 12 } ?: person.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1
        )
    }
}
