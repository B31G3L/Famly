package com.beigel.famly.ui.screens.tree

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beigel.famly.data.model.Person
import com.beigel.famly.ui.components.FamlyAvatar
import com.beigel.famly.ui.theme.FamlyAccentOrange
import com.beigel.famly.ui.theme.FamlyBackground
import com.beigel.famly.ui.theme.FamlyPetrolPrimary
import com.beigel.famly.ui.theme.FamlyTextPrimary
import com.beigel.famly.ui.theme.FamlyTextSecondary
import com.beigel.famly.ui.theme.FamlyTreeLine
import com.beigel.famly.ui.theme.FamlyWhite

private val CARD_WIDTH = 78.dp
private val CENTER_CARD_WIDTH = 98.dp

/**
 * Baum als beidseitig aufklappbarer "Ego-Baum": immer sichtbar sind die
 * fokussierte Person + Partner:in in der Mitte, beider Eltern darüber
 * (meine UND ihre Seite gleichzeitig) und die gemeinsamen Kinder darunter.
 * Alles WEITER weg (Großeltern, Urgroßeltern, Enkelkinder, ...) ist über
 * "+"-Buttons einzeln aufklappbar; der Auf-/Zugeklappt-Zustand ist pro
 * Person global gemerkt und bleibt bestehen, auch wenn man auf eine andere
 * Person umzentriert und wieder zurück navigiert.
 *
 * Tippen auf eine Karte öffnet ein kleines Auswahl-Sheet: Detailansicht
 * öffnen, oder den Baum auf diese Person umzentrieren.
 */
@Composable
fun TreeScreen(
    members: List<Person>,
    onPersonClick: (Person) -> Unit,
    onOpenSelf: () -> Unit,
    focusPersonId: String? = null,
    selfPersonId: String = "ich"
) {
    var centeredId by rememberSaveable { mutableStateOf(selfPersonId) }

    LaunchedEffect(focusPersonId) {
        if (focusPersonId != null) centeredId = focusPersonId
    }

    // Wer weitere Vorfahren/Nachkommen aufgeklappt hat - bleibt dauerhaft
    // erhalten (auch über Umzentrieren und App-Neustarts hinweg).
    var expandedAncestorsOf by rememberSaveable(
        stateSaver = listSaver<Set<String>, String>(save = { it.toList() }, restore = { it.toSet() })
    ) { mutableStateOf(emptySet<String>()) }
    var expandedDescendantsOf by rememberSaveable(
        stateSaver = listSaver<Set<String>, String>(save = { it.toList() }, restore = { it.toSet() })
    ) { mutableStateOf(emptySet<String>()) }

    var sheetPerson by remember { mutableStateOf<Person?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val byId = remember(members) { members.associateBy { it.id } }
    val childrenOf = remember(members) {
        val map = HashMap<String, MutableList<Person>>()
        members.forEach { child ->
            child.parentIds.forEach { parentId ->
                map.getOrPut(parentId) { mutableListOf() }.add(child)
            }
        }
        map
    }
    val centered = byId[centeredId] ?: byId[selfPersonId]
    val searchResults = remember(searchQuery, members) {
        val query = searchQuery.trim()
        if (query.length < 2) emptyList() else members.filter { it.name.contains(query, ignoreCase = true) }.take(6)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FamlyBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp, 22.dp, 22.dp, 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Stammbaum", style = MaterialTheme.typography.titleLarge)
                Text(
                    "${members.size} Personen",
                    style = MaterialTheme.typography.bodySmall,
                    color = FamlyTextSecondary
                )
            }
            // Immer erreichbarer Weg zurück zu "Ich", unabhängig davon, wie
            // weit man sich im Baum umzentriert hat.
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(FamlyWhite)
                    .clickable(onClick = onOpenSelf),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Zu mir",
                    tint = FamlyPetrolPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Column(modifier = Modifier.padding(22.dp, 0.dp, 22.dp, 10.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Person suchen...") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = FamlyTextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Suche leeren",
                            tint = FamlyTextSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = RoundedCornerShape(100.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = FamlyWhite,
                    focusedContainerColor = FamlyWhite,
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedBorderColor = FamlyPetrolPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (searchResults.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(FamlyWhite)
                ) {
                    searchResults.forEach { result ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    centeredId = result.id
                                    searchQuery = ""
                                }
                                .padding(14.dp, 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FamlyAvatar(initial = result.initial, accentType = result.accent, size = 28, cornerRadius = 10)
                            Text(result.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        if (centered == null) {
            Text(
                "Noch keine Person gefunden",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(24.dp),
                color = FamlyTextSecondary
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val partner = byId[centered.partnerId]

                // Meine Seite und die Seite der Partnerin/des Partners
                // NEBENEINANDER, jeweils mit eigenem (ggf. aufgeklapptem)
                // Vorfahren-Turm darüber. Bottom-Alignment sorgt dafür, dass
                // die beiden Personen-Karten selbst auf gleicher Höhe
                // landen, auch wenn eine Seite mehr Generationen aufgeklappt
                // hat als die andere.
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    key(centered.id) {
                        AncestorStack(
                            person = centered,
                            byId = byId,
                            expandedAncestorsOf = expandedAncestorsOf,
                            onToggleAncestors = { id -> expandedAncestorsOf = expandedAncestorsOf.toggled(id) },
                            onTapCard = { sheetPerson = it },
                            alwaysShowParents = true,
                            isCenterCard = true
                        )
                    }
                    if (partner != null) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = FamlyAccentOrange,
                            modifier = Modifier
                                .padding(bottom = 14.dp)
                                .size(13.dp)
                        )
                        key(partner.id) {
                            AncestorStack(
                                person = partner,
                                byId = byId,
                                expandedAncestorsOf = expandedAncestorsOf,
                                onToggleAncestors = { id -> expandedAncestorsOf = expandedAncestorsOf.toggled(id) },
                                onTapCard = { sheetPerson = it },
                                alwaysShowParents = true,
                                isCenterCard = false
                            )
                        }
                    }
                }

                val children = childrenOf[centered.id].orEmpty()
                if (children.isNotEmpty()) {
                    Connector()
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            children.forEach { child ->
                                key(child.id) {
                                    DescendantStack(
                                        person = child,
                                        childrenOf = childrenOf,
                                        expandedDescendantsOf = expandedDescendantsOf,
                                        onToggleDescendants = { id -> expandedDescendantsOf = expandedDescendantsOf.toggled(id) },
                                        onTapCard = { sheetPerson = it },
                                        alwaysShowChildren = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val tappedPerson = sheetPerson
    if (tappedPerson != null) {
        PersonActionSheet(
            person = tappedPerson,
            onDismiss = { sheetPerson = null },
            onShowDetail = {
                sheetPerson = null
                onPersonClick(tappedPerson)
            },
            onRecenter = {
                sheetPerson = null
                centeredId = tappedPerson.id
            }
        )
    }
}

private fun Set<String>.toggled(id: String): Set<String> = if (id in this) this - id else this + id

/**
 * Rendert [person] MIT ihrem Vorfahren-Turm darüber (rekursiv, jeweils
 * Mama+Papa nebeneinander). [alwaysShowParents] gilt nur für die
 * äußerste (Basis-)Ebene - für alle rekursiv aufgerufenen Vorfahren
 * entscheidet ausschließlich [expandedAncestorsOf], ob ihre eigenen Eltern
 * mit angezeigt werden oder nur ein "+" erscheint.
 */
@Composable
private fun AncestorStack(
    person: Person,
    byId: Map<String, Person>,
    expandedAncestorsOf: Set<String>,
    onToggleAncestors: (String) -> Unit,
    onTapCard: (Person) -> Unit,
    alwaysShowParents: Boolean,
    isCenterCard: Boolean
) {
    val explicitMother = byId[person.motherId]
    val explicitFather = byId[person.fatherId]
    val unclassifiedParents = person.parentIds
        .filter { it != person.motherId && it != person.fatherId }
        .mapNotNull { byId[it] }
    val mother = explicitMother ?: unclassifiedParents.getOrNull(0)
    val father = explicitFather ?: unclassifiedParents.firstOrNull { it.id != mother?.id }
    val hasParents = mother != null || father != null
    val showParents = alwaysShowParents || person.id in expandedAncestorsOf

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (hasParents && showParents) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                mother?.let {
                    key(it.id) {
                        AncestorStack(
                            person = it,
                            byId = byId,
                            expandedAncestorsOf = expandedAncestorsOf,
                            onToggleAncestors = onToggleAncestors,
                            onTapCard = onTapCard,
                            alwaysShowParents = false,
                            isCenterCard = false
                        )
                    }
                }
                father?.let {
                    key(it.id) {
                        AncestorStack(
                            person = it,
                            byId = byId,
                            expandedAncestorsOf = expandedAncestorsOf,
                            onToggleAncestors = onToggleAncestors,
                            onTapCard = onTapCard,
                            alwaysShowParents = false,
                            isCenterCard = false
                        )
                    }
                }
            }
            Connector()
        } else if (hasParents) {
            PlusButton(onClick = { onToggleAncestors(person.id) })
            Connector(height = 8.dp)
        }
        PersonCard(person = person, isCenter = isCenterCard, onClick = { onTapCard(person) })
    }
}

/**
 * Spiegelbildlich zu [AncestorStack], nur nach unten: [person] MIT ihrem
 * Nachkommen-Turm darunter.
 */
@Composable
private fun DescendantStack(
    person: Person,
    childrenOf: Map<String, List<Person>>,
    expandedDescendantsOf: Set<String>,
    onToggleDescendants: (String) -> Unit,
    onTapCard: (Person) -> Unit,
    alwaysShowChildren: Boolean
) {
    val children = childrenOf[person.id].orEmpty()
    val showChildren = alwaysShowChildren || person.id in expandedDescendantsOf

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PersonCard(person = person, isCenter = false, onClick = { onTapCard(person) })
        if (children.isNotEmpty()) {
            if (showChildren) {
                Connector()
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    children.forEach { child ->
                        key(child.id) {
                            DescendantStack(
                                person = child,
                                childrenOf = childrenOf,
                                expandedDescendantsOf = expandedDescendantsOf,
                                onToggleDescendants = onToggleDescendants,
                                onTapCard = onTapCard,
                                alwaysShowChildren = false
                            )
                        }
                    }
                }
            } else {
                Connector(height = 8.dp)
                PlusButton(onClick = { onToggleDescendants(person.id) })
            }
        }
    }
}

/** Kurze vertikale Verbindungslinie zwischen zwei Reihen. */
@Composable
private fun Connector(height: androidx.compose.ui.unit.Dp = 16.dp) {
    Box(
        modifier = Modifier
            .width(1.5.dp)
            .height(height)
            .background(FamlyTreeLine)
    )
}

/** Kleiner gestrichelter Kreis-Button: signalisiert "hier gibt's noch mehr, aufklappbar". */
@Composable
private fun PlusButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(50))
            .background(FamlyWhite)
            .border(1.dp, FamlyTextSecondary.copy(alpha = 0.35f), RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Weitere Personen anzeigen",
            tint = FamlyTextSecondary,
            modifier = Modifier.size(12.dp)
        )
    }
}

/** Personen-Karte: Avatar (Platzhalter fürs Foto), Name, Geburts-/Sterbedatum. */
@Composable
private fun PersonCard(person: Person, isCenter: Boolean, onClick: () -> Unit) {
    val dateLine = when {
        person.isDeceased && person.birthDate.isNotBlank() && person.deathDate.isNotBlank() ->
            "${person.birthDate} – ${person.deathDate}"
        person.isDeceased && person.deathDate.isNotBlank() -> "† ${person.deathDate}"
        person.birthDate.isNotBlank() -> person.birthDate
        else -> null
    }
    Column(
        modifier = Modifier
            .width(if (isCenter) CENTER_CARD_WIDTH else CARD_WIDTH)
            .clip(RoundedCornerShape(if (isCenter) 16.dp else 14.dp))
            .background(FamlyWhite)
            .then(
                if (isCenter) {
                    Modifier.border(2.dp, FamlyPetrolPrimary, RoundedCornerShape(16.dp))
                } else {
                    Modifier.border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = if (isCenter) 10.dp else 8.dp, horizontal = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FamlyAvatar(
            initial = person.initial,
            accentType = person.accent,
            size = if (isCenter) 40 else 32,
            cornerRadius = if (isCenter) 14 else 11
        )
        Text(
            person.name,
            style = if (isCenter) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = FamlyTextPrimary,
            maxLines = 1
        )
        if (dateLine != null) {
            Text(dateLine, fontSize = if (isCenter) 10.5.sp else 9.5.sp, color = FamlyTextSecondary, maxLines = 1)
        }
    }
}

/** Auswahl-Sheet beim Antippen einer Karte: Detail öffnen oder Baum umzentrieren. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonActionSheet(
    person: Person,
    onDismiss: () -> Unit,
    onShowDetail: () -> Unit,
    onRecenter: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FamlyAvatar(initial = person.initial, accentType = person.accent, size = 32, cornerRadius = 11)
                Text(person.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onShowDetail)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = FamlyTextSecondary)
                Text("Person anzeigen", style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRecenter)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Filled.FitScreen, contentDescription = null, tint = FamlyTextSecondary)
                Text("Baum hierher zentrieren", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}