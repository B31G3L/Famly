package com.beigel.famly.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beigel.famly.data.model.FamilyTree
import com.beigel.famly.data.model.Person
import com.beigel.famly.ui.components.FamlyAvatar
import com.beigel.famly.ui.theme.FamlyAccentOrange
import com.beigel.famly.ui.theme.FamlyAccentYellow
import com.beigel.famly.ui.theme.FamlyBackground
import com.beigel.famly.ui.theme.FamlyGenColors
import com.beigel.famly.ui.theme.FamlyHeaderGradientEnd
import com.beigel.famly.ui.theme.FamlyHeaderGradientStart
import com.beigel.famly.ui.theme.FamlyChipBackground
import com.beigel.famly.ui.theme.FamlyPillBlueBackground
import com.beigel.famly.ui.theme.FamlyPillBlueValue
import com.beigel.famly.ui.theme.FamlyPillOrangeBackground
import com.beigel.famly.ui.theme.FamlyPetrolPrimary
import com.beigel.famly.ui.theme.FamlyStatusAlive
import com.beigel.famly.ui.theme.FamlyTextPrimary
import com.beigel.famly.ui.theme.FamlyTextSecondary
import com.beigel.famly.ui.theme.FamlyWhite

@Composable
fun DashboardScreen(
    userName: String,
    familyTree: FamilyTree,
    recentlyAdded: List<Person>,
    onOpenTree: () -> Unit,
    onOpenPerson: (Person) -> Unit,
    onOpenSelf: () -> Unit
) {
    val generationCount = familyTree.members.mapNotNull { it.treePosition?.generation }.distinct().size

    Box(modifier = Modifier.fillMaxSize().background(FamlyBackground)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                HeaderSection(userName = userName, onOpenSelf = onOpenSelf)
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-34).dp)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    TreeSummaryCard(
                        memberCount = familyTree.memberCount,
                        generationCount = generationCount,
                        previewMembers = familyTree.members.take(4),
                        onOpenTree = onOpenTree
                    )

                    StatPillsRow(
                        peopleCount = familyTree.memberCount,
                        generationCount = generationCount,
                        newCount = recentlyAdded.size
                    )

                    if (recentlyAdded.isNotEmpty()) {
                        Column {
                            Text(
                                "Zuletzt hinzugefügt",
                                style = MaterialTheme.typography.titleSmall,
                                color = FamlyTextPrimary
                            )
                            Spacer(modifier = Modifier.padding(top = 10.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                recentlyAdded.forEachIndexed { index, person ->
                                    RecentPersonItem(
                                        person = person,
                                        color = FamlyGenColors[index.mod(FamlyGenColors.size)],
                                        onClick = { onOpenPerson(person) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(userName: String, onOpenSelf: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(FamlyHeaderGradientStart, FamlyHeaderGradientEnd),
                    start = Offset(0f, 0f)
                ),
                shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
            )
            .padding(20.dp, 24.dp, 20.dp, 68.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.clickable(onClick = onOpenSelf)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(50))
                        .background(FamlyWhite.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(userName.take(1).uppercase(), color = FamlyWhite, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
                Column {
                    Text("Hallo, $userName!", color = FamlyWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Schön, dich wiederzusehen", color = FamlyWhite.copy(alpha = 0.8f), fontSize = 13.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf(FamlyAccentOrange, FamlyAccentYellow, FamlyStatusAlive).forEach { dotColor ->
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(dotColor))
                }
            }
        }
    }
}

@Composable
private fun TreeSummaryCard(
    memberCount: Int,
    generationCount: Int,
    previewMembers: List<Person>,
    onOpenTree: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(FamlyWhite)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row {
            previewMembers.forEachIndexed { index, person ->
                Box(
                    modifier = Modifier
                        .offset(x = (-14 * index).dp)
                        .size(34.dp)
                        .clip(RoundedCornerShape(50))
                        .background(FamlyWhite),
                    contentAlignment = Alignment.Center
                ) {
                    FamlyAvatar(
                        initial = person.initial,
                        accent = FamlyGenColors[index.mod(FamlyGenColors.size)],
                        size = 30,
                        cornerRadius = 15
                    )
                }
            }
        }
        Text("Dein Familienbaum", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "$generationCount Generationen · $memberCount Personen",
            style = MaterialTheme.typography.bodySmall,
            color = FamlyTextSecondary
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(100.dp))
                .background(FamlyAccentOrange)
                .clickable(onClick = onOpenTree)
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Baum ansehen", color = FamlyWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun StatPillsRow(peopleCount: Int, generationCount: Int, newCount: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatPill(value = peopleCount, label = "Personen", background = FamlyChipBackground, valueColor = FamlyPetrolPrimary, modifier = Modifier.weight(1f))
        StatPill(value = generationCount, label = "Generationen", background = FamlyPillBlueBackground, valueColor = FamlyPillBlueValue, modifier = Modifier.weight(1f))
        StatPill(value = newCount, label = "Neu", background = FamlyPillOrangeBackground, valueColor = FamlyAccentOrange, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatPill(value: Int, label: String, background: Color, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value.toString(), color = valueColor, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(label, color = FamlyTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RecentPersonItem(person: Person, color: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(84.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FamlyAvatar(initial = person.initial, accent = color, size = 56, cornerRadius = 28)
        Text(person.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(person.relation, fontSize = 11.sp, color = FamlyTextSecondary, maxLines = 1)
    }
}
