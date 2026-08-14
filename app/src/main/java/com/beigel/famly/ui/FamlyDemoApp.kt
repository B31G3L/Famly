package com.beigel.famly.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.beigel.famly.R
import com.beigel.famly.data.DemoFamily
import com.beigel.famly.data.FamilyIndex

private enum class DemoTab { OVERVIEW, TREE, PEOPLE }

/**
 * Einstiegspunkt des Demo-Bereichs: drei Bereiche, eine gemeinsame
 * Ausgangsperson und ein Detail-Sheet, das aus allen Bereichen aufgeht.
 *
 * Läuft ohne Firebase und ohne Repository - die Daten kommen aus [DemoFamily].
 */
@Composable
fun FamlyDemoApp(modifier: Modifier = Modifier) {
    val index = remember { FamilyIndex(DemoFamily.people) }

    var tab by rememberSaveable { mutableStateOf(DemoTab.TREE) }
    var egoId by rememberSaveable { mutableStateOf(DemoFamily.DEFAULT_EGO_ID) }
    var selectedPersonId by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DemoColors.Background,
        bottomBar = {
            NavigationBar(containerColor = DemoColors.Surface, tonalElevation = 0.dp) {
                DemoTab.entries.forEach { entry ->
                    val (label, icon) = when (entry) {
                        DemoTab.OVERVIEW -> R.string.demo_tab_overview to Icons.Filled.Dashboard
                        DemoTab.TREE -> R.string.demo_tab_tree to Icons.Filled.AccountTree
                        DemoTab.PEOPLE -> R.string.demo_tab_people to Icons.Filled.People
                    }
                    NavigationBarItem(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(stringResource(label)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = DemoColors.Ink,
                            indicatorColor = DemoColors.Ink,
                            unselectedIconColor = DemoColors.Faint,
                            unselectedTextColor = DemoColors.Faint
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (tab) {
                DemoTab.OVERVIEW -> OverviewScreen(
                    index = index,
                    egoId = egoId,
                    onOpenPerson = { selectedPersonId = it },
                    onOpenTree = { tab = DemoTab.TREE }
                )
                DemoTab.TREE -> TreeScreen(
                    index = index,
                    egoId = egoId,
                    onEgoChange = { egoId = it },
                    onOpenPerson = { selectedPersonId = it }
                )
                DemoTab.PEOPLE -> PeopleScreen(
                    index = index,
                    egoId = egoId,
                    onOpenPerson = { selectedPersonId = it }
                )
            }
        }

        selectedPersonId?.let { id ->
            PersonDetailSheet(
                index = index,
                personId = id,
                egoId = egoId,
                onOpenPerson = { selectedPersonId = it },
                onSetEgo = {
                    egoId = it
                    selectedPersonId = null
                    tab = DemoTab.TREE
                },
                onDismiss = { selectedPersonId = null }
            )
        }
    }
}
