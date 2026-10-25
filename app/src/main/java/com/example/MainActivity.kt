package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.IconButton
import com.example.ui.components.CloudSyncDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SupportedFormat
import com.example.ui.components.DocumentPreviewDialog
import com.example.ui.screens.CompilerScreen
import com.example.ui.screens.ConverterScreen
import com.example.ui.screens.ExplorerScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppViewModel

enum class AppNavTab(
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    PROJECTS("Proyectos", Icons.Default.Folder, "tab_projects"),
    EXPLORER("Explorador", Icons.Default.Source, "tab_explorer"),
    COMPILER("Compilador", Icons.Default.Layers, "tab_compiler"),
    CONVERTER("Conversor", Icons.Default.SwapHoriz, "tab_converter"),
    HISTORY("Historial", Icons.Default.History, "tab_history")
}

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            viewModel.authManager.attemptAutoSignIn(this)
        }

        setContent {
            MyApplicationTheme {
                val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
                var showCloudDialog by remember { mutableStateOf(false) }

                val projects by viewModel.projects.collectAsStateWithLifecycle()
                val documents by viewModel.allDocuments.collectAsStateWithLifecycle()
                val compilations by viewModel.compilations.collectAsStateWithLifecycle()
                val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()
                val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                val formatFilter by viewModel.formatFilter.collectAsStateWithLifecycle()
                val selectedDocIds by viewModel.selectedDocIdsForCompilation.collectAsStateWithLifecycle()
                val isCompiling by viewModel.isCompiling.collectAsStateWithLifecycle()
                val compilationMessage by viewModel.compilationMessage.collectAsStateWithLifecycle()

                val converterSelectedFile by viewModel.converterSelectedFile.collectAsStateWithLifecycle()
                val converterTargetFormat by viewModel.converterTargetFormat.collectAsStateWithLifecycle()
                val isConverting by viewModel.isConverting.collectAsStateWithLifecycle()
                val conversionResult by viewModel.conversionResult.collectAsStateWithLifecycle()

                val previewDoc by viewModel.previewDoc.collectAsStateWithLifecycle()
                val previewText by viewModel.previewText.collectAsStateWithLifecycle()
                val previewBitmap by viewModel.previewBitmap.collectAsStateWithLifecycle()
                val previewPdfPage by viewModel.previewPdfPage.collectAsStateWithLifecycle()
                val previewPdfTotalPages by viewModel.previewPdfTotalPages.collectAsStateWithLifecycle()

                var currentTab by remember { mutableStateOf(AppNavTab.EXPLORER) }

                // Handle Back Navigation cleanly
                BackHandler(enabled = previewDoc != null || currentTab != AppNavTab.EXPLORER || selectedProjectId != null) {
                    when {
                        previewDoc != null -> viewModel.closeDocumentPreview()
                        selectedProjectId != null -> viewModel.selectProject(null)
                        currentTab != AppNavTab.EXPLORER -> currentTab = AppNavTab.EXPLORER
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.systemBars,
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "DocMerge",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        letterSpacing = (-0.5).sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.OfflineBolt,
                                                contentDescription = null,
                                                tint = Color(0xFF059669),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "OFFLINE",
                                                color = Color(0xFF047857),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { showCloudDialog = true },
                                    modifier = Modifier.testTag("btn_cloud_sync_topbar")
                                ) {
                                    if (currentUser != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = (currentUser?.displayName?.take(1) ?: currentUser?.email?.take(1) ?: "G").uppercase(),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.CloudQueue,
                                            contentDescription = "Sincronización en la Nube",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 4.dp
                        ) {
                            AppNavTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    icon = {
                                        if (tab == AppNavTab.COMPILER && selectedDocIds.isNotEmpty()) {
                                            BadgedBox(
                                                badge = {
                                                    Badge {
                                                        Text(selectedDocIds.size.toString())
                                                    }
                                                }
                                            ) {
                                                Icon(imageVector = tab.icon, contentDescription = tab.title)
                                            }
                                        } else {
                                            Icon(imageVector = tab.icon, contentDescription = tab.title)
                                        }
                                    },
                                    modifier = Modifier.testTag(tab.testTag),
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            AppNavTab.PROJECTS -> {
                                ProjectsScreen(
                                    projects = projects,
                                    documents = documents,
                                    selectedProjectId = selectedProjectId,
                                    onSelectProject = { pId ->
                                        viewModel.selectProject(pId)
                                        currentTab = AppNavTab.EXPLORER
                                    },
                                    onCreateProject = { name, desc, color ->
                                        viewModel.createProject(name, desc, color)
                                    },
                                    onDeleteProject = { pId ->
                                        viewModel.deleteProject(pId)
                                    },
                                    onNavigateToExplorer = {
                                        viewModel.selectProject(null)
                                        currentTab = AppNavTab.EXPLORER
                                    }
                                )
                            }
                            AppNavTab.EXPLORER -> {
                                ExplorerScreen(
                                    documents = documents,
                                    projects = projects,
                                    selectedProjectId = selectedProjectId,
                                    searchQuery = searchQuery,
                                    formatFilter = formatFilter,
                                    selectedDocIds = selectedDocIds,
                                    isCompiling = isCompiling,
                                    onSelectProject = { pId -> viewModel.selectProject(pId) },
                                    onSearchChange = { q -> viewModel.setSearchQuery(q) },
                                    onFilterChange = { fmt -> viewModel.setFormatFilter(fmt) },
                                    onToggleSelect = { id -> viewModel.toggleDocumentSelection(id) },
                                    onSelectAll = { ids -> viewModel.selectAllDocuments(ids) },
                                    onClearSelection = { viewModel.clearCompilationSelection() },
                                    onImportFiles = { uris -> viewModel.importFiles(uris) },
                                    onCreateDocument = { title, content, format ->
                                        viewModel.createNewDocument(title, content, format)
                                    },
                                    onPreviewDocument = { doc -> viewModel.openDocumentPreview(doc) },
                                    onConvertDocument = { doc ->
                                        viewModel.setConverterSelectedFile(doc)
                                        currentTab = AppNavTab.CONVERTER
                                    },
                                    onDeleteDocument = { id -> viewModel.deleteDocument(id) },
                                    onCompileConfirm = { title, format, toc, sha, div ->
                                        viewModel.compileDocuments(title, format, toc, sha, div)
                                        currentTab = AppNavTab.COMPILER
                                    }
                                )
                            }
                            AppNavTab.COMPILER -> {
                                CompilerScreen(
                                    documents = documents,
                                    selectedDocIds = selectedDocIds,
                                    isCompiling = isCompiling,
                                    compilationMessage = compilationMessage,
                                    onToggleSelect = { id -> viewModel.toggleDocumentSelection(id) },
                                    onCompile = { title, format, toc, sha, div ->
                                        viewModel.compileDocuments(title, format, toc, sha, div)
                                    },
                                    onNavigateToExplorer = { currentTab = AppNavTab.EXPLORER },
                                    storageManager = viewModel.storageManager
                                )
                            }
                            AppNavTab.CONVERTER -> {
                                ConverterScreen(
                                    documents = documents,
                                    selectedFile = converterSelectedFile,
                                    targetFormat = converterTargetFormat,
                                    isConverting = isConverting,
                                    conversionResult = conversionResult,
                                    onSelectFile = { doc -> viewModel.setConverterSelectedFile(doc) },
                                    onSelectTargetFormat = { fmt -> viewModel.setConverterTargetFormat(fmt) },
                                    onConvert = { viewModel.runDirectConversion() },
                                    storageManager = viewModel.storageManager
                                )
                            }
                            AppNavTab.HISTORY -> {
                                HistoryScreen(
                                    compilations = compilations,
                                    onDeleteCompilation = { id -> viewModel.deleteCompilationRecord(id) },
                                    storageManager = viewModel.storageManager
                                )
                            }
                        }

                        // Inspection / Preview Dialog Modal
                        previewDoc?.let { doc ->
                            DocumentPreviewDialog(
                                document = doc,
                                textPreview = previewText,
                                imagePreview = previewBitmap,
                                pdfCurrentPage = previewPdfPage,
                                pdfTotalPages = previewPdfTotalPages,
                                onPdfPageChange = { page -> viewModel.loadPdfPage(page) },
                                onSaveEditedText = { newText ->
                                    viewModel.updateDocumentContent(doc, newText)
                                },
                                onConvertToFormat = {
                                    viewModel.setConverterSelectedFile(doc)
                                    viewModel.closeDocumentPreview()
                                    currentTab = AppNavTab.CONVERTER
                                },
                                onDismiss = { viewModel.closeDocumentPreview() },
                                storageManager = viewModel.storageManager
                            )
                        }

                        if (showCloudDialog) {
                            CloudSyncDialog(
                                currentUser = currentUser,
                                authManager = viewModel.authManager,
                                syncRepository = viewModel.syncRepository,
                                onSyncComplete = {
                                    viewModel.syncWithFirestore()
                                },
                                onDismiss = { showCloudDialog = false }
                            )
                        }
                    }
                }
            }
        }
    }
}
