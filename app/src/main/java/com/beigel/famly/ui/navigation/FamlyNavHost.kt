package com.beigel.famly.ui.navigation

import android.util.Log
import android.widget.Toast
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.beigel.famly.data.model.RelationType
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
    const val ADD_PERSON = "add_person?personId={personId}&relativeOf={relativeOf}&relationType={relationType}"
    const val INVITE = "invite"
    const val PROFILE = "profile"

    fun personDetail(personId: String) = "person_detail/$personId"
    fun addPerson(personId: String? = null, relativeOf: String? = null, relationType: RelationType? = null) =
        "add_person?personId=${personId ?: ""}&relativeOf=${relativeOf ?: ""}&relationType=${relationType?.name ?: ""}"
    // "Kind hinzufügen": kein fester relationType, im Formular wird noch
    // zwischen Tochter/Sohn gewählt.
    fun addChildTo(personId: String) = addPerson(relativeOf = personId)
    // Mama/Papa/Partner:in hinzufügen: relationType steht schon fest, das
    // Formular überspringt die Auswahl komplett.
    fun addRelativeTo(personId: String, relationType: RelationType) =
        addPerson(relativeOf = personId, relationType = relationType)
    fun tree(focusPersonId: String? = null) = "tree?focusPersonId=${focusPersonId ?: ""}"
}

private val bottomBarRoutes = FamlyBottomDestination.entries.map { it.route }.toSet()
private const val SELF_PERSON_ID = "ich"
private const val TAG = "FamlyNavHost"

@Composable
fun FamlyNavHost(
    familyRepository: FamilyRepository,
    authRepository: AuthRepository,
    onSignInWithGoogle: () -> Unit,
    startDestination: String = FamlyRoutes.ONBOARDING,
    onOnboardingCompleted: () -> Unit = {},
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    LaunchedEffect(currentRoute) {
        Log.d(TAG, "UI zeigt jetzt Route: $currentRoute")
    }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val familyTree by familyRepository.familyTree.collectAsState()
    val currentUserName by familyRepository.currentUserName.collectAsState()
    val inviteCode by familyRepository.inviteCode.collectAsState()
    val authUser by authRepository.currentUser.collectAsState()

    val members = familyTree.members
    val recentlyAdded = members.filter { it.id != SELF_PERSON_ID }.takeLast(2).reversed()
    // Für "Liste teilen" zählen nur Personen, die tatsächlich über den
    // Einladungscode beigetreten sind (echter Account, erkennbar an uid).
    // Rein manuell angelegte Baum-Einträge (z. B. verstorbene Verwandte ohne
    // eigenen Account) sollen hier NICHT als Mitglieder auftauchen.
    val familyMembers = members.filter { it.uid != null }.map { person ->
        FamilyMember(
            person = person,
            role = if (person.id == SELF_PERSON_ID) "Besitzer" else "Mitglied",
            status = if (person.id == SELF_PERSON_ID) MemberStatus.OWNER else MemberStatus.MEMBER
        )
    }

    // Nach dem Anlegen/Bearbeiten einer Person direkt zum Baum, mit dieser
    // Person mittig fokussiert, statt einfach nur zurückzunavigieren.
    //
    // Der Fokus wird bewusst NICHT über das Navigations-Argument transportiert:
    // die Bottom-Bar navigiert mit restoreState = true, und ein restaurierter
    // Back-Stack-Eintrag bringt seine ALTEN Argumente mit - die frisch
    // übergebene focusPersonId wurde dadurch stillschweigend verworfen. Ein
    // eigener State ist hier robuster und übersteht auch den Prozess-Tod.
    var pendingFocusPersonId by rememberSaveable { mutableStateOf<String?>(null) }

    // Verhindert doppeltes Speichern bei schnellem Doppel-Tap auf "Speichern".
    var isSaving by remember { mutableStateOf(false) }

    // War vorher als popUpTo(DASHBOARD){saveState=true} + restoreState=true
    // gebaut (analog zur Bottom-Bar), hat sich aber als kompletter No-Op
    // erwiesen: navigate() lief durch, ohne den Back-Stack überhaupt zu
    // verändern (siehe Debug-Logs). Stattdessen jetzt zwei unabhängige,
    // simple Schritte: erst zurückpoppen bis (aber ohne) Dashboard, dann
    // ganz normal zu Tree navigieren.
    fun goToTreeFocusedOn(personId: String) {
        Log.d(TAG, "goToTreeFocusedOn($personId) wird ausgeführt")
        Log.d(TAG, "Back-Stack VOR popBackStack: ${navController.currentBackStack.value.map { it.destination.route }}")
        pendingFocusPersonId = personId
        val popped = navController.popBackStack(FamlyRoutes.DASHBOARD, false)
        Log.d(TAG, "popBackStack(DASHBOARD) Ergebnis=$popped, Back-Stack danach: ${navController.currentBackStack.value.map { it.destination.route }}")
        navController.navigate(FamlyRoutes.tree()) {
            launchSingleTop = true
        }
        Log.d(TAG, "navController.navigate(tree) abgeschickt")
        Log.d(TAG, "Back-Stack NACH navigate: ${navController.currentBackStack.value.map { it.destination.route }}")
        Log.d(TAG, "currentDestination NACH navigate: ${navController.currentDestination?.route}")
    }

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomBarRoutes ||
                bottomBarRoutes.any { currentRoute?.startsWith("$it?") == true }
            ) {
                FamlyBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        // Bewusster Tab-Wechsel: kein Auto-Fokus mehr, der Baum
                        // soll da stehen bleiben, wo der Nutzer ihn verlassen hat.
                        pendingFocusPersonId = null
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
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(FamlyRoutes.ONBOARDING) {
                OnboardingScreen(
                    onGetStarted = {
                        // Persistiert (siehe MainActivity), damit das
                        // Onboarding nur beim allerersten Start gezeigt wird,
                        // nicht bei jedem App-Start erneut.
                        onOnboardingCompleted()
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
                val routeFocusPersonId = entry.arguments?.getString("focusPersonId")?.takeIf { it.isNotBlank() }
                TreeScreen(
                    members = members,
                    onPersonClick = { person -> navController.navigate(FamlyRoutes.personDetail(person.id)) },
                    onOpenSelf = { navController.navigate(FamlyRoutes.personDetail(SELF_PERSON_ID)) },
                    focusPersonId = pendingFocusPersonId ?: routeFocusPersonId,
                    selfPersonId = SELF_PERSON_ID
                )
            }

            composable(FamlyRoutes.PERSON_DETAIL) { entry ->
                val personId = entry.arguments?.getString("personId")
                val person = members.find { it.id == personId }
                if (person != null) {
                    // Fallback für Altbestand/unklare Fälle: falls motherId/fatherId
                    // nicht gesetzt sind, aber parentIds trotzdem 1-2 Einträge hat
                    // (z. B. Migration aus dem alten connections-Feld), die freien
                    // Slots damit auffüllen statt fälschlich "+ hinzufügen" zu zeigen.
                    val explicitMother = members.find { it.id == person.motherId }
                    val explicitFather = members.find { it.id == person.fatherId }
                    val unclassifiedParents = person.parentIds
                        .filter { it != person.motherId && it != person.fatherId }
                        .mapNotNull { id -> members.find { it.id == id } }
                    val mother = explicitMother ?: unclassifiedParents.getOrNull(0)
                    val father = explicitFather
                        ?: unclassifiedParents.firstOrNull { it.id != mother?.id }
                    val partner = members.find { it.id == person.partnerId }
                    val children = members.filter { person.id in it.parentIds }

                    PersonDetailScreen(
                        person = person,
                        mother = mother,
                        father = father,
                        partner = partner,
                        children = children,
                        canInvite = person.id != SELF_PERSON_ID,
                        onBack = { navController.popBackStack() },
                        onEdit = { navController.navigate(FamlyRoutes.addPerson(personId = person.id)) },
                        onInvite = { navController.navigate(FamlyRoutes.INVITE) },
                        onOpenPerson = { target -> navController.navigate(FamlyRoutes.personDetail(target.id)) },
                        onAddMother = { navController.navigate(FamlyRoutes.addRelativeTo(person.id, RelationType.MOTHER)) },
                        onAddFather = { navController.navigate(FamlyRoutes.addRelativeTo(person.id, RelationType.FATHER)) },
                        onAddPartner = { navController.navigate(FamlyRoutes.addRelativeTo(person.id, RelationType.PARTNER)) },
                        onAddChild = { navController.navigate(FamlyRoutes.addChildTo(person.id)) }
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
                    },
                    navArgument("relationType") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                val personId = entry.arguments?.getString("personId")?.takeIf { it.isNotBlank() }
                val relativeOfId = entry.arguments?.getString("relativeOf")?.takeIf { it.isNotBlank() }
                val presetRelationType = entry.arguments?.getString("relationType")?.takeIf { it.isNotBlank() }
                    ?.let { raw -> runCatching { RelationType.valueOf(raw) }.getOrNull() }
                val existingPerson = members.find { it.id == personId }
                val relativeOfPerson = members.find { it.id == relativeOfId }
                // "Ich" darf nie löschbar sein - egal auf welchem Weg das Formular
                // erreicht wurde.
                val canDelete = existingPerson != null && existingPerson.id != SELF_PERSON_ID

                AddPersonScreen(
                    existingPerson = existingPerson,
                    relativeOf = relativeOfPerson,
                    presetRelationType = presetRelationType,
                    onClose = { navController.popBackStack() },
                    onSave = { result ->
                        // Doppel-Tap auf "Speichern" wuerde sonst zwei Personen
                        // anlegen - der Firestore-Aufruf ist asynchron.
                        Log.d(TAG, "onSave getriggert, isSaving=$isSaving, existingPerson=${existingPerson?.id}, relativeOf=${relativeOfPerson?.id}, relationType=${result.relationType}")
                        if (!isSaving) {
                            isSaving = true
                            coroutineScope.launch {
                                if (existingPerson != null) {
                                    Log.d(TAG, "updatePerson(${existingPerson.id}) wird aufgerufen")
                                    familyRepository.updatePerson(
                                        id = existingPerson.id,
                                        name = result.name,
                                        birthDate = result.birthDate,
                                        birthPlace = result.birthPlace,
                                        isDeceased = result.isDeceased,
                                        deathDate = result.deathDate,
                                        bio = result.bio
                                    ).onSuccess {
                                        Log.d(TAG, "updatePerson erfolgreich, navigiere zum Baum")
                                        goToTreeFocusedOn(existingPerson.id)
                                    }.onFailure { error ->
                                        Log.e(TAG, "updatePerson fehlgeschlagen", error)
                                        Toast.makeText(
                                            context,
                                            "Speichern fehlgeschlagen: ${error.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } else if (relativeOfPerson != null && result.relationType != null) {
                                    Log.d(TAG, "addPerson(relativeOf=${relativeOfPerson.id}, relationType=${result.relationType}) wird aufgerufen")
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
                                        Log.d(TAG, "addPerson erfolgreich, neue Person=${newPerson.id}, navigiere zum Baum")
                                        goToTreeFocusedOn(newPerson.id)
                                    }.onFailure { error ->
                                        Log.e(TAG, "addPerson fehlgeschlagen", error)
                                        Toast.makeText(
                                            context,
                                            "Speichern fehlgeschlagen: ${error.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } else {
                                    // Sollte über die Buttons nie erreichbar sein, aber
                                    // falls doch: sichtbar machen statt still zu verpuffen.
                                    Log.w(TAG, "onSave: weder existingPerson noch (relativeOf+relationType) gesetzt - nichts gespeichert")
                                    Toast.makeText(
                                        context,
                                        "Konnte nicht speichern: fehlende Zuordnung",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                isSaving = false
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