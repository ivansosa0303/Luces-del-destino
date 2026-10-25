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
import com.example.ui.components.AppBaseScaffold
import com.example.ui.components.CloudSyncDialog
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
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

        intent?.let { incomingIntent ->
            if (incomingIntent.action in listOf(
                    android.content.Intent.ACTION_SEND,
                    android.content.Intent.ACTION_SEND_MULTIPLE,
                    android.content.Intent.ACTION_VIEW
                )
            ) {
                viewModel.handleIncomingIntent(incomingIntent)
            }
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

                val snackbarHostState = remember { SnackbarHostState() }
                val selectedProjectName = projects.firstOrNull { it.id == selectedProjectId }?.name

                LaunchedEffect(compilationMessage) {
                    compilationMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                    }
                }

                val canNavigateBack = previewDoc != null || selectedProjectId != null || currentTab != AppNavTab.EXPLORER
                val onNavigateBackAction: (() -> Unit)? = if (canNavigateBack) {
                    {
                        when {
                            previewDoc != null -> viewModel.closeDocumentPreview()
                            selectedProjectId != null -> viewModel.selectProject(null)
                            currentTab != AppNavTab.EXPLORER -> currentTab = AppNavTab.EXPLORER
                        }
                    }
                } else null

                AppBaseScaffold(
                    currentTab = currentTab,
                    onTabSelected = { tab -> currentTab = tab },
                    selectedProjectName = selectedProjectName,
                    onNavigateBack = onNavigateBackAction,
                    currentUser = currentUser,
                    onOpenCloudSync = { showCloudDialog = true },
                    compilerBadgeCount = selectedDocIds.size,
                    snackbarHostState = snackbarHostState
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
                                    onSelectFileUri = { uri -> viewModel.importAndSelectForConversion(uri) },
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

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.handleIncomingIntent(intent)
    }
}
