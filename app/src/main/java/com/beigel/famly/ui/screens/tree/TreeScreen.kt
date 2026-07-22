package com.beigel.famly.ui.screens.tree

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.beigel.famly.data.model.Person
import com.beigel.famly.ui.components.FamlyAvatar
import com.beigel.famly.ui.theme.FamlyBackground

/**
 * Vereinfachte, statische Baum-Darstellung nach Generation/Slot,
 * analog zum Handoff-Screen "Stammbaum". Verbindungslinien werden
 * anhand relativer Positionen pro Generation gezeichnet.
 */
@Composable
fun TreeScreen(
    members: List<Person>,
    onPersonClick: (Person) -> Unit
) {
    val generations = members.filter { it.treePosition != null }
        .groupBy { it.treePosition!!.generation }
        .toSortedMap()

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
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(56.dp)
            ) {
                generations.forEach { (_, peopleInGen) ->
                    androidx.compose.foundation.layout.Row(
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(28.dp),
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
}

@Composable
private fun TreeNode(person: Person, onClick: () -> Unit) {
    val size = if (person.relation == "Ich") 60 else 52
    Column(
        modifier = Modifier
            .width(68.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
    ) {
        FamlyAvatar(initial = person.initial, accentType = person.accent, size = size)
        Text(
            text = person.relation.takeIf { it != "Ich" && it.length <= 12 } ?: person.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1
        )
    }
}
