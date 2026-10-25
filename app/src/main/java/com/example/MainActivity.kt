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
import androidx.compose.material.icons.filled.AutoStories
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
import com.example.ui.screens.ChapterEditorScreen
import com.example.ui.screens.CompilerScreen
import com.example.ui.screens.ConverterScreen
import com.example.ui.screens.ExplorerScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.StoryProjectDetailScreen
import com.example.ui.screens.StoryProjectsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppViewModel

enum class AppNavTab(
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    STORIES("Historias", Icons.Default.AutoStories, "tab_stories"),
    PROJECTS("Carpetas", Icons.Default.Folder, "tab_projects"),
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

                val storyProjects by viewModel.storyProjects.collectAsStateWithLifecycle()
                val activeStoryProject by viewModel.activeStoryProject.collectAsStateWithLifecycle()
                val activeStoryChapters by viewModel.activeStoryChapters.collectAsStateWithLifecycle()
                val filteredChapters by viewModel.filteredChapters.collectAsStateWithLifecycle()
                val activeChapter by viewModel.activeChapter.collectAsStateWithLifecycle()
                val chapterContent by viewModel.chapterContent.collectAsStateWithLifecycle()
                val editorialAnalysis by viewModel.editorialAnalysis.collectAsStateWithLifecycle()
                val storyBibleEntries by viewModel.storyBibleEntries.collectAsStateWithLifecycle()
                val filteredBibleEntries by viewModel.filteredBibleEntries.collectAsStateWithLifecycle()
                val chapterSearchQuery by viewModel.chapterSearchQuery.collectAsStateWithLifecycle()
                val recentGoals by viewModel.recentGoals.collectAsStateWithLifecycle()

                var currentTab by remember { mutableStateOf(AppNavTab.STORIES) }

                // Handle Back Navigation cleanly
                BackHandler(enabled = activeChapter != null || activeStoryProject != null || previewDoc != null || currentTab != AppNavTab.STORIES || selectedProjectId != null) {
                    when {
                        activeChapter != null -> {
                            viewModel.saveActiveChapter()
                            viewModel.selectChapter(null)
                        }
                        activeStoryProject != null -> viewModel.selectStoryProject(null)
                        previewDoc != null -> viewModel.closeDocumentPreview()
                        selectedProjectId != null -> viewModel.selectProject(null)
                        currentTab != AppNavTab.STORIES -> currentTab = AppNavTab.STORIES
                    }
                }

                val snackbarHostState = remember { SnackbarHostState() }
                val selectedProjectName = when {
                    activeChapter != null -> "Cap. ${activeChapter?.chapterNumber}: ${activeChapter?.title}"
                    activeStoryProject != null -> activeStoryProject?.name
                    selectedProjectId != null -> projects.firstOrNull { it.id == selectedProjectId }?.name
                    else -> null
                }

                LaunchedEffect(compilationMessage) {
                    compilationMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                    }
                }

                val canNavigateBack = activeChapter != null || activeStoryProject != null || previewDoc != null || selectedProjectId != null || currentTab != AppNavTab.STORIES
                val onNavigateBackAction: (() -> Unit)? = if (canNavigateBack) {
                    {
                        when {
                            activeChapter != null -> {
                                viewModel.saveActiveChapter()
                                viewModel.selectChapter(null)
                            }
                            activeStoryProject != null -> viewModel.selectStoryProject(null)
                            previewDoc != null -> viewModel.closeDocumentPreview()
                            selectedProjectId != null -> viewModel.selectProject(null)
                            currentTab != AppNavTab.STORIES -> currentTab = AppNavTab.STORIES
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
                            AppNavTab.STORIES -> {
                                when {
                                    activeChapter != null -> {
                                        ChapterEditorScreen(
                                            chapter = activeChapter!!,
                                            content = chapterContent,
                                            editorialAnalysis = editorialAnalysis,
                                            bibleEntries = storyBibleEntries,
                                            onContentChange = { viewModel.updateChapterContent(it) },
                                            onSave = { viewModel.saveActiveChapter() },
                                            onBack = {
                                                viewModel.saveActiveChapter()
                                                viewModel.selectChapter(null)
                                            },
                                            onToggleStatus = { chap ->
                                                val nextStatus = when (chap.status) {
                                                    "BORRADOR" -> "EN_REVISION"
                                                    "EN_REVISION" -> "PULIDO"
                                                    else -> "BORRADOR"
                                                }
                                                viewModel.updateChapterMetadata(chap.copy(status = nextStatus))
                                            },
                                            onApplyDialogueDashFix = { viewModel.applyDialogueDashFix() },
                                            onApplyWhitespaceFix = { viewModel.applyFormattingWhitespaceFix() },
                                            onApplyOpeningPunctuationFix = { viewModel.applyOpeningPunctuationFix() }
                                        )
                                    }
                                    activeStoryProject != null -> {
                                        StoryProjectDetailScreen(
                                            project = activeStoryProject!!,
                                            chapters = filteredChapters,
                                            bibleEntries = filteredBibleEntries,
                                            recentGoals = recentGoals,
                                            searchQuery = chapterSearchQuery,
                                            onSearchQueryChange = { viewModel.setChapterSearchQuery(it) },
                                            onBack = { viewModel.selectStoryProject(null) },
                                            onSelectChapter = { chap -> viewModel.selectChapter(chap) },
                                            onCreateChapter = { title, summary, pov ->
                                                viewModel.createChapter(title, summary, pov)
                                            },
                                            onDeleteChapter = { id -> viewModel.deleteChapter(id) },
                                            onToggleChapterStatus = { chap ->
                                                val nextStatus = when (chap.status) {
                                                    "BORRADOR" -> "EN_REVISION"
                                                    "EN_REVISION" -> "PULIDO"
                                                    else -> "BORRADOR"
                                                }
                                                viewModel.updateChapterMetadata(chap.copy(status = nextStatus))
                                            },
                                            onCreateBibleEntry = { cat, name, role, desc, notes ->
                                                viewModel.addBibleEntry(cat, name, role, desc, notes)
                                            },
                                            onDeleteBibleEntry = { id -> viewModel.deleteBibleEntry(id) },
                                            onCompileStory = { format, callback ->
                                                viewModel.compileStory(format, callback)
                                            }
                                        )
                                    }
                                    else -> {
                                        StoryProjectsScreen(
                                            stories = storyProjects,
                                            onSelectStory = { id -> viewModel.selectStoryProject(id) },
                                            onCreateStory = { title, genre, synopsis, words, color ->
                                                viewModel.createStoryProject(title, genre, synopsis, words, color)
                                            },
                                            onDeleteStory = { id -> viewModel.deleteStoryProject(id) }
                                        )
                                    }
                                }
                            }
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
