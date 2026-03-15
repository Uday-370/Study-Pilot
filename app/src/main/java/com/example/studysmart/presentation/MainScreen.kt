package com.example.studysmart.presentation

import android.app.Activity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.studysmart.presentation.destinations.CalendarScreenRouteDestination
import com.example.studysmart.presentation.destinations.DashboardScreenRouteDestination
import com.example.studysmart.presentation.destinations.AccountScreenRouteDestination
import com.example.studysmart.presentation.destinations.ResourcesScreenRouteDestination
import com.example.studysmart.presentation.destinations.OnboardingScreenRouteDestination // 🚀 IMPORT ADDED
import com.example.studysmart.presentation.destinations.SplashScreenRouteDestination
import com.example.studysmart.presentation.navigation.StudySmartBottomBar
import com.example.studysmart.presentation.session.StudySessionTimerService
import com.example.studysmart.util.AppReviewManager
import com.example.studysmart.util.StreakManager
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.navigation.dependency
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialNavigationApi::class)
@Composable
fun MainScreen(
    timerService: StudySessionTimerService
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val streakManager = remember { StreakManager(context) }
    val currentStreak by streakManager.currentStreak.collectAsState(initial = 0)
    val hasSeenReview by streakManager.hasSeenReviewPrompt.collectAsState(initial = true)

    val navHostEngine = rememberAnimatedNavHostEngine(
        rootDefaultAnimations = RootNavGraphDefaultAnimations(
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        )
    )

    // Hide the bottom bar on the Onboarding screen!
    val showBottomBar = currentDestination?.route in listOf(
        DashboardScreenRouteDestination.route,
        CalendarScreenRouteDestination.route,
        ResourcesScreenRouteDestination.route,
        AccountScreenRouteDestination.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                StudySmartBottomBar(
                    navController = navController,
                    currentDestination = currentDestination
                )
            }
        }
    ) { paddingValues ->
        DestinationsNavHost(
            navGraph = NavGraphs.root,
            navController = navController,
            engine = navHostEngine,
            // 🚀 ALWAYS start at Splash; it will handle the logic
            startRoute = SplashScreenRouteDestination,
            modifier = Modifier.padding(paddingValues),
            dependenciesContainerBuilder = {
                dependency(timerService)
            }
        )

        // 3-Day Streak Review Trigger
        if (currentStreak >= 3 && !hasSeenReview) {
            AlertDialog(
                onDismissRequest = {
                    scope.launch { streakManager.markReviewPromptSeen() }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = "Rate Us",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(40.dp)
                    )
                },
                title = {
                    Text(
                        text = "You are on fire! 🔥",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = "You've built a $currentStreak-day focus streak! Are you enjoying StudyPilot? A 5-star rating helps us keep the app ad-free.",
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch { streakManager.markReviewPromptSeen() }

                            val activity = context as? Activity
                            if (activity != null) {
                                AppReviewManager.askForReview(activity)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Rate 5 Stars ⭐", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            scope.launch { streakManager.markReviewPromptSeen() }
                        }
                    ) {
                        Text("Not Now", color = MaterialTheme.colorScheme.secondary)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}