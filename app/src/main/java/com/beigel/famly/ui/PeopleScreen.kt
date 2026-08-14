package com.beigel.famly.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beigel.famly.R
import com.beigel.famly.data.FamilyIndex

private enum class PeopleFilter { ALL, LIVING, DECEASED }

/**
 * Personenliste über den gesamten Bestand - inklusive Verwandtschaftsangabe
 * relativ zur aktuellen Ausgangsperson.
 */
@Composable
fun PeopleScreen(
    index: FamilyIndex,
    egoId: String,
    onOpenPerson: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(PeopleFilter.ALL) }
    var sortByYear by rememberSaveable { mutableStateOf(false) }

    val results = remember(index, query, filter, sortByYear) {
        index.people
            .filter { person ->
                when (filter) {
                    PeopleFilter.LIVING -> !person.isDeceased
                    PeopleFilter.DECEASED -> person.isDeceased
                    PeopleFilter.ALL -> true
                }
            }
            .filter { person ->
                if (query.isBlank()) return@filter true
                val haystack = listOf(
                    person.fullName, person.birthName.orEmpty(),
                    person.birthPlace, person.city, person.job
                ).joinToString(" ").lowercase()
                haystack.contains(query.trim().lowercase())
            }
            .sortedWith(
                if (sortByYear) compareBy { it.birth }
                else compareBy({ it.lastName }, { it.firstName })
            )
    }

    Column(modifier.fillMaxSize().background(DemoColors.Background)) {
        Surface(color = DemoColors.Surface) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text(stringResource(R.string.demo_search_hint)) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TogglePill(
                        text = stringResource(R.string.demo_filter_all),
                        selected = filter == PeopleFilter.ALL,
                        onClick = { filter = PeopleFilter.ALL }
                    )
                    TogglePill(
                        text = stringResource(R.string.demo_filter_living),
                        selected = filter == PeopleFilter.LIVING,
                        onClick = { filter = PeopleFilter.LIVING }
                    )
                    TogglePill(
                        text = stringResource(R.string.demo_filter_deceased),
                        selected = filter == PeopleFilter.DECEASED,
                        onClick = { filter = PeopleFilter.DECEASED }
                    )
                    Box(Modifier.width(1.dp).height(20.dp).background(DemoColors.Divider))
                    TogglePill(
                        text = stringResource(R.string.demo_sort_az),
                        selected = !sortByYear,
                        onClick = { sortByYear = false }
                    )
                    TogglePill(
                        text = stringResource(R.string.demo_sort_year),
                        selected = sortByYear,
                        onClick = { sortByYear = true }
                    )
                    Text(
                        text = stringResource(R.string.demo_hits, results.size),
                        color = DemoColors.Faint,
                        fontSize = 12.sp
                    )
                }
            }
        }
        HorizontalDivider(color = DemoColors.Divider)

        LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
            items(results, key = { it.id }) { person ->
                val place = person.city.ifBlank {
                    person.birthPlace.ifBlank { stringResource(R.string.demo_unknown_place) }
                }
                val job = person.job.ifBlank { "—" }
                PersonRow(
                    person = person,
                    subtitle = "$job · $place",
                    trailingTop = person.lifeSpan(),
                    trailingBottom = relationLabel(index.relationTo(egoId, person.id)),
                    onClick = { onOpenPerson(person.id) }
                )
                HorizontalDivider(color = DemoColors.Divider.copy(alpha = 0.6f))
            }
        }
    }
}
