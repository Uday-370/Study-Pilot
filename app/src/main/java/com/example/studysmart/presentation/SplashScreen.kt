package com.example.studysmart.presentation // Keep your package!

import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import com.ramcosta.composedestinations.navigation.popUpTo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.example.studysmart.R
import com.example.studysmart.presentation.destinations.DashboardScreenRouteDestination
import com.example.studysmart.presentation.destinations.OnboardingScreenRouteDestination
import com.example.studysmart.presentation.destinations.SplashScreenRouteDestination
import com.example.studysmart.util.AppPreferences
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// 🚀 1. NEW: A tiny ViewModel to fetch the app's memory
@HiltViewModel
class SplashViewModel @Inject constructor(
    appPreferences: AppPreferences
) : ViewModel() {
    val hasCompletedOnboarding: Flow<Boolean> = appPreferences.hasCompletedOnboarding
}

@Destination(start = true)
@Composable
fun SplashScreenRoute(
    navigator: DestinationsNavigator,
    viewModel: SplashViewModel = hiltViewModel()
) {
    // Collect the memory state from DataStore
    val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsState(initial = false)

    LaunchedEffect(key1 = true) {
        delay(4000L) // Duration of your splash_animation video

        // 🚀 THE BULLETPROOF FIX: Pop the Splash Screen off the stack BEFORE navigating
        navigator.popBackStack()

        // Navigate based on memory
        if (hasCompletedOnboarding) {
            navigator.navigate(DashboardScreenRouteDestination)
        } else {
            navigator.navigate(OnboardingScreenRouteDestination)
        }
    }

    SplashScreenContent()
}

// ... [Keep your exact SplashScreenContent() below here] ...

@Composable
fun SplashScreenContent() {
    val context = LocalContext.current
    val videoUri = remember {
        Uri.parse("android.resource://${context.packageName}/${R.raw.splash_animation}")
    }

    var isVideoReady by remember { mutableStateOf(false) }

    // 🚀 NEW: State to track when the text should appear
    var isTextVisible by remember { mutableStateOf(false) }

    // 🚀 NEW: Once the video starts playing, wait 600ms, then trigger the text
    LaunchedEffect(isVideoReady) {
        if (isVideoReady) {
            delay(600) // The "Stagger" delay (adjust if you want it faster/slower)
            isTextVisible = true
        }
    }

    val message = remember {
        listOf(
            "Rise above distractions with StudyPilot.",
            "Focus. Execute. Win.",
            "Lock in. Level up.",
            "StudyPilot: Ready for liftoff.",
            "Built for achievers.",
            "Focus like never before.",
            "Push past your limits.",
            "The strongest minds never quit.",
            "Your training arc begins now.",
            "Unlock your potential.",
            "Rise beyond limits.",
            "Train. Improve. Repeat.",
            "Training Arc: Activated.",
            "Focus Mode: Unlocked."
        ).random()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .heightIn(max = 300.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(videoUri)
                        setMediaController(null)
                        setOnPreparedListener { mediaPlayer ->
                            mediaPlayer.isLooping = true
                            mediaPlayer.setOnInfoListener { _, what, _ ->
                                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                                    isVideoReady = true
                                    true
                                } else false
                            }
                            mediaPlayer.start()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            this@Column.AnimatedVisibility(
                visible = !isVideoReady,
                exit = fadeOut(animationSpec = tween(200))
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.White))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 🚀 THE MAGIC: The text now slides up and fades in, stealing focus!
        AnimatedVisibility(
            visible = isTextVisible,
            enter = fadeIn(animationSpec = tween(800)) + slideInVertically(
                initialOffsetY = { 40 }, // Starts slightly lower and glides up
                animationSpec = tween(800)
            )
        ) {
            Text(
                text = message,
                // I slightly increased the typography to titleLarge so it commands more authority
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                ),
                color = Color(0xFF1A1A1A), // A slightly darker, punchier contrast
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1.5f))
    }
}