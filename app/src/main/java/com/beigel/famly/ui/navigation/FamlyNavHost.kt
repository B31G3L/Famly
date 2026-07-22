package com.beigel.famly.ui.navigation

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.getValue
import com.beigel.famly.data.repository.FamilyRepository
import com.beigel.famly.ui.components.FamlyBottomBar
import com.beigel.famly.ui.components.FamlyBottomDestination
import com.beigel.famly.ui.screens.addperson.AddPersonScreen
import com.beigel.famly.ui.screens.dashboard.DashboardScreen
import com.beigel.famly.ui.screens.invite.InviteScreen
import com.beigel.famly.ui.screens.onboarding.OnboardingScreen
import com.beigel.famly.ui.screens.persondetail.PersonDetailScreen
import com.beigel.famly.ui.screens.profile.ProfileMenuEntry
import com.beigel.famly.ui.screens.profile.ProfileScreen
import com.beigel.famly.ui.screens.tree.TreeScreen

object FamlyRoutes {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val TREE = "tree"
    const val PERSON_DETAIL = "person_detail/{personId}"
    const val ADD_PERSON = "add_person"
    const val INVITE = "invite"
    const val PROFILE = "profile"

    fun personDetail(personId: String) = "person_detail/$personId"
}

private val bottomBarRoutes = FamlyBottomDestination.entries.map { it.route }.toSet()

@Composable
fun FamlyNavHost(
    repository: FamilyRepository,
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                FamlyBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(FamlyRoutes.DASHBOARD) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = FamlyRoutes.ONBOARDING,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(FamlyRoutes.ONBOARDING) {
                OnboardingScreen(
                    onGetStarted = {
                        navController.navigate(FamlyRoutes.DASHBOARD) {
                            popUpTo(FamlyRoutes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }

            composable(FamlyRoutes.DASHBOARD) {
                DashboardScreen(
                    userName = repository.getCurrentUserName(),
                    familyTree = repository.getFamilyTree(),
                    recentlyAdded = repository.getRecentlyAdded(),
                    onOpenTree = { navController.navigate(FamlyRoutes.TREE) },
                    onOpenPerson = { person -> navController.navigate(FamlyRoutes.personDetail(person.id)) },
                    onAddPerson = { navController.navigate(FamlyRoutes.ADD_PERSON) }
                )
            }

            composable(FamlyRoutes.TREE) {
                TreeScreen(
                    members = repository.getTreeMembers(),
                    onPersonClick = { person -> navController.navigate(FamlyRoutes.personDetail(person.id)) }
                )
            }

            composable(FamlyRoutes.PERSON_DETAIL) { entry ->
                val personId = entry.arguments?.getString("personId")
                val person = personId?.let { repository.getPersonById(it) }
                if (person != null) {
                    PersonDetailScreen(
                        person = person,
                        onBack = { navController.popBackStack() },
                        onEdit = { navController.navigate(FamlyRoutes.ADD_PERSON) },
                        onInvite = { navController.navigate(FamlyRoutes.INVITE) }
                    )
                }
            }

            composable(FamlyRoutes.ADD_PERSON) {
                AddPersonScreen(
                    onClose = { navController.popBackStack() },
                    onSave = { navController.popBackStack() }
                )
            }

            composable(FamlyRoutes.INVITE) {
                val tree = repository.getFamilyTree()
                InviteScreen(
                    familyName = tree.name,
                    memberCount = tree.memberCount,
                    inviteCode = repository.getInviteCode(),
                    members = repository.getFamilyMembers(),
                    onBack = { navController.popBackStack() },
                    onCopyCode = {}
                )
            }

            composable(FamlyRoutes.PROFILE) {
                val userName = repository.getCurrentUserName()
                ProfileScreen(
                    name = "$userName Müller",
                    email = "${userName.lowercase()}@example.com",
                    menuEntries = listOf(
                        ProfileMenuEntry("Familie verwalten") {},
                        ProfileMenuEntry("Benachrichtigungen") {},
                        ProfileMenuEntry("Hilfe & Feedback") {}
                    )
                )
            }
        }
    }
}
