package com.example.studysmart.presentation.resources

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.studysmart.R
import com.example.studysmart.domain.model.Resource
import com.example.studysmart.domain.model.Subject
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Destination
@Composable
fun ResourcesScreenRoute() {
    val viewModel: ResourcesViewModel = hiltViewModel()
    val resources by viewModel.resources.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()

    ResourcesScreen(
        resources = resources,
        subjects = subjects,
        onSaveResource = viewModel::saveResource,
        onDeleteResource = viewModel::deleteResource
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResourcesScreen(
    resources: List<Resource>,
    subjects: List<Subject>,
    onSaveResource: (Resource) -> Unit,
    onDeleteResource: (Resource) -> Unit
) {
    var selectedSubjectId by rememberSaveable { mutableStateOf<Int?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val displayedResources = remember(resources, selectedSubjectId, searchQuery) {
        resources.filter { resource ->
            val matchesSubject = selectedSubjectId == null || resource.subjectId == selectedSubjectId
            val matchesSearch = searchQuery.isBlank() || resource.name.contains(searchQuery, ignoreCase = true)
            matchesSubject && matchesSearch
        }
    }

    // --- FORM STATE ---
    var isAddingMaterial by rememberSaveable { mutableStateOf(false) }
    var tempName by rememberSaveable { mutableStateOf("") }
    var tempUri by rememberSaveable { mutableStateOf("") }
    var tempSubjectId by rememberSaveable { mutableStateOf(-1) }
    var isLinkMode by rememberSaveable { mutableStateOf(true) }

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    // --- THE UNIVERSAL VAULT COPIER ---
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                isAddingMaterial = true
                Toast.makeText(context, "Adding to Vault...", Toast.LENGTH_SHORT).show()

                scope.launch(Dispatchers.IO) {
                    val result = copyFileToInternalStorage(context, uri)
                    withContext(Dispatchers.Main) {
                        if (result != null) {
                            tempUri = result.first
                            if (tempName.isBlank()) {
                                tempName = result.second.substringBeforeLast(".")
                            }
                            Toast.makeText(context, "File Ready! Click Save.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to copy file.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Study Vault", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            if (!isAddingMaterial) {
                ExtendedFloatingActionButton(
                    onClick = {
                        tempSubjectId = selectedSubjectId ?: (subjects.firstOrNull()?.subjectId ?: -1)
                        isAddingMaterial = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Material") // 🚀 Rounded Icon
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Material")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {

            // THE SEARCH BAR
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search materials...", color = MaterialTheme.colorScheme.secondary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            // 1. THE FILTER CHIPS (🚀 UPGRADED STYLING)
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedSubjectId == null,
                            onClick = { selectedSubjectId = null },
                            label = { Text("All", fontWeight = if (selectedSubjectId == null) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                    items(subjects) { subject ->
                        FilterChip(
                            selected = selectedSubjectId == subject.subjectId,
                            onClick = { selectedSubjectId = subject.subjectId },
                            label = { Text(subject.name, fontWeight = if (selectedSubjectId == subject.subjectId) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // 2. THE PREMIUM INLINE ADD FORM
            item {
                AnimatedVisibility(visible = isAddingMaterial, enter = expandVertically(), exit = shrinkVertically()) {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("New Material", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                                IconButton(onClick = { isAddingMaterial = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Form")
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isLinkMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                        .clickable { isLinkMode = true; tempUri = "" }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Web Link", color = if (isLinkMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (!isLinkMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                        .clickable { isLinkMode = false; tempUri = "" }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("File Upload", color = if (!isLinkMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (isLinkMode) {
                                OutlinedTextField(value = tempUri, onValueChange = { tempUri = it }, label = { Text("Paste URL (https://...)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                            } else {
                                val hasFile = tempUri.isNotBlank()
                                val primaryColor = MaterialTheme.colorScheme.primary

                                val dashedStroke = remember {
                                    Stroke(
                                        width = 4f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 20f), 0f)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (hasFile) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .clickable { documentPickerLauncher.launch("*/*") }
                                        .drawBehind {
                                            if (!hasFile) {
                                                drawRoundRect(
                                                    color = primaryColor.copy(alpha = 0.5f),
                                                    style = dashedStroke,
                                                    cornerRadius = CornerRadius(20.dp.toPx())
                                                )
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(if (hasFile) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (hasFile) Icons.Rounded.CheckCircle else Icons.Rounded.CloudUpload,
                                                contentDescription = "Upload",
                                                tint = if (hasFile) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                            text = if (hasFile) "File Attached Securely" else "Tap to browse device files",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (hasFile) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )

                                        if (!hasFile) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "PDFs, Docs, Videos, or Images",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = tempName,
                                onValueChange = { tempName = it },
                                label = { Text("Title (Auto-fills on upload!)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            var expanded by remember { mutableStateOf(false) }

                            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                                OutlinedTextField(
                                    value = subjects.find { it.subjectId == tempSubjectId }?.name ?: "Select Class",
                                    onValueChange = {}, readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    subjects.forEach { subject ->
                                        DropdownMenuItem(text = { Text(subject.name) }, onClick = { tempSubjectId = subject.subjectId ?: -1; expanded = false })
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (tempName.isNotBlank() && tempUri.isNotBlank() && tempSubjectId != -1) {
                                        val finalUri = if (isLinkMode && !tempUri.startsWith("http")) "https://$tempUri" else tempUri
                                        onSaveResource(Resource(name = tempName, uri = finalUri, subjectId = tempSubjectId))
                                        tempName = ""; tempUri = ""; isAddingMaterial = false
                                    } else {
                                        Toast.makeText(context, "Please fill out all fields", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Save Material", fontSize = 16.dp.value.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. THE LIST & EMPTY STATES
            if (displayedResources.isEmpty() && !isAddingMaterial) {
                item {
                    val emptyMessage = when {
                        searchQuery.isNotBlank() -> "No results found for \"$searchQuery\""
                        selectedSubjectId != null -> "No resources for this class yet."
                        else -> "Your Vault is Empty"
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 100.dp, start = 32.dp, end = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 🚀 NEW: Using the Mascot for the Empty Vault
                        Icon(
                            painter = painterResource(id = R.drawable.vault1),
                            contentDescription = "Empty Vault",
                            modifier = Modifier.size(210.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(emptyMessage, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (searchQuery.isBlank()) {
                            Text("Upload videos, PDFs, PPTs, or save web links here.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                items(displayedResources, key = { it.resourceId ?: it.hashCode() }) { resource ->
                    val subjectName = subjects.find { it.subjectId == resource.subjectId }?.name ?: "Unknown"

                    ResourceCard(
                        resource = resource,
                        subjectName = subjectName,
                        onClick = {
                            try {
                                if (resource.uri.startsWith("http")) {
                                    uriHandler.openUri(resource.uri)
                                } else {
                                    val file = File(resource.uri)
                                    if (file.exists()) {
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                        val extension = MimeTypeMap.getFileExtensionFromUrl(file.absolutePath)
                                        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"

                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, mimeType)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }

                                        context.startActivity(intent)
                                    } else {
                                        Toast.makeText(context, "File missing from Vault!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "No app found to open this file type.", Toast.LENGTH_LONG).show()
                            }
                        },
                        onDelete = {
                            onDeleteResource(resource)
                            if (!resource.uri.startsWith("http")) File(resource.uri).delete()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResourceCard(resource: Resource, subjectName: String, onClick: () -> Unit, onDelete: () -> Unit) {
    val isWebLink = resource.uri.startsWith("http")
    val isVideo = resource.uri.endsWith(".mp4") || resource.uri.endsWith(".mkv")

    val icon = when {
        isWebLink -> Icons.Rounded.Language
        isVideo -> Icons.Rounded.OndemandVideo
        else -> Icons.Rounded.Description
    }

    val iconTint = when {
        isWebLink -> Color(0xFF4158D0)
        isVideo -> Color(0xFFD041A9)
        else -> Color(0xFF6B8A7A)
    }

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = "Type", tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = resource.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subjectName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            }
            // 🚀 Upgraded the Delete Icon to Rounded
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)) }
        }
    }
}

private fun copyFileToInternalStorage(context: Context, uri: Uri): Pair<String, String>? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null

        var originalFileName = "Material_${System.currentTimeMillis()}"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) originalFileName = cursor.getString(index)
            }
        }

        val directory = File(context.filesDir, "study_materials")
        if (!directory.exists()) directory.mkdirs()

        val safeFileName = originalFileName.replace(" ", "_")
        val newFile = File(directory, safeFileName)
        val outputStream = FileOutputStream(newFile)

        inputStream.copyTo(outputStream)

        inputStream.close()
        outputStream.close()

        Pair(newFile.absolutePath, originalFileName)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}