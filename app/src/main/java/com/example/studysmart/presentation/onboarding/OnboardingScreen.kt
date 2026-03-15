package com.example.studysmart.presentation.onboarding

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studysmart.R
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studysmart.presentation.destinations.DashboardScreenRouteDestination
import com.example.studysmart.presentation.destinations.OnboardingScreenRouteDestination
import com.example.studysmart.util.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// 1. The ViewModel that saves the data
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {
    fun completeOnboarding() {
        viewModelScope.launch {
            appPreferences.saveOnboardingCompleted()
        }
    }
}

// 2. Updated Route
@Destination
@Composable
fun OnboardingScreenRoute(
    navigator: DestinationsNavigator,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    PremiumOnboardingScreen(
        onComplete = {
            // 1. Tell the app's memory that we finished!
            viewModel.completeOnboarding()

            // 2. Navigate to Dashboard and DESTROY the Onboarding backstack
            // so they can't press the physical back button to return here!
            navigator.navigate(DashboardScreenRouteDestination) {
                popUpTo(OnboardingScreenRouteDestination.route) {
                    inclusive = true
                }
            }
        }
    )
}

data class PremiumPage(
    val title: String,
    val description: String,
    val iconRes: Int? = null,
    val vectorIcon: ImageVector? = null
)

val premiumPages = listOf(
    PremiumPage(
        title = "Awaken The Pilot",
        description = "Your mind is a weapon. Strip away the noise, lock onto your target, and enter absolute flow.",
        iconRes = R.drawable.study_pilot
    ),
    PremiumPage(
        title = "Forge Your Vault",
        description = "Knowledge requires architecture. Track sessions, organize mastery, and watch your empire grow.",
        iconRes = R.drawable.forge_vault
    ),
    PremiumPage(
        title = "Infinite Discipline",
        description = "Discipline is the bridge between goals and reality. Keep the streak alive. Become undeniable.",
        iconRes = R.drawable.infinite
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PremiumOnboardingScreen(onComplete: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { premiumPages.size })
    val scope = rememberCoroutineScope()

    // 🚀 ALIGNED WITH THEME: Uses native background color
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. THE FLUID HERO GRADIENT
        FluidGradientBackground(pagerState = pagerState)

        // 2. THE MATERIAL PARALLAX PAGER
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            MaterialParallaxPage(
                page = page,
                pagerState = pagerState,
                data = premiumPages[page]
            )
        }

        // 3. THE UNIFIED ACTION BAR
        BottomLiquidInterface(
            pagerState = pagerState,
            onNextClick = {
                if (pagerState.currentPage < premiumPages.size - 1) {
                    scope.launch {
                        pagerState.animateScrollToPage(
                            pagerState.currentPage + 1,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                } else {
                    onComplete()
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MaterialParallaxPage(
    page: Int,
    pagerState: PagerState,
    data: PremiumPage
) {
    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
    val absOffset = pageOffset.absoluteValue

    // Soft levitation for the inner icon
    val infiniteTransition = rememberInfiniteTransition(label = "iconFloat")
    val hoverY by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconHover"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // LAYER 1: The Icon Portal (Z-Axis Depth Scaling)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 160.dp)
                .graphicsLayer {
                    // Shrinks into the background and fades when swiped
                    scaleX = 1f - (absOffset * 0.4f)
                    scaleY = 1f - (absOffset * 0.4f)
                    alpha = 1f - absOffset
                    translationY = hoverY // Levitates
                },
            contentAlignment = Alignment.Center
        ) {
            // A clean, soft surface portal behind the icon (Matches Dashboard ElevatedCard)
            Surface(
                modifier = Modifier.size(200.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {}

            // 🚀 THE FIX: We have REMOVED the glowing aura 'Box' that was here.

            // The Primary Tinted Icon (Scaled from 150.dp -> 130.dp for balance)
            if (data.iconRes != null) {
                Icon(
                    painter = painterResource(id = data.iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(130.dp)
                )
            } else if (data.vectorIcon != null) {
                Icon(
                    imageVector = data.vectorIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(130.dp)
                )
            }
        }

        // LAYER 2: The Typography (Fast Horizontal Parallax)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
                .align(Alignment.BottomCenter)
                .padding(bottom = 240.dp)
                .graphicsLayer {
                    translationX = pageOffset * 500f // Fast swipe off-screen
                    alpha = 1f - (absOffset * 1.5f).coerceIn(0f, 1f)
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = data.title,
                style = MaterialTheme.typography.displaySmall, // Modern Material Typography
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                lineHeight = 44.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = data.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FluidGradientBackground(pagerState: PagerState) {
    val scrollPosition = (pagerState.currentPage + pagerState.currentPageOffsetFraction)

    // Dynamic Primary & Tertiary colors exactly like your HeroActionCard
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    // Slowly rotates the aura for a liquid feel
    val infiniteTransition = rememberInfiniteTransition(label = "auraRotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .blur(80.dp) // Massive blur creates the "Liquid Glow"
            .graphicsLayer { rotationZ = rotation }
    ) {
        val width = size.width
        val height = size.height

        // The fluid shifts its center of gravity when you swipe
        val shiftX = (scrollPosition * width * 0.4f)

        // Massive glowing orb using your theme's exact gradients
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.25f),
                    tertiaryColor.copy(alpha = 0.1f),
                    Color.Transparent
                ),
                center = Offset(width * 0.8f - shiftX, height * 0.3f),
                radius = width * 1.5f
            )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BottomLiquidInterface(
    pagerState: PagerState,
    onNextClick: () -> Unit
) {
    val isLastPage = pagerState.currentPage == premiumPages.size - 1

    val buttonWidth by animateDpAsState(
        targetValue = if (isLastPage) 220.dp else 72.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "btnWidth"
    )

    // Grabs the exact same gradient from your HeroActionCard in the Dashboard!
    val heroGradient = Brush.linearGradient(
        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Material 3 Morphing Indicators
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(premiumPages.size) { index ->
                    val selected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (selected) 32.dp else 10.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                        label = "indicator"
                    )
                    Box(
                        modifier = Modifier
                            .height(10.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                    )
                }
            }

            // The Liquid Action Button (Matches Hero Card!)
            Box(
                modifier = Modifier
                    .height(72.dp)
                    .width(buttonWidth)
                    .clip(RoundedCornerShape(36.dp))
                    .background(heroGradient) // Unified Theme
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onNextClick
                    )
                    .animateContentSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isLastPage) {
                    Text(
                        text = "START FOCUS",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        style = MaterialTheme.typography.titleMedium
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}