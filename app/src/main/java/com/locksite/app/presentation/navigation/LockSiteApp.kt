package com.locksite.app.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
    val startDestination = remember {
        if (container.onboardingStore.isCompleted()) Routes.HOME else Routes.ONBOARDING
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute != null && Routes.topLevel.contains(currentRoute)

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
                    DashboardViewModel(container.blocklistRepository)
                }
                DashboardScreen(
                    viewModel = viewModel,
                    onAddDomain = { navController.navigate(Routes.ADD_DOMAIN) },
                    onOpenList = { navigateToTopLevel(Routes.LOCKED_LIST) },
                    onOpenSettings = { navigateToTopLevel(Routes.SETTINGS) },
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
                    BlocklistViewModel(container.blocklistRepository)
                }
                BlocklistScreen(
                    viewModel = viewModel,
                    onAddDomain = { navController.navigate(Routes.ADD_DOMAIN) },
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
        }
    }
}
