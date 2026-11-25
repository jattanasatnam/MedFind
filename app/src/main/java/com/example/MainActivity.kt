package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Dataset
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.auth.attemptAutoSignIn
import com.example.data.firebase.configureAppCheck
import com.example.data.model.AccountStatus
import com.example.data.model.UserRole
import com.example.data.repository.MedFindRepository
import com.example.ui.components.LoadingStateBox
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.ConsoleOverviewPane
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.FirestoreCollectionTab
import com.example.ui.screens.FirestoreDataExplorerPane
import com.example.ui.screens.HealthAgentScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PharmacyDashboardScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SavedAndReportsScreen
import com.example.ui.screens.WelcomeAuthScreen
import com.example.ui.theme.AdminSidebarNavy
import com.example.ui.theme.MedFindTheme
import com.example.ui.theme.PharmacyHeroStart
import com.example.ui.viewmodel.AppWorkspaceMode
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.AuthenticationViewModel
import com.example.ui.viewmodel.HealthAgentViewModel
import com.example.ui.viewmodel.MedFindViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_HOME = "home"
const val ROUTE_SEARCH = "search"
const val ROUTE_AGENT = "agent"
const val ROUTE_PROFILE = "profile"
const val ROUTE_SAVED = "saved"

const val ROUTE_PHARMACY_PORTAL = "pharmacy_portal"

const val ROUTE_ADMIN_CONSOLE = "admin_console"
const val ROUTE_ADMIN_FIRESTORE = "admin_firestore"
const val ROUTE_ADMIN_APPROVALS = "admin_approvals"

data class BottomNavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val customerNavDestinations = listOf(
    BottomNavDestination(ROUTE_HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    BottomNavDestination(ROUTE_SEARCH, "Find Meds", Icons.Filled.Search, Icons.Outlined.Search, "nav_search"),
    BottomNavDestination(ROUTE_AGENT, "AI Guide", Icons.Filled.HealthAndSafety, Icons.Outlined.HealthAndSafety, "nav_agent"),
    BottomNavDestination(ROUTE_PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
)

private val pharmacyNavDestinations = listOf(
    BottomNavDestination(ROUTE_PHARMACY_PORTAL, "My Store", Icons.Filled.LocalPharmacy, Icons.Outlined.LocalPharmacy, "nav_pharmacy_store"),
    BottomNavDestination(ROUTE_AGENT, "AI Guide", Icons.Filled.HealthAndSafety, Icons.Outlined.HealthAndSafety, "nav_agent"),
    BottomNavDestination(ROUTE_PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
)

private val adminNavDestinations = listOf(
    BottomNavDestination(ROUTE_ADMIN_CONSOLE, "Console", Icons.Filled.Analytics, Icons.Outlined.Analytics, "nav_admin_console"),
    BottomNavDestination(ROUTE_ADMIN_FIRESTORE, "Firestore", Icons.Filled.Dataset, Icons.Outlined.Dataset, "nav_admin_firestore"),
    BottomNavDestination(ROUTE_ADMIN_APPROVALS, "Approvals", Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings, "nav_admin_approvals"),
    BottomNavDestination(ROUTE_PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            MedFindTheme {
                MedFindRoot()
            }
        }
    }
}

@Composable
fun MedFindRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val authViewModel: AuthenticationViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                AuthenticationViewModel(
                    db = FirebaseFirestore.getInstance(databaseId),
                    appContext = app.applicationContext,
                    auth = Firebase.auth
                )
            }
        }
    )

    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val workspaceMode by authViewModel.workspaceMode.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { authViewModel.markAutoSignInResolved() },
                onUnauthenticated = { authViewModel.markAutoSignInResolved() },
                scope = scope
            )
        } else {
            authViewModel.markAutoSignInResolved()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        when (val state = authState) {
            is AuthUiState.Loading -> {
                LoadingStateBox("Checking session...")
            }
            is AuthUiState.Unauthenticated -> {
                WelcomeAuthScreen(
                    onAuthSuccess = { authViewModel.markAutoSignInResolved() }
                )
            }
            is AuthUiState.Authenticated -> {
                if (state.accountStatus == AccountStatus.SUSPENDED) {
                    SuspendedAccountNotice(onSignOut = { authViewModel.signOut(credentialManager) })
                } else {
                    key(state.uid) {
                        val medFindViewModel: MedFindViewModel = viewModel(
                            key = state.uid,
                            factory = viewModelFactory {
                                initializer {
                                    val app = checkNotNull(this[APPLICATION_KEY]) {
                                        "APPLICATION_KEY missing from CreationExtras"
                                    }
                                    val databaseId = app.getString(R.string.firestore_database_id)
                                    val db = FirebaseFirestore.getInstance(databaseId)
                                    MedFindViewModel(
                                        repository = MedFindRepository(db),
                                        currentUserId = state.uid,
                                        userEmail = state.email,
                                        userDisplayName = state.displayName,
                                        isEmailVerified = state.isEmailVerified
                                    )
                                }
                            }
                        )
                        val healthAgentViewModel: HealthAgentViewModel = viewModel(key = "agent_${state.uid}")
                        val resolvedMode = when (workspaceMode) {
                            AppWorkspaceMode.ADMIN -> if (state.role == UserRole.ADMIN) AppWorkspaceMode.ADMIN else AppWorkspaceMode.CUSTOMER
                            AppWorkspaceMode.PHARMACY -> if (state.canAccessPharmacyPortal) AppWorkspaceMode.PHARMACY else AppWorkspaceMode.CUSTOMER
                            AppWorkspaceMode.CUSTOMER -> AppWorkspaceMode.CUSTOMER
                        }

                        MedFindNavHostShell(
                            authState = state,
                            workspaceMode = resolvedMode,
                            viewModel = medFindViewModel,
                            healthAgentViewModel = healthAgentViewModel,
                            onSelectWorkspaceMode = authViewModel::setWorkspaceMode,
                            onSignOut = { authViewModel.signOut(credentialManager) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MedFindNavHostShell(
    authState: AuthUiState.Authenticated,
    workspaceMode: AppWorkspaceMode,
    viewModel: MedFindViewModel,
    healthAgentViewModel: HealthAgentViewModel,
    onSelectWorkspaceMode: (AppWorkspaceMode) -> Unit,
    onSignOut: () -> Unit
) {
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var activeCollectionTab by rememberSaveable { mutableStateOf(FirestoreCollectionTab.INVENTORY) }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    key(workspaceMode) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val navItems = remember(workspaceMode) {
            when (workspaceMode) {
                AppWorkspaceMode.CUSTOMER -> customerNavDestinations
                AppWorkspaceMode.PHARMACY -> pharmacyNavDestinations
                AppWorkspaceMode.ADMIN -> adminNavDestinations
            }
        }
        val startDestination = when (workspaceMode) {
            AppWorkspaceMode.CUSTOMER -> ROUTE_HOME
            AppWorkspaceMode.PHARMACY -> ROUTE_PHARMACY_PORTAL
            AppWorkspaceMode.ADMIN -> ROUTE_ADMIN_CONSOLE
        }

        fun navigateTopLevel(route: String) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useRail) {
                        val barColor = when (workspaceMode) {
                            AppWorkspaceMode.CUSTOMER -> MaterialTheme.colorScheme.primary
                            AppWorkspaceMode.PHARMACY -> PharmacyHeroStart
                            AppWorkspaceMode.ADMIN -> AdminSidebarNavy
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(26.dp),
                                color = barColor,
                                shadowElevation = 8.dp,
                                modifier = Modifier
                                    .widthIn(max = 520.dp)
                                    .fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    navItems.forEach { dest ->
                                        val selected = currentRoute == dest.route
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .defaultMinSize(minHeight = 48.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(
                                                    if (selected) Color.White.copy(alpha = 0.22f)
                                                    else Color.Transparent
                                                )
                                                .clickable { navigateTopLevel(dest.route) }
                                                .minimumInteractiveComponentSize()
                                                .testTag(dest.testTag)
                                                .padding(horizontal = 6.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                    contentDescription = dest.label,
                                                    tint = if (selected) Color.White else Color.White.copy(alpha = 0.72f),
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Text(
                                                    text = dest.label,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                                    ),
                                                    color = if (selected) Color.White else Color.White.copy(alpha = 0.78f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        val isDarkRail = workspaceMode == AppWorkspaceMode.ADMIN ||
                            workspaceMode == AppWorkspaceMode.PHARMACY
                        val railContainer = when (workspaceMode) {
                            AppWorkspaceMode.ADMIN -> AdminSidebarNavy
                            AppWorkspaceMode.PHARMACY -> PharmacyHeroStart
                            AppWorkspaceMode.CUSTOMER -> MaterialTheme.colorScheme.surface
                        }
                        NavigationRail(containerColor = railContainer) {
                            Spacer(modifier = Modifier.height(8.dp))
                            navItems.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label) },
                                    colors = if (isDarkRail) {
                                        NavigationRailItemDefaults.colors(
                                            selectedIconColor = Color.White,
                                            selectedTextColor = Color.White,
                                            indicatorColor = Color.White.copy(alpha = 0.18f),
                                            unselectedIconColor = Color.White.copy(alpha = 0.7f),
                                            unselectedTextColor = Color.White.copy(alpha = 0.7f)
                                        )
                                    } else {
                                        NavigationRailItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                        if (!isDarkRail) {
                            VerticalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    ) {
                        when (workspaceMode) {
                            AppWorkspaceMode.CUSTOMER -> {
                                composable(ROUTE_HOME) {
                                    HomeScreen(
                                        viewModel = viewModel,
                                        onNavigateToSearch = { navigateTopLevel(ROUTE_SEARCH) },
                                        onNavigateToAgent = { navigateTopLevel(ROUTE_AGENT) },
                                        onNavigateToSaved = { navController.navigate(ROUTE_SAVED) }
                                    )
                                }
                                composable(ROUTE_SEARCH) {
                                    DiscoverScreen(viewModel = viewModel)
                                }
                                composable(ROUTE_AGENT) {
                                    HealthAgentScreen(
                                        agentViewModel = healthAgentViewModel,
                                        medFindViewModel = viewModel,
                                        onFindMedicineInStores = { med ->
                                            viewModel.selectMedicine(med)
                                            navigateTopLevel(ROUTE_SEARCH)
                                        },
                                        onOpenSearch = { navigateTopLevel(ROUTE_SEARCH) }
                                    )
                                }
                                composable(ROUTE_SAVED) {
                                    SavedAndReportsScreen(
                                        viewModel = viewModel,
                                        onOpenPharmacy = { pharmacy ->
                                            viewModel.selectPharmacy(pharmacy)
                                            navigateTopLevel(ROUTE_SEARCH)
                                        }
                                    )
                                }
                                composable(ROUTE_PROFILE) {
                                    ProfileScreen(
                                        viewModel = viewModel,
                                        userRole = authState.role,
                                        canAccessPharmacyPortal = authState.canAccessPharmacyPortal,
                                        workspaceMode = workspaceMode,
                                        onSelectWorkspaceMode = onSelectWorkspaceMode,
                                        onSignOut = onSignOut
                                    )
                                }
                            }

                            AppWorkspaceMode.PHARMACY -> {
                                composable(ROUTE_PHARMACY_PORTAL) {
                                    PharmacyDashboardScreen(viewModel = viewModel)
                                }
                                composable(ROUTE_AGENT) {
                                    HealthAgentScreen(
                                        agentViewModel = healthAgentViewModel,
                                        medFindViewModel = viewModel,
                                        onFindMedicineInStores = { med ->
                                            viewModel.selectMedicine(med)
                                            onSelectWorkspaceMode(AppWorkspaceMode.CUSTOMER)
                                        },
                                        onOpenSearch = { onSelectWorkspaceMode(AppWorkspaceMode.CUSTOMER) }
                                    )
                                }
                                composable(ROUTE_PROFILE) {
                                    ProfileScreen(
                                        viewModel = viewModel,
                                        userRole = authState.role,
                                        canAccessPharmacyPortal = authState.canAccessPharmacyPortal,
                                        workspaceMode = workspaceMode,
                                        onSelectWorkspaceMode = onSelectWorkspaceMode,
                                        onSignOut = onSignOut
                                    )
                                }
                            }

                            AppWorkspaceMode.ADMIN -> {
                                composable(ROUTE_ADMIN_CONSOLE) {
                                    ConsoleOverviewPane(
                                        viewModel = viewModel,
                                        onOpenCollection = { col ->
                                            activeCollectionTab = col
                                            navigateTopLevel(ROUTE_ADMIN_FIRESTORE)
                                        },
                                        onOpenPharmacyPortal = {
                                            onSelectWorkspaceMode(AppWorkspaceMode.PHARMACY)
                                        },
                                        onOpenAdminGovernance = {
                                            navigateTopLevel(ROUTE_ADMIN_APPROVALS)
                                        },
                                        onNavigateToSearch = {
                                            onSelectWorkspaceMode(AppWorkspaceMode.CUSTOMER)
                                        },
                                        onNavigateToAgent = {
                                            onSelectWorkspaceMode(AppWorkspaceMode.CUSTOMER)
                                        }
                                    )
                                }
                                composable(ROUTE_ADMIN_FIRESTORE) {
                                    FirestoreDataExplorerPane(
                                        viewModel = viewModel,
                                        selectedCollection = activeCollectionTab,
                                        onSelectCollection = { activeCollectionTab = it },
                                        onJumpToPharmacyPortal = {
                                            onSelectWorkspaceMode(AppWorkspaceMode.PHARMACY)
                                        }
                                    )
                                }
                                composable(ROUTE_ADMIN_APPROVALS) {
                                    AdminDashboardScreen(viewModel = viewModel)
                                }
                                composable(ROUTE_PROFILE) {
                                    ProfileScreen(
                                        viewModel = viewModel,
                                        userRole = authState.role,
                                        canAccessPharmacyPortal = authState.canAccessPharmacyPortal,
                                        workspaceMode = workspaceMode,
                                        onSelectWorkspaceMode = onSelectWorkspaceMode,
                                        onSignOut = onSignOut
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
private fun SuspendedAccountNotice(onSignOut: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.widthIn(max = 460.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Account Suspended",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "This account has been suspended by a MedFind administrator and cannot query or publish records. Sign out or contact platform support.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("suspended_sign_out_button")
                ) {
                    Text("Sign Out")
                }
            }
        }
    }
}
