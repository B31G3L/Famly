package com.beigel.famly.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beigel.famly.R
import com.beigel.famly.data.DemoPerson
import com.beigel.famly.data.FamilyIndex

/**
 * Detailansicht als Bottom Sheet: Stammdaten, dann die Familie als anklickbare
 * Zeilen (Eltern / Partner:in / Geschwister / Kinder) und der Sprung in den Baum.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailSheet(
    index: FamilyIndex,
    personId: String,
    egoId: String,
    onOpenPerson: (String) -> Unit,
    onSetEgo: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val person = index[personId] ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DemoColors.Surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Avatar(person, 52.dp)
                Column {
                    Text(
                        text = person.fullName,
                        fontFamily = FontFamily.Serif,
                        fontSize = 22.sp,
                        color = DemoColors.Ink
                    )
                    person.birthName?.let {
                        Text(
                            text = stringResource(R.string.demo_birth_name, it),
                            color = DemoColors.Faint,
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = relationLabel(index.relationTo(egoId, person.id)) + " · " +
                            if (person.isDeceased) {
                                stringResource(R.string.demo_age_reached, person.ageYears())
                            } else {
                                stringResource(R.string.demo_age_years, person.ageYears())
                            },
                        color = DemoColors.Muted,
                        fontSize = 12.sp
                    )
                }
            }

            Column(
                Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DetailLine(
                    Icons.Filled.Cake,
                    if (person.birthPlace.isNotBlank()) {
                        stringResource(
                            R.string.demo_born_in,
                            formatIsoDate(person.birth),
                            person.birthPlace
                        )
                    } else {
                        formatIsoDate(person.birth)
                    }
                )
                person.death?.let {
                    DetailLine(Icons.Filled.Cake, stringResource(R.string.demo_died, formatIsoDate(it)))
                }
                if (person.job.isNotBlank()) DetailLine(Icons.Filled.Work, person.job)
                if (person.city.isNotBlank()) DetailLine(Icons.Filled.LocationOn, person.city)
            }

            if (person.note.isNotBlank()) {
                Text(
                    text = person.note,
                    color = DemoColors.Muted,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                )
            }

            RelativeGroup(
                title = stringResource(R.string.demo_group_parents),
                empty = stringResource(R.string.demo_group_parents_empty),
                people = index.parentsOf(person.id).mapNotNull { index[it] },
                onOpenPerson = onOpenPerson
            )
            RelativeGroup(
                title = stringResource(R.string.demo_group_partner),
                empty = stringResource(R.string.demo_group_partner_empty),
                people = person.partnerIds.mapNotNull { index[it] },
                onOpenPerson = onOpenPerson
            )
            RelativeGroup(
                title = stringResource(R.string.demo_group_siblings),
                empty = stringResource(R.string.demo_group_siblings_empty),
                people = index.siblingsOf(person.id).mapNotNull { index[it] },
                onOpenPerson = onOpenPerson
            )
            RelativeGroup(
                title = stringResource(R.string.demo_group_children),
                empty = stringResource(R.string.demo_group_children_empty),
                people = index.childrenOf(person.id).mapNotNull { index[it] },
                onOpenPerson = onOpenPerson
            )

            Button(
                onClick = { onSetEgo(person.id) },
                enabled = person.id != egoId,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DemoColors.Ink),
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
            ) {
                Icon(Icons.Filled.AccountTree, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    text = stringResource(R.string.demo_show_tree_from_here),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailLine(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = DemoColors.Faint, modifier = Modifier.size(16.dp))
        Text(text = text, color = DemoColors.Text, fontSize = 14.sp)
    }
}

@Composable
private fun RelativeGroup(
    title: String,
    empty: String,
    people: List<DemoPerson>,
    onOpenPerson: (String) -> Unit
) {
    Column(Modifier.padding(top = 18.dp)) {
        Eyebrow(title)
        if (people.isEmpty()) {
            Text(
                text = empty,
                color = DemoColors.Faint,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        } else {
            people.forEach { relative ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPerson(relative.id) }
                        .padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Avatar(relative, 30.dp)
                    Text(
                        text = relative.fullName,
                        fontSize = 14.sp,
                        color = DemoColors.Text,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = relative.lifeSpan(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = DemoColors.Faint
                    )
                }
            }
        }
    }
}
