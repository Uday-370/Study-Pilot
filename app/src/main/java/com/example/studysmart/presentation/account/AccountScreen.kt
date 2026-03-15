package com.example.studysmart.presentation.account

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Delete // 🚀 ROUNDED
import androidx.compose.material.icons.rounded.Email // 🚀 ROUNDED
import androidx.compose.material.icons.rounded.Info // 🚀 ROUNDED
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.studysmart.R
import com.example.studysmart.domain.model.Session
import com.example.studysmart.presentation.components.StudyBarChart
import com.example.studysmart.util.ShareAchievementUtils
import com.example.studysmart.util.StreakManager
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Destination
@Composable
fun AccountScreenRoute() {
    val viewModel: AccountViewModel = hiltViewModel()
    val name by viewModel.userName.collectAsStateWithLifecycle()
    val profilePicUri by viewModel.profilePicUri.collectAsStateWithLifecycle()
    val weeklyData by viewModel.weeklyStudyData.collectAsStateWithLifecycle()
    val totalHours by viewModel.totalStudiedHours.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val streakManager = remember { StreakManager(context) }
    val currentStreak by streakManager.currentStreak.collectAsStateWithLifecycle(initialValue = 0)

    AccountScreen(
        name = name ?: "Student",
        profilePicUri = profilePicUri,
        weeklyData = weeklyData,
        totalHours = totalHours,
        currentStreak = currentStreak,
        labels = viewModel.weeklyStudyLabels,
        sessions = sessions,
        onNameChange = viewModel::onNewNameChange,
        onProfilePicChange = viewModel::onProfilePicChange,
        onDeleteSession = viewModel::deleteSession
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountScreen(
    name: String,
    profilePicUri: String?,
    weeklyData: List<Float>,
    totalHours: Float,
    currentStreak: Int,
    labels: List<String>,
    sessions: List<Session>,
    onNameChange: (String) -> Unit,
    onProfilePicChange: (String) -> Unit,
    onDeleteSession: (Session) -> Unit
) {
    var showNameDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(name) }

    var showSettingsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showAboutDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                onProfilePicChange(it.toString())
            }
        }
    )

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Change Name") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Your Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onNameChange(tempName)
                    showNameDialog = false
                }) { Text("Save") }
            }
        )
    }

    if (showAboutDialog) {
        AboutAppDialog(onDismiss = { showAboutDialog = false })
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )

                // GENERAL SECTION
                SettingsSectionHeader("General")
                SettingsMenuItem(
                    icon = Icons.Rounded.Notifications,
                    title = "Notifications",
                    subtitle = "Manage reminders and alarms.",
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { showSettingsSheet = false }
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        try { context.startActivity(intent) } catch (e: Exception) { e.printStackTrace() }
                    }
                )

                // SUPPORT SECTION
                SettingsSectionHeader("Support Us")
                SettingsMenuItem(
                    icon = Icons.Rounded.Star,
                    title = "Rate Us",
                    subtitle = "Love StudyPilot? Leave a 5-star review!",
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { showSettingsSheet = false }
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")))
                        } catch (e: ActivityNotFoundException) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")))
                        }
                    }
                )
                SettingsMenuItem(
                    icon = Icons.Rounded.Share,
                    title = "Share StudyPilot",
                    subtitle = "Help your friends stay focused.",
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { showSettingsSheet = false }
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "I use StudyPilot to stay focused and track my study streaks! Download it here: https://play.google.com/store/apps/details?id=${context.packageName}")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share via"))
                    }
                )
                SettingsMenuItem(
                    icon = Icons.Rounded.Feedback,
                    title = "Give Feedback",
                    subtitle = "Report a bug or suggest a feature.",
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { showSettingsSheet = false }
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:dev.studypilot@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "StudyPilot Feedback")
                        }
                        try { context.startActivity(intent) } catch (e: Exception) { Toast.makeText(context, "No email app found.", Toast.LENGTH_SHORT).show() }
                    }
                )

                // LEGAL & ABOUT SECTION
                SettingsSectionHeader("Legal & Info")
                SettingsMenuItem(
                    icon = Icons.Rounded.Policy,
                    title = "Privacy Policy",
                    subtitle = "How we protect your data.",
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { showSettingsSheet = false }
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.freeprivacypolicy.com/live/98a2dde9-1f3d-43ef-8907-f1f85d13a361")))
                    }
                )
                SettingsMenuItem(
                    icon = Icons.Rounded.Gavel,
                    title = "Terms of Service",
                    subtitle = "Rules and guidelines.",
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { showSettingsSheet = false }
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://policies.google.com/terms")))
                    }
                )
                SettingsMenuItem(
                    icon = Icons.Rounded.Info, // 🚀 ROUNDED
                    title = "About App",
                    subtitle = "Version info and developer details.",
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showSettingsSheet = false
                            showAboutDialog = true
                        }
                    }
                )
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mastery Hub", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 1. PROFILE HEADER
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (profilePicUri != null) {
                        AsyncImage(
                            model = profilePicUri,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = name.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold), // 🚀 BOLDER
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { showNameDialog = true },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit Name",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // 2. STATS CARDS
            item {
                val totalMins = (totalHours * 60).toInt()
                val tHours = totalMins / 60
                val tMins = totalMins % 60
                val totalFocusedString = if (tHours > 0) "${tHours}h ${tMins}m" else "${tMins}m"

                val bestDayMins = ((weeklyData.maxOrNull() ?: 0f) * 60).toInt()
                val bHours = bestDayMins / 60
                val bMins = bestDayMins % 60
                val bestDayString = if (bHours > 0) "${bHours}h ${bMins}m" else "${bMins}m"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ElevatedCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Total Focused", style = MaterialTheme.typography.labelMedium)
                            Text(totalFocusedString, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    }
                    ElevatedCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Best Day", style = MaterialTheme.typography.labelMedium)
                            Text(bestDayString, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    }
                }
            }

            // 3. THE VIRAL SHARE BUTTON
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFD48A88), Color(0xFFE4A9A8))
                            )
                        )
                        .clickable {
                            ShareAchievementUtils.shareToInstagramStory(
                                context = context,
                                name = name,
                                totalHours = totalHours,
                                currentStreak = currentStreak
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocalFireDepartment, contentDescription = "Share", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Share Achievement",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            // 4. WEEKLY CHART
            item {
                Spacer(modifier = Modifier.height(24.dp))
                StudyBarChart(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    data = weeklyData,
                    labels = labels
                )
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }

            // 5. FOCUS ARCHIVE
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = "Focus Archive",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (sessions.isEmpty()) {
                // 🚀 ZEN MASCOT EMPTY STATE
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.accnt_stat),
                            contentDescription = "Empty Archive",
                            modifier = Modifier.size(160.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Your archive is pristine.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Complete a focus session to see your history here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(sessions, key = { it.sessionId ?: it.hashCode() }) { session ->
                    SessionHistoryCard(session = session, onDelete = { onDeleteSession(session) })
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = "Go",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun SessionHistoryCard(
    session: Session,
    onDelete: () -> Unit
) {
    val dateStr = remember(session.date) {
        Instant.ofEpochMilli(session.date)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    }

    val totalMins = (session.duration / 60).toInt().coerceAtLeast(1)
    val hours = totalMins / 60
    val mins = totalMins % 60
    val durationString = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = session.relatedToSubject,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$dateStr • $durationString",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.Delete, // 🚀 ROUNDED
                    contentDescription = "Delete Session",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun AboutAppDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    val appVersion = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFD48A88), Color(0xFFE4A9A8))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.idea),
                        contentDescription = stringResource(R.string.cd_self_improvement_illustration),
                        tint = Color.White,
                        modifier = Modifier
                            .size(78.dp)
                            .rotate(-25f) // 🚀 THE FIX: Tips the icon back by exactly 25 degrees
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "StudyPilot",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Version $appVersion",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Designed to help you conquer distractions, build discipline, and master your subjects.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:dev.studypilot@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "StudyPilot Support")
                        }
                        try { context.startActivity(intent) } catch (e: Exception) { Toast.makeText(context, "No email app found.", Toast.LENGTH_SHORT).show() }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Email, // 🚀 ROUNDED
                            contentDescription = "Email",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "dev.studypilot@gmail.com",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}