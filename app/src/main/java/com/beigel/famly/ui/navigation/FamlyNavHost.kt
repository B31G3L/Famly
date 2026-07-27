package com.beigel.famly.ui.navigation

import android.widget.Toast
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
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
    const val TREE = "tree?focusPersonId={focusPersonId}"
    const val PERSON_DETAIL = "person_detail/{personId}"
    const val ADD_PERSON = "add_person?personId={personId}&relativeOf={relativeOf}"
    const val INVITE = "invite"
    const val PROFILE = "profile"

    fun personDetail(personId: String) = "person_detail/$personId"
    fun addPerson(personId: String? = null, relativeOf: String? = null) =
        "add_person?personId=${personId ?: ""}&relativeOf=${relativeOf ?: ""}"
    fun addRelativeTo(personId: String) = addPerson(relativeOf = personId)
    fun tree(focusPersonId: String? = null) = "tree?focusPersonId=${focusPersonId ?: ""}"
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
    val context = LocalContext.current

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

    // Nach dem Anlegen/Bearbeiten einer Person direkt zum Baum, mit dieser
    // Person mittig fokussiert, statt einfach nur zurückzunavigieren.
    // WICHTIG: dasselbe saveState/restoreState-Muster wie die Bottom-Bar
    // weiter unten verwenden - sonst gerät der interne Back-Stack der
    // Navigation durcheinander und Tab-Wechsel über die Bottom-Bar reagieren
    // danach nicht mehr zuverlässig.
    fun goToTreeFocusedOn(personId: String) {
        navController.navigate(FamlyRoutes.tree(focusPersonId = personId)) {
            popUpTo(FamlyRoutes.DASHBOARD) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomBarRoutes ||
                bottomBarRoutes.any { currentRoute?.startsWith("$it?") == true }
            ) {
                FamlyBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        val resolvedRoute = if (route == FamlyBottomDestination.TREE.route) {
                            FamlyRoutes.tree()
                        } else {
                            route
                        }
                        navController.navigate(resolvedRoute) {
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
                    onOpenTree = { navController.navigate(FamlyRoutes.tree()) },
                    onOpenPerson = { person -> navController.navigate(FamlyRoutes.personDetail(person.id)) },
                    onOpenSelf = { navController.navigate(FamlyRoutes.personDetail(SELF_PERSON_ID)) }
                )
            }

            composable(
                route = FamlyRoutes.TREE,
                arguments = listOf(
                    navArgument("focusPersonId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                val focusPersonId = entry.arguments?.getString("focusPersonId")?.takeIf { it.isNotBlank() }
                TreeScreen(
                    members = members,
                    onPersonClick = { person -> navController.navigate(FamlyRoutes.personDetail(person.id)) },
                    onOpenSelf = { navController.navigate(FamlyRoutes.personDetail(SELF_PERSON_ID)) },
                    focusPersonId = focusPersonId
                )
            }

            composable(FamlyRoutes.PERSON_DETAIL) { entry ->
                val personId = entry.arguments?.getString("personId")
                val person = members.find { it.id == personId }
                if (person != null) {
                    PersonDetailScreen(
                        person = person,
                        onBack = { navController.popBackStack() },
                        onEdit = { navController.navigate(FamlyRoutes.addPerson(personId = person.id)) },
                        onInvite = { navController.navigate(FamlyRoutes.INVITE) },
                        onAddRelative = { navController.navigate(FamlyRoutes.addRelativeTo(person.id)) }
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
                    },
                    navArgument("relativeOf") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                val personId = entry.arguments?.getString("personId")?.takeIf { it.isNotBlank() }
                val relativeOfId = entry.arguments?.getString("relativeOf")?.takeIf { it.isNotBlank() }
                val existingPerson = members.find { it.id == personId }
                val relativeOfPerson = members.find { it.id == relativeOfId }
                // "Ich" darf nie löschbar sein - egal auf welchem Weg das Formular
                // erreicht wurde.
                val canDelete = existingPerson != null && existingPerson.id != SELF_PERSON_ID

                AddPersonScreen(
                    existingPerson = existingPerson,
                    relativeOf = relativeOfPerson,
                    onClose = { navController.popBackStack() },
                    onSave = { result ->
                        coroutineScope.launch {
                            if (existingPerson != null) {
                                familyRepository.updatePerson(
                                    id = existingPerson.id,
                                    name = result.name,
                                    birthDate = result.birthDate,
                                    birthPlace = result.birthPlace,
                                    isDeceased = result.isDeceased,
                                    deathDate = result.deathDate,
                                    bio = result.bio
                                ).onSuccess {
                                    goToTreeFocusedOn(existingPerson.id)
                                }.onFailure { error ->
                                    Toast.makeText(
                                        context,
                                        "Speichern fehlgeschlagen: ${error.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } else if (relativeOfPerson != null && result.relationType != null) {
                                familyRepository.addPerson(
                                    name = result.name,
                                    birthDate = result.birthDate,
                                    birthPlace = result.birthPlace,
                                    isDeceased = result.isDeceased,
                                    deathDate = result.deathDate,
                                    bio = result.bio,
                                    relativeOfId = relativeOfPerson.id,
                                    relationType = result.relationType
                                ).onSuccess { newPerson ->
                                    goToTreeFocusedOn(newPerson.id)
                                }.onFailure { error ->
                                    Toast.makeText(
                                        context,
                                        "Speichern fehlgeschlagen: ${error.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    },
                    onDelete = if (canDelete) {
                        {
                            coroutineScope.launch {
                                familyRepository.deletePerson(existingPerson!!.id)
                                    .onSuccess {
                                        navController.popBackStack(FamlyRoutes.DASHBOARD, inclusive = false)
                                    }
                                    .onFailure { error ->
                                        Toast.makeText(
                                            context,
                                            "Löschen fehlgeschlagen: ${error.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
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
