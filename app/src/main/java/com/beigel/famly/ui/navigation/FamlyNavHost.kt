package com.beigel.famly.ui.navigation

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.beigel.famly.data.auth.AuthRepository
import com.beigel.famly.data.model.FamilyMember
import com.beigel.famly.data.model.MemberStatus
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
import kotlinx.coroutines.launch

object FamlyRoutes {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val TREE = "tree"
    const val PERSON_DETAIL = "person_detail/{personId}"
    const val ADD_PERSON = "add_person?personId={personId}"
    const val INVITE = "invite"
    const val PROFILE = "profile"

    fun personDetail(personId: String) = "person_detail/$personId"
    fun addPerson(personId: String? = null) =
        if (personId != null) "add_person?personId=$personId" else "add_person"
}

private val bottomBarRoutes = FamlyBottomDestination.entries.map { it.route }.toSet()
private const val SELF_PERSON_ID = "ich"

@Composable
fun FamlyNavHost(
    familyRepository: FamilyRepository,
    authRepository: AuthRepository,
    onSignInWithGoogle: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val familyTree by familyRepository.familyTree.collectAsState()
    val currentUserName by familyRepository.currentUserName.collectAsState()
    val inviteCode by familyRepository.inviteCode.collectAsState()
    val authUser by authRepository.currentUser.collectAsState()

    val members = familyTree.members
    val recentlyAdded = members.filter { it.id != SELF_PERSON_ID }.takeLast(2).reversed()
    val familyMembers = members.map { person ->
        FamilyMember(
            person = person,
            role = if (person.id == SELF_PERSON_ID) "Besitzer" else "Mitglied",
            status = if (person.id == SELF_PERSON_ID) MemberStatus.OWNER else MemberStatus.MEMBER
        )
    }

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
                    userName = currentUserName,
                    familyTree = familyTree,
                    recentlyAdded = recentlyAdded,
                    onOpenTree = { navController.navigate(FamlyRoutes.TREE) },
                    onOpenPerson = { person -> navController.navigate(FamlyRoutes.personDetail(person.id)) },
                    onAddPerson = { navController.navigate(FamlyRoutes.addPerson()) }
                )
            }

            composable(FamlyRoutes.TREE) {
                TreeScreen(
                    members = members,
                    onPersonClick = { person -> navController.navigate(FamlyRoutes.personDetail(person.id)) },
                    onAddPerson = { navController.navigate(FamlyRoutes.addPerson()) }
                )
            }

            composable(FamlyRoutes.PERSON_DETAIL) { entry ->
                val personId = entry.arguments?.getString("personId")
                val person = members.find { it.id == personId }
                if (person != null) {
                    PersonDetailScreen(
                        person = person,
                        onBack = { navController.popBackStack() },
                        onEdit = { navController.navigate(FamlyRoutes.addPerson(person.id)) },
                        onInvite = { navController.navigate(FamlyRoutes.INVITE) }
                    )
                }
            }

            composable(
                route = FamlyRoutes.ADD_PERSON,
                arguments = listOf(
                    navArgument("personId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                val personId = entry.arguments?.getString("personId")
                val existingPerson = members.find { it.id == personId }

                AddPersonScreen(
                    existingPerson = existingPerson,
                    availableConnections = members,
                    onClose = { navController.popBackStack() },
                    onSave = { result ->
                        coroutineScope.launch {
                            if (existingPerson != null) {
                                familyRepository.updatePerson(
                                    id = existingPerson.id,
                                    name = result.name,
                                    relation = result.relation,
                                    birthDate = result.birthDate,
                                    birthPlace = result.birthPlace,
                                    isDeceased = result.isDeceased,
                                    bio = result.bio,
                                    connections = result.connections
                                )
                            } else {
                                familyRepository.addPerson(
                                    name = result.name,
                                    relation = result.relation,
                                    birthDate = result.birthDate,
                                    birthPlace = result.birthPlace,
                                    isDeceased = result.isDeceased,
                                    bio = result.bio,
                                    connections = result.connections
                                )
                            }
                            navController.popBackStack()
                        }
                    },
                    onDelete = if (existingPerson != null) {
                        {
                            coroutineScope.launch {
                                familyRepository.deletePerson(existingPerson.id)
                                navController.popBackStack(FamlyRoutes.DASHBOARD, inclusive = false)
                            }
                        }
                    } else null
                )
            }

            composable(FamlyRoutes.INVITE) {
                InviteScreen(
                    familyName = familyTree.name,
                    memberCount = familyTree.memberCount,
                    inviteCode = inviteCode,
                    members = familyMembers,
                    onBack = { navController.popBackStack() },
                    onCopyCode = { clipboardManager.setText(AnnotatedString(inviteCode)) }
                )
            }

            composable(FamlyRoutes.PROFILE) {
                val isAnonymous = authUser?.isAnonymous ?: true
                ProfileScreen(
                    name = "$currentUserName Müller",
                    email = authUser?.email ?: "${currentUserName.lowercase()}@example.com",
                    menuEntries = listOf(
                        ProfileMenuEntry("Familie verwalten") {},
                        ProfileMenuEntry(
                            if (isAnonymous) "Mit Google sichern" else "Mit Google verknüpft"
                        ) {
                            if (isAnonymous) onSignInWithGoogle()
                        },
                        ProfileMenuEntry("Benachrichtigungen") {},
                        ProfileMenuEntry("Hilfe & Feedback") {}
                    )
                )
            }
        }
    }
}
