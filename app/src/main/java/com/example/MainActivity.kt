package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File
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
                val bibleSearchQuery by viewModel.bibleSearchQuery.collectAsStateWithLifecycle()
                val recentGoals by viewModel.recentGoals.collectAsStateWithLifecycle()
                val chapterCounts by viewModel.chapterCounts.collectAsStateWithLifecycle()
                val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
                val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
                val saveStatus by viewModel.saveStatus.collectAsStateWithLifecycle()

                val importBackupLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) {
                        viewModel.importBackup(
                            uri = uri,
                            onResult = { summary ->
                                viewModel.showMessage(
                                    "Respaldo restaurado: ${summary.projects} novelas y ${summary.chapters} capítulos."
                                )
                            },
                            onError = { error -> viewModel.showMessage(error) }
                        )
                    }
                }

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
                                            saveStatus = saveStatus,
                                            canUndo = canUndo,
                                            canRedo = canRedo,
                                            onContentChange = { viewModel.updateChapterContent(it) },
                                            onSave = { viewModel.saveActiveChapter() },
                                            onUndo = { viewModel.undo() },
                                            onRedo = { viewModel.redo() },
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
                                            bibleSearchQuery = bibleSearchQuery,
                                            onBibleSearchQueryChange = { viewModel.setBibleSearchQuery(it) },
                                            onBack = { viewModel.selectStoryProject(null) },
                                            onSelectChapter = { chap -> viewModel.selectChapter(chap) },
                                            onCreateChapter = { title, summary, pov, targetWords, notes ->
                                                viewModel.createChapter(title, summary, pov, targetWords, notes)
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
                                            onMoveChapterUp = { chap -> viewModel.moveChapterUp(chap) },
                                            onMoveChapterDown = { chap -> viewModel.moveChapterDown(chap) },
                                            onDuplicateChapter = { chap -> viewModel.duplicateChapter(chap) },
                                            onUpdateChapterMetadata = { id, title, summary, pov, target, notes ->
                                                viewModel.updateChapterDetails(id, title, summary, pov, target, notes)
                                            },
                                            onCreateCharacter = { name, role, motivation, appearance, flaw ->
                                                viewModel.addCharacter(name, role, motivation, appearance, flaw)
                                            },
                                            onCreateSetting = { name, type, atmosphere, rules, secrets ->
                                                viewModel.addSetting(name, type, atmosphere, rules, secrets)
                                            },
                                            onCreateSubplot = { title, type, stakes, characters, resolution, status ->
                                                viewModel.addSubplot(title, type, stakes, characters, resolution, status)
                                            },
                                            onCycleSubplotStatus = { entry -> viewModel.cycleSubplotStatus(entry) },
                                            onUpdateBibleEntry = { entry -> viewModel.updateBibleEntry(entry) },
                                            onCreateBibleEntry = { cat, name, role, desc, notes ->
                                                viewModel.addBibleEntry(cat, name, role, desc, notes)
                                            },
                                            onDeleteBibleEntry = { id -> viewModel.deleteBibleEntry(id) },
                                            onCompileStory = { format, callback ->
                                                viewModel.compileStory(
                                                    format = format,
                                                    onResult = callback,
                                                    onError = { error -> viewModel.showMessage(error) }
                                                )
                                            },
                                            onShareFile = { file, mime -> shareFile(this@MainActivity, file, mime) }
                                        )
                                    }
                                    else -> {
                                        StoryProjectsScreen(
                                            stories = storyProjects,
                                            chapterCounts = chapterCounts,
                                            onSelectStory = { id -> viewModel.selectStoryProject(id) },
                                            onCreateStory = { title, genre, synopsis, words, color ->
                                                viewModel.createStoryProject(title, genre, synopsis, words, color)
                                            },
                                            onDeleteStory = { id -> viewModel.deleteStoryProject(id) },
                                            onExportBackup = {
                                                viewModel.exportBackup(
                                                    onResult = { file ->
                                                        shareFile(this@MainActivity, file, "application/json")
                                                    },
                                                    onError = { error -> viewModel.showMessage(error) }
                                                )
                                            },
                                            onImportBackup = {
                                                importBackupLauncher.launch(
                                                    arrayOf("application/json", "text/plain", "*/*")
                                                )
                                            }
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

    /**
     * Al salir de la app (o al minimizarla) se fuerza el guardado del capítulo
     * abierto. Antes, cerrar la app desde recientes podía perder lo último
     * escrito si el autoguardado aún no había disparado.
     */
    override fun onPause() {
        viewModel.flushActiveChapter()
        super.onPause()
    }

    override fun onStop() {
        viewModel.flushActiveChapter()
        super.onStop()
    }
}

/**
 * Comparte un archivo exportado usando FileProvider (el mismo patrón que ya
 * usaba el conversor de documentos). Sin esto, la novela exportada quedaba
 * atrapada en el almacenamiento privado de la app.
 */
private fun shareFile(context: Context, file: File, mimeType: String) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir ${file.name}"))
    } catch (e: Exception) {
        android.util.Log.e("MainActivity", "No se pudo compartir ${file.name}", e)
    }
}
