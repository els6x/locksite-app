package com.locksite.app.presentation.navigation

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.locksite.app.AppContainer
import com.locksite.app.presentation.adddomain.AddDomainScreen
import com.locksite.app.presentation.adddomain.AddDomainViewModel
import com.locksite.app.presentation.blocklist.BlocklistScreen
import com.locksite.app.presentation.blocklist.BlocklistViewModel
import com.locksite.app.presentation.dashboard.DashboardScreen
import com.locksite.app.presentation.dashboard.DashboardViewModel
import com.locksite.app.presentation.diagnostics.DiagnosticsScreen
import com.locksite.app.presentation.diagnostics.DiagnosticsViewModel
import com.locksite.app.presentation.onboarding.OnboardingScreen
import com.locksite.app.presentation.settings.SettingsScreen
import com.locksite.app.ui.components.BottomDestination
import com.locksite.app.ui.components.LockSiteBottomBar
import com.locksite.app.ui.theme.LockSiteColors
import com.locksite.app.ui.theme.Spacing

/**
 * App shell: navigation graph plus the three-destination bottom bar.
 *
 * The bottom bar is hidden on onboarding and on the add-domain screen, where a single
 * task should not compete with navigation (Hick + Von Restorff).
 */
@Composable
fun LockSiteApp(container: AppContainer) {
    val navController = rememberNavController()
    // One source of truth for the display preference, shared by Settings, Home and the list.
    val maskDomains by container.privacyPreferences.maskDomains.collectAsStateWithLifecycle()
    val startDestination = remember {
        if (container.onboardingStore.isCompleted()) Routes.HOME else Routes.ONBOARDING
    }

    // A conflict with another VPN is an external fact. Re-check it whenever the app comes
    // back to the foreground so the dashboard cannot keep saying "Bentrok" after the other
    // VPN was turned off (PRD FR-030).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                container.vpnController.refreshConflictState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute != null && Routes.topLevel.contains(currentRoute)

    // The VPN consent dialog is a system Activity, so only this composable can launch it.
    val vpnConsentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            container.vpnController.start()
        } else {
            container.vpnController.onPermissionDenied()
        }
    }

    fun activateProtection() {
        val consent = container.vpnController.prepareIntent()
        if (consent == null) {
            container.vpnController.start()
        } else {
            vpnConsentLauncher.launch(consent)
        }
    }

    fun navigateToTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = LockSiteColors.Canvas,
        bottomBar = {
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = Spacing.screenMargin, vertical = Spacing.s),
                ) {
                    LockSiteBottomBar(
                        current = when (currentRoute) {
                            Routes.LOCKED_LIST -> BottomDestination.LIST
                            Routes.SETTINGS -> BottomDestination.SETTINGS
                            else -> BottomDestination.HOME
                        },
                        onSelect = { destination ->
                            navigateToTopLevel(
                                when (destination) {
                                    BottomDestination.HOME -> Routes.HOME
                                    BottomDestination.LIST -> Routes.LOCKED_LIST
                                    BottomDestination.SETTINGS -> Routes.SETTINGS
                                },
                            )
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
            // Short fade + a small slide: enough to read as motion, never a decoration.
            enterTransition = {
                fadeIn(tween(durationMillis = 180)) +
                    slideInHorizontally(tween(durationMillis = 180)) { it / 12 }
            },
            exitTransition = { fadeOut(tween(durationMillis = 140)) },
            popEnterTransition = { fadeIn(tween(durationMillis = 180)) },
            popExitTransition = {
                fadeOut(tween(durationMillis = 140)) +
                    slideOutHorizontally(tween(durationMillis = 140)) { it / 12 }
            },
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onStart = {
                        container.onboardingStore.markCompleted()
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }

            composable(Routes.HOME) {
                val viewModel: DashboardViewModel = viewModel {
                    DashboardViewModel(
                        container.blocklistRepository,
                        container.vpnController,
                        container.privacyPreferences.maskDomains,
                    )
                }
                DashboardScreen(
                    viewModel = viewModel,
                    onAddDomain = { navController.navigate(Routes.ADD_DOMAIN) },
                    onOpenList = { navigateToTopLevel(Routes.LOCKED_LIST) },
                    onOpenSettings = { navigateToTopLevel(Routes.SETTINGS) },
                    onActivateProtection = ::activateProtection,
                )
            }

            composable(Routes.ADD_DOMAIN) {
                val viewModel: AddDomainViewModel = viewModel {
                    AddDomainViewModel(container.addDomainUseCase)
                }
                AddDomainScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenList = {
                        navController.navigate(Routes.LOCKED_LIST) {
                            popUpTo(Routes.HOME)
                        }
                    },
                )
            }

            composable(Routes.LOCKED_LIST) {
                val viewModel: BlocklistViewModel = viewModel {
                    BlocklistViewModel(
                        container.blocklistRepository,
                        container.privacyPreferences.maskDomains,
                    )
                }
                BlocklistScreen(
                    viewModel = viewModel,
                    onAddDomain = { navController.navigate(Routes.ADD_DOMAIN) },
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    maskDomains = maskDomains,
                    onToggleMask = container.privacyPreferences::setMaskDomains,
                    onOpenDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) },
                )
            }

            composable(Routes.DIAGNOSTICS) {
                val viewModel: DiagnosticsViewModel = viewModel {
                    DiagnosticsViewModel(container.blocklistRepository, container.vpnController)
                }
                DiagnosticsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
