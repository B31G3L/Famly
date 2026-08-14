package com.beigel.famly.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beigel.famly.R
import com.beigel.famly.data.DemoPerson
import com.beigel.famly.data.FamilyIndex
import java.time.LocalDate

/**
 * Übersicht: Zahlen zum Bestand, anstehende Geburtstage und Gedenktage sowie
 * die offenen Lücken. Der Blickfang ist die direkte Linie der Ausgangsperson.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OverviewScreen(
    index: FamilyIndex,
    egoId: String,
    onOpenPerson: (String) -> Unit,
    onOpenTree: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val all = index.people
    val living = remember(all) { all.filter { !it.isDeceased } }
    val oldest = remember(living) { living.minByOrNull { it.birth } }

    val birthdays = remember(living, today) {
        living.map { it to it.daysUntilBirthday(today) }
            .filter { it.second <= 90 }
            .sortedBy { it.second }
            .take(5)
    }
    val memorials = remember(all, today) {
        all.filter { it.isDeceased }
            .map { it to it.daysUntilMemorial(today) }
            .sortedBy { it.second }
            .take(3)
    }
    val gaps = remember(all) {
        all.filter { it.parentIds.isEmpty() }.sortedBy { it.birth }.take(6)
    }
    val line = remember(index, egoId) { directLine(index, egoId) }
    val generations = remember(index) { index.generationCount() }

    LazyColumn(
        modifier = modifier.fillMaxWidth().background(DemoColors.Background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Eyebrow(stringResource(R.string.demo_tab_overview))
                Text(
                    text = stringResource(R.string.demo_family_title),
                    fontFamily = FontFamily.Serif,
                    fontSize = 28.sp,
                    color = DemoColors.Ink
                )
                Text(
                    text = stringResource(R.string.demo_overview_subtitle, all.size, generations),
                    color = DemoColors.Muted,
                    fontSize = 13.sp
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(
                    label = stringResource(R.string.demo_stat_people),
                    value = all.size.toString(),
                    caption = stringResource(R.string.demo_stat_living, living.size),
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = stringResource(R.string.demo_stat_generations),
                    value = generations.toString(),
                    caption = line.lastOrNull()?.firstName.orEmpty(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(
                    label = stringResource(R.string.demo_stat_oldest),
                    value = oldest?.let { stringResource(R.string.demo_years_short, it.ageYears(today)) }.orEmpty(),
                    caption = oldest?.fullName.orEmpty(),
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = stringResource(R.string.demo_stat_gaps),
                    value = gaps.size.toString(),
                    caption = stringResource(R.string.demo_stat_gaps_caption),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            SectionCard(
                title = stringResource(R.string.demo_direct_line),
                action = {
                    Text(
                        text = stringResource(R.string.demo_show_in_tree),
                        fontSize = 12.sp,
                        color = DemoColors.Muted,
                        modifier = Modifier.clickable(onClick = onOpenTree)
                    )
                }
            ) {
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    line.forEachIndexed { position, person ->
                        if (position > 0) {
                            Box(
                                Modifier
                                    .align(Alignment.CenterVertically)
                                    .width(18.dp)
                                    .height(1.dp)
                                    .background(DemoColors.Divider)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (position == 0) DemoColors.Ink else DemoColors.Surface,
                            border = BorderStroke(
                                1.dp,
                                if (position == 0) DemoColors.Ink else DemoColors.Divider
                            ),
                            modifier = Modifier.clickable { onOpenPerson(person.id) }
                        ) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = person.firstName,
                                    fontSize = 13.sp,
                                    color = if (position == 0) Color.White else DemoColors.Ink
                                )
                                Text(
                                    text = person.birthYear,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = if (position == 0) Color(0xFFCBD5E1) else DemoColors.Faint
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.demo_birthdays)) {
                Column {
                    birthdays.forEach { (person, days) ->
                        PersonRow(
                            person = person,
                            subtitle = stringResource(
                                R.string.demo_birthday_turns,
                                person.ageYears(today) + if (days > 0L) 1 else 0
                            ),
                            trailingTop = if (days == 0L) {
                                stringResource(R.string.demo_today)
                            } else {
                                stringResource(R.string.demo_in_days, days.toInt())
                            },
                            trailingBottom = null,
                            onClick = { onOpenPerson(person.id) }
                        )
                    }
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.demo_memorials)) {
                Column {
                    memorials.forEach { (person, days) ->
                        PersonRow(
                            person = person,
                            subtitle = formatIsoDate(person.death.orEmpty()),
                            trailingTop = stringResource(R.string.demo_in_days, days.toInt()),
                            trailingBottom = null,
                            onClick = { onOpenPerson(person.id) }
                        )
                    }
                }
            }
        }

        item {
            SectionCard(title = stringResource(R.string.demo_gaps_title)) {
                Column {
                    Text(
                        text = stringResource(R.string.demo_gaps_text),
                        color = DemoColors.Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        gaps.forEach { person ->
                            OutlinedChip(
                                text = "${person.fullName} ${person.birthYear}",
                                onClick = { onOpenPerson(person.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Ausgangsperson, dann jeweils der Vater - sonst der erste erfasste Elternteil. */
private fun directLine(index: FamilyIndex, egoId: String): List<DemoPerson> {
    val result = mutableListOf<DemoPerson>()
    var current = index[egoId]
    while (current != null && result.size < 8) {
        result += current
        val nextId = current.parentIds.firstOrNull { index[it]?.isFemale == false }
            ?: current.parentIds.firstOrNull()
        current = nextId?.let { index[it] }
    }
    return result
}
