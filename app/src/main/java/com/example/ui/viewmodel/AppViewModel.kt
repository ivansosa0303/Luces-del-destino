package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.backup.ImportSummary
import com.example.data.model.CompilationRecordEntity
import com.example.data.model.ChapterEntity
import com.example.data.model.DailyWritingGoalEntity
import com.example.data.model.DocumentItemEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.StoryBibleEntity
import com.example.data.model.SupportedFormat
import com.example.data.repository.CompilationRepository
import com.example.data.repository.DocumentRepository
import com.example.data.repository.ProjectRepository
import com.example.data.repository.StoryRepository
import com.example.engine.DocxProcessor
import com.example.engine.DocumentConverter
import com.example.engine.DocumentMerger
import com.example.engine.EditorialAnalysisResult
import com.example.engine.FileStorageManager
import com.example.engine.PdfExtractor
import com.example.engine.StoryEditorialEngine
import com.example.engine.StoryExporter
import com.example.engine.TextMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val prefs = application.getSharedPreferences("luces_session", android.content.Context.MODE_PRIVATE)
    val storageManager = FileStorageManager(application)
    private val projectRepo = ProjectRepository(db.projectDao(), db.documentItemDao(), storageManager)
    private val documentRepo = DocumentRepository(db.documentItemDao(), storageManager)
    private val compilationRepo = CompilationRepository(db.compilationRecordDao(), storageManager)
    val storyRepo = StoryRepository(db.projectDao(), db.chapterDao(), db.storyBibleDao(), db.dailyWritingGoalDao(), storageManager)
    private val documentMerger = DocumentMerger(application, storageManager)
    private val documentConverter = DocumentConverter(application, storageManager)
    private val docxProcessor = DocxProcessor()
    private val pdfExtractor = PdfExtractor()
    val uriFileResolverService: com.example.service.UriFileResolverService =
        com.example.service.DefaultUriFileResolverService(application, storageManager)

    val authManager = com.example.firebase.AuthManager(application)
    val syncRepository = com.example.firebase.FirestoreSyncRepository(application)
    val currentUser: StateFlow<com.google.firebase.auth.FirebaseUser?> = authManager.currentUser

    val projects: StateFlow<List<ProjectEntity>> = projectRepo.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDocuments: StateFlow<List<DocumentItemEntity>> = documentRepo.allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val compilations: StateFlow<List<CompilationRecordEntity>> = compilationRepo.allCompilations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selection & Filter State
    private val _selectedProjectId = MutableStateFlow<Long?>(null)
    val selectedProjectId: StateFlow<Long?> = _selectedProjectId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _formatFilter = MutableStateFlow<SupportedFormat?>(null)
    val formatFilter: StateFlow<SupportedFormat?> = _formatFilter.asStateFlow()

    private val _selectedDocIdsForCompilation = MutableStateFlow<Set<Long>>(emptySet())
    val selectedDocIdsForCompilation: StateFlow<Set<Long>> = _selectedDocIdsForCompilation.asStateFlow()

    // Compilation UI status
    private val _isCompiling = MutableStateFlow(false)
    val isCompiling: StateFlow<Boolean> = _isCompiling.asStateFlow()

    private val _compilationMessage = MutableStateFlow<String?>(null)
    val compilationMessage: StateFlow<String?> = _compilationMessage.asStateFlow()

    // Converter State
    private val _converterSelectedFile = MutableStateFlow<DocumentItemEntity?>(null)
    val converterSelectedFile: StateFlow<DocumentItemEntity?> = _converterSelectedFile.asStateFlow()

    private val _converterTargetFormat = MutableStateFlow(SupportedFormat.PDF)
    val converterTargetFormat: StateFlow<SupportedFormat> = _converterTargetFormat.asStateFlow()

    private val _isConverting = MutableStateFlow(false)
    val isConverting: StateFlow<Boolean> = _isConverting.asStateFlow()

    private val _conversionResult = MutableStateFlow<DocumentConverter.ConversionResult?>(null)
    val conversionResult: StateFlow<DocumentConverter.ConversionResult?> = _conversionResult.asStateFlow()

    // Document Preview & Inspection Modal
    private val _previewDoc = MutableStateFlow<DocumentItemEntity?>(null)
    val previewDoc: StateFlow<DocumentItemEntity?> = _previewDoc.asStateFlow()

    private val _previewText = MutableStateFlow<String?>(null)
    val previewText: StateFlow<String?> = _previewText.asStateFlow()

    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

    private val _previewPdfPage = MutableStateFlow(0)
    val previewPdfPage: StateFlow<Int> = _previewPdfPage.asStateFlow()

    private val _previewPdfTotalPages = MutableStateFlow(0)
    val previewPdfTotalPages: StateFlow<Int> = _previewPdfTotalPages.asStateFlow()

    init {
        seedSampleDataIfFirstRun()
        restoreLastSession()
        startPeriodicAutosave()
    }

    /**
     * Red de seguridad del autoguardado.
     *
     * El guardado por rebote (2,5 s) no dispara mientras el usuario escribe sin
     * pausar; este ciclo guarda cada 15 s si hay cambios pendientes, así una
     * sesión larga nunca queda a merced de un cierre inesperado.
     */
    private fun startPeriodicAutosave() {
        viewModelScope.launch {
            while (true) {
                delay(15_000)
                val chapter = _activeChapter.value ?: continue
                val content = _chapterContent.value
                if (content != chapter.content) {
                    persistChapter(chapter.id, content)
                }
            }
        }
    }

    private fun seedSampleDataIfFirstRun() {
        viewModelScope.launch(Dispatchers.IO) {
            // Check if projects exist
            val existing = db.projectDao().getAllProjects()
            // To check synchronously in IO
            val pCount = db.openHelper.readableDatabase.compileStatement("SELECT COUNT(*) FROM projects").simpleQueryForLong()
            if (pCount == 0L) {
                // Create Demo Project
                val pId = projectRepo.createProject(
                    name = "Manual & Reporte Alfa",
                    description = "Proyecto de demostración con documentos Markdown, JSON, DOCX y Texto.",
                    colorHex = "#2563EB"
                )

                // 1. Markdown document
                val mdContent = """# Manual de Especificaciones del Sistema
## Visión General
Este proyecto demuestra la capacidad de **DocMerge** para unificar múltiples formatos sin comprometer la integridad estructural ni los bytes originales.

### Características Principales:
- **Preservación Total**: Algoritmo de integridad con verificación SHA-256 local.
- **Sin Conexión**: 100% autónomo y privado en el dispositivo.
- **Formatos Soportados**: TXT, Markdown, JSON, PDF, DOCX e Imágenes.

> "La simplicidad y la fidelidad de datos son la máxima sofisticación."
"""
                documentRepo.createTextDocument("01_Manual_Sistema", mdContent, "md", pId)

                // 2. JSON document
                val jsonContent = """{
  "project": "DocMerge Engine",
  "version": "1.0.0",
  "offline_mode": true,
  "storage": "Local SQLite Room",
  "formats": ["txt", "md", "json", "pdf", "docx", "png", "jpg"],
  "security": {
    "checksum": "SHA-256",
    "cloud_sync": false,
    "user_privacy": "strict_device_isolated"
  }
}"""
                documentRepo.createTextDocument("02_Configuracion", jsonContent, "json", pId)

                // 3. Text Notes
                val txtContent = """REUNIÓN DE ARQUITECTURA Y REVISIÓN DE FORMATOS
Fecha: 2026-10-25
Participantes: Equipo de Desarrollo y Archivos

Puntos Tratados:
1. Verificación del compilador offline para generar PDFs y DOCX sin APIs externas.
2. Comprobación del cálculo de hash SHA-256 tras cada guardado.
3. Organización por proyectos con memoria permanente en el teléfono.
4. Exportación directa a descargas o compartición por aplicaciones locales.
"""
                documentRepo.createTextDocument("03_Notas_Reunion", txtContent, "txt", pId)

                // 4. Sample DOCX file
                val docxSections = listOf(
                    DocxProcessor.DocxSection(
                        title = "Resumen Ejecutivo de Integridad",
                        subtitle = "Generado con estructura OpenXML estándar",
                        content = "Este archivo demuestra la extracción y compilación nativa en formato Microsoft Word (.docx) directamente dentro de Android."
                    )
                )
                val docxDest = storageManager.createCompilationFile("04_Resumen_Ejecutivo.docx")
                docxProcessor.createDocxFile(docxDest, "Resumen Ejecutivo", docxSections)
                val docxBytes = docxDest.readBytes()
                val (savedDocx, sha256) = storageManager.saveBytesToDocument(docxBytes, "04_Resumen_Ejecutivo.docx")
                val docxEntity = DocumentItemEntity(
                    projectId = pId,
                    title = "04_Resumen_Ejecutivo",
                    fileName = "04_Resumen_Ejecutivo.docx",
                    extension = "docx",
                    mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    relativeFilePath = "documents/${savedDocx.name}",
                    fileSizeBytes = savedDocx.length(),
                    sha256Checksum = sha256,
                    textSummaryOrPreview = "Resumen Ejecutivo de Integridad... Generado con estructura OpenXML estándar"
                )
                db.documentItemDao().insertDocument(docxEntity)
            }
        }
    }

    // Projects actions
    fun createProject(name: String, description: String, colorHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            projectRepo.createProject(name, description, colorHex)
        }
    }

    fun selectProject(projectId: Long?) {
        _selectedProjectId.value = projectId
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            projectRepo.deleteProject(id)
            if (_selectedProjectId.value == id) {
                _selectedProjectId.value = null
            }
        }
    }

    // Filter & Search
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFormatFilter(format: SupportedFormat?) {
        _formatFilter.value = format
    }

    // Document Import & Creation via ContentResolver & UriFileResolverService
    fun importFiles(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentProjectId = _selectedProjectId.value ?: 0L
            val resolvedResults = uriFileResolverService.resolveUris(uris)
            for (result in resolvedResults) {
                if (result.isValid) {
                    try {
                        documentRepo.importFromIncomingResult(result, currentProjectId)
                    } catch (e: Exception) {
                        android.util.Log.e("AppViewModel", "Error al importar archivo resuelto: ${e.message}", e)
                    }
                }
            }
        }
    }

    fun importAndSelectForConversion(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isConverting.value = true
            try {
                val result = uriFileResolverService.resolveUri(uri)
                if (result.isValid) {
                    val currentProjectId = _selectedProjectId.value ?: 0L
                    val docId = documentRepo.importFromIncomingResult(result, currentProjectId)
                    val doc = documentRepo.getDocumentById(docId)
                    if (doc != null) {
                        withContext(Dispatchers.Main) {
                            setConverterSelectedFile(doc)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AppViewModel", "Error al resolver archivo para conversión: ${e.message}", e)
            } finally {
                _isConverting.value = false
            }
        }
    }

    fun handleIncomingIntent(intent: android.content.Intent) {
        viewModelScope.launch(Dispatchers.IO) {
            val results = uriFileResolverService.resolveIntent(intent)
            val currentProjectId = _selectedProjectId.value ?: 0L
            var firstImportedDoc: DocumentItemEntity? = null
            for (result in results) {
                if (result.isValid) {
                    val docId = documentRepo.importFromIncomingResult(result, currentProjectId)
                    if (firstImportedDoc == null) {
                        firstImportedDoc = documentRepo.getDocumentById(docId)
                    }
                }
            }
            if (firstImportedDoc != null) {
                withContext(Dispatchers.Main) {
                    setConverterSelectedFile(firstImportedDoc)
                }
            }
        }
    }

    fun createNewDocument(title: String, content: String, format: SupportedFormat) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentProjectId = _selectedProjectId.value ?: 0L
            documentRepo.createTextDocument(
                title = title,
                content = content,
                extension = format.extension,
                projectId = currentProjectId
            )
        }
    }

    fun deleteDocument(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            documentRepo.deleteDocument(id)
            _selectedDocIdsForCompilation.value = _selectedDocIdsForCompilation.value - id
        }
    }

    fun updateDocumentContent(item: DocumentItemEntity, newContent: String) {
        viewModelScope.launch(Dispatchers.IO) {
            documentRepo.updateDocumentText(item, newContent)
            // Refresh preview if open
            if (_previewDoc.value?.id == item.id) {
                _previewText.value = newContent
            }
        }
    }

    // Compilation selection
    fun toggleDocumentSelection(id: Long) {
        val current = _selectedDocIdsForCompilation.value
        _selectedDocIdsForCompilation.value = if (current.contains(id)) {
            current - id
        } else {
            current + id
        }
    }

    fun selectAllDocuments(ids: List<Long>) {
        _selectedDocIdsForCompilation.value = ids.toSet()
    }

    fun clearCompilationSelection() {
        _selectedDocIdsForCompilation.value = emptySet()
    }

    // Perform Compilation
    fun compileDocuments(
        title: String,
        targetFormat: SupportedFormat,
        includeTOC: Boolean = true,
        includeSha256: Boolean = true,
        includeDividers: Boolean = true
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isCompiling.value = true
            _compilationMessage.value = "Compilando archivos a ${targetFormat.displayName}..."

            try {
                val selectedIds = _selectedDocIdsForCompilation.value.toList()
                val docsToCompile = documentRepo.getDocumentsByIds(selectedIds)

                if (docsToCompile.isEmpty()) {
                    _compilationMessage.value = "Selecciona al menos un documento para unificar."
                    _isCompiling.value = false
                    return@launch
                }

                val options = DocumentMerger.MergeOptions(
                    targetFormat = targetFormat,
                    outputTitle = title.ifBlank { "Compilación_${targetFormat.displayName}" },
                    includeTableOfContents = includeTOC,
                    includeSha256Checksums = includeSha256,
                    includeSeparators = includeDividers
                )

                val result = documentMerger.compile(docsToCompile, options)

                if (result != null) {
                    val (destFile, sha256) = result
                    val currentProject = _selectedProjectId.value?.let { projectRepo.getProjectById(it) }

                    val recordId = compilationRepo.saveCompilationRecord(
                        projectId = currentProject?.id,
                        projectName = currentProject?.name ?: "Varios",
                        title = options.outputTitle,
                        outputFileName = destFile.name,
                        outputFormat = targetFormat.extension,
                        relativeFilePath = "compilations/${destFile.name}",
                        fileSizeBytes = destFile.length(),
                        sha256Checksum = sha256,
                        sourceItemCount = docsToCompile.size
                    )

                    _compilationMessage.value = "¡Compilación completada exitosamente! Guardado en: ${destFile.name}"
                } else {
                    _compilationMessage.value = "Ocurrió un error al compilar los documentos."
                }
            } catch (e: Exception) {
                _compilationMessage.value = "Error: ${e.message}"
            } finally {
                _isCompiling.value = false
            }
        }
    }

    // Direct Conversion
    fun setConverterSelectedFile(doc: DocumentItemEntity?) {
        _converterSelectedFile.value = doc
        _conversionResult.value = null
        if (doc != null) {
            val srcFormat = SupportedFormat.fromExtension(doc.extension)
            val available = documentConverter.getAvailableConversions(srcFormat)
            if (available.isNotEmpty() && !available.contains(_converterTargetFormat.value)) {
                _converterTargetFormat.value = available.first()
            }
        }
    }

    fun setConverterTargetFormat(format: SupportedFormat) {
        _converterTargetFormat.value = format
    }

    fun runDirectConversion() {
        val doc = _converterSelectedFile.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _isConverting.value = true
            try {
                val file = storageManager.getFileFromRelativePath(doc.relativeFilePath)
                val srcFormat = SupportedFormat.fromExtension(doc.extension)
                val targetFormat = _converterTargetFormat.value

                val result = documentConverter.convertFile(
                    sourceFile = file,
                    sourceFileName = doc.fileName,
                    sourceFormat = srcFormat,
                    targetFormat = targetFormat
                )
                _conversionResult.value = result

                if (result.success && result.outputFile != null) {
                    // Record in compilation history
                    compilationRepo.saveCompilationRecord(
                        projectId = if (doc.projectId != 0L) doc.projectId else null,
                        projectName = "Conversión Directa",
                        title = "${doc.title} a ${targetFormat.displayName}",
                        outputFileName = result.outputFile.name,
                        outputFormat = targetFormat.extension,
                        relativeFilePath = "compilations/${result.outputFile.name}",
                        fileSizeBytes = result.outputFile.length(),
                        sha256Checksum = result.sha256,
                        sourceItemCount = 1
                    )
                }
            } catch (e: Exception) {
                _conversionResult.value = DocumentConverter.ConversionResult(
                    success = false,
                    outputFile = null,
                    targetFormat = _converterTargetFormat.value,
                    message = "Error en conversión: ${e.message}"
                )
            } finally {
                _isConverting.value = false
            }
        }
    }

    // Preview Modal Logic
    fun openDocumentPreview(doc: DocumentItemEntity) {
        _previewDoc.value = doc
        _previewText.value = null
        _previewBitmap.value = null
        _previewPdfPage.value = 0

        viewModelScope.launch(Dispatchers.IO) {
            val file = storageManager.getFileFromRelativePath(doc.relativeFilePath)
            val format = SupportedFormat.fromExtension(doc.extension)

            when (format) {
                SupportedFormat.TXT, SupportedFormat.MD, SupportedFormat.JSON -> {
                    val text = storageManager.readTextSafely(file)
                    _previewText.value = text
                }
                SupportedFormat.DOCX -> {
                    val text = docxProcessor.extractTextFromDocx(file)
                    _previewText.value = text
                }
                SupportedFormat.PDF -> {
                    val count = pdfExtractor.getPageCount(file)
                    _previewPdfTotalPages.value = count
                    loadPdfPage(file, 0)
                }
                SupportedFormat.PNG, SupportedFormat.JPG -> {
                    val bmp = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                    _previewBitmap.value = bmp
                }
                SupportedFormat.ZIP -> {
                    _previewText.value = "Paquete ZIP: ${doc.fileName}\nTamaño: ${FileStorageManager.formatFileSize(doc.fileSizeBytes)}\nChecksum SHA-256: ${doc.sha256Checksum}"
                }
            }
        }
    }

    fun loadPdfPage(pageIndex: Int) {
        val doc = _previewDoc.value ?: return
        val file = storageManager.getFileFromRelativePath(doc.relativeFilePath)
        loadPdfPage(file, pageIndex)
    }

    private fun loadPdfPage(file: File, pageIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val bmp = pdfExtractor.renderPageToBitmap(file, pageIndex, 1080)
            _previewBitmap.value = bmp
            _previewPdfPage.value = pageIndex
        }
    }

    fun closeDocumentPreview() {
        _previewDoc.value = null
        _previewText.value = null
        _previewBitmap.value?.recycle()
        _previewBitmap.value = null
    }

    // --- STORY & EDITORIAL SUITE STATE ---
    val storyProjects: StateFlow<List<ProjectEntity>> = storyRepo.getStoryProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chapterCounts: StateFlow<Map<Long, Int>> = storyRepo.getChapterCounts()
        .map { counts -> counts.associate { it.projectId to it.chapterCount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _activeStoryProjectId = MutableStateFlow<Long?>(null)
    val activeStoryProjectId: StateFlow<Long?> = _activeStoryProjectId.asStateFlow()

    val activeStoryProject: StateFlow<ProjectEntity?> = combine(storyProjects, _activeStoryProjectId) { list, id ->
        list.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _chapterSearchQuery = MutableStateFlow("")
    val chapterSearchQuery: StateFlow<String> = _chapterSearchQuery.asStateFlow()

    private val _activeStoryChapters = MutableStateFlow<List<ChapterEntity>>(emptyList())
    val activeStoryChapters: StateFlow<List<ChapterEntity>> = _activeStoryChapters.asStateFlow()

    val filteredChapters: StateFlow<List<ChapterEntity>> = combine(_activeStoryChapters, _chapterSearchQuery) { chaps, query ->
        if (query.isBlank()) chaps
        else chaps.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.content.contains(query, ignoreCase = true) ||
            it.summary.contains(query, ignoreCase = true) ||
            it.povCharacter.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeChapter = MutableStateFlow<ChapterEntity?>(null)
    val activeChapter: StateFlow<ChapterEntity?> = _activeChapter.asStateFlow()

    private val _chapterContent = MutableStateFlow("")
    val chapterContent: StateFlow<String> = _chapterContent.asStateFlow()

    private val _editorialAnalysis = MutableStateFlow(StoryEditorialEngine.analyze(""))
    val editorialAnalysis: StateFlow<EditorialAnalysisResult> = _editorialAnalysis.asStateFlow()

    // --- Autoguardado ---
    private var autosaveJob: Job? = null
    private var analysisJob: Job? = null
    private val saveMutex = Mutex()

    private val _saveStatus = MutableStateFlow<String?>(null)
    val saveStatus: StateFlow<String?> = _saveStatus.asStateFlow()

    // --- Deshacer / rehacer ---
    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()
    private var lastUndoSnapshotAt = 0L

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Colectores de la novela activa: se cancelan al cambiar de novela para no
    // acumular observadores duplicados en cada navegación.
    private var chaptersJob: Job? = null
    private var bibleJob: Job? = null
    private var goalsJob: Job? = null

    private val _storyBibleEntries = MutableStateFlow<List<StoryBibleEntity>>(emptyList())
    val storyBibleEntries: StateFlow<List<StoryBibleEntity>> = _storyBibleEntries.asStateFlow()

    private val _bibleSearchQuery = MutableStateFlow("")
    val bibleSearchQuery: StateFlow<String> = _bibleSearchQuery.asStateFlow()

    val filteredBibleEntries: StateFlow<List<StoryBibleEntity>> = combine(_storyBibleEntries, _bibleSearchQuery) { entries, q ->
        if (q.isBlank()) entries
        else entries.filter {
            it.name.contains(q, ignoreCase = true) ||
            it.description.contains(q, ignoreCase = true) ||
            it.roleOrLocation.contains(q, ignoreCase = true) ||
            it.notes.contains(q, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _recentGoals = MutableStateFlow<List<DailyWritingGoalEntity>>(emptyList())
    val recentGoals: StateFlow<List<DailyWritingGoalEntity>> = _recentGoals.asStateFlow()

    fun selectStoryProject(projectId: Long?) {
        _activeStoryProjectId.value = projectId
        _activeChapter.value = null
        _chapterContent.value = ""
        _chapterSearchQuery.value = ""
        _bibleSearchQuery.value = ""
        if (projectId != null) {
            collectStoryData(projectId)
        } else {
            chaptersJob?.cancel()
            bibleJob?.cancel()
            goalsJob?.cancel()
            _activeStoryChapters.value = emptyList()
            _storyBibleEntries.value = emptyList()
            _recentGoals.value = emptyList()
        }
    }

    private fun collectStoryData(projectId: Long) {
        chaptersJob?.cancel()
        bibleJob?.cancel()
        goalsJob?.cancel()
        chaptersJob = viewModelScope.launch {
            storyRepo.getChapters(projectId).collect { _activeStoryChapters.value = it }
        }
        bibleJob = viewModelScope.launch {
            storyRepo.getBibleEntries(projectId).collect { _storyBibleEntries.value = it }
        }
        goalsJob = viewModelScope.launch {
            storyRepo.getRecentGoals(projectId).collect { _recentGoals.value = it }
        }
    }

    private fun rememberLastSession(projectId: Long, chapterId: Long) {
        prefs.edit()
            .putLong("last_project", projectId)
            .putLong("last_chapter", chapterId)
            .apply()
    }

    /**
     * Reabre el último capítulo en el que estaba trabajando el usuario.
     * Antes, cerrar la app devolvía siempre a la lista de novelas.
     */
    private fun restoreLastSession() {
        viewModelScope.launch(Dispatchers.IO) {
            if (_activeStoryProjectId.value != null) return@launch
            val projectId = prefs.getLong("last_project", 0L)
            val chapterId = prefs.getLong("last_chapter", 0L)
            if (projectId == 0L || chapterId == 0L) return@launch
            val project = storyRepo.getProjectById(projectId) ?: return@launch
            val chapter = storyRepo.getChapterById(chapterId) ?: return@launch
            withContext(Dispatchers.Main) {
                _activeStoryProjectId.value = project.id
                collectStoryData(project.id)
                selectChapter(chapter)
            }
        }
    }

    fun setChapterSearchQuery(query: String) {
        _chapterSearchQuery.value = query
    }

    fun setBibleSearchQuery(query: String) {
        _bibleSearchQuery.value = query
    }

    fun createStoryProject(title: String, genre: String, synopsis: String, targetWords: Int, colorHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = storyRepo.createStoryProject(title, genre, synopsis, targetWords, colorHex)
            selectStoryProject(id)
        }
    }

    fun updateStoryProject(project: ProjectEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            storyRepo.updateProject(project)
        }
    }

    fun deleteStoryProject(projectId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            storyRepo.deleteStoryProject(projectId)
            if (_activeStoryProjectId.value == projectId) {
                selectStoryProject(null)
            }
        }
    }

    fun selectChapter(chapter: ChapterEntity?) {
        // Red de seguridad: si se cambia de capítulo con texto pendiente de
        // autoguardado, se guarda antes de reemplazar el contenido en memoria.
        if (chapter != null && chapter.id != _activeChapter.value?.id) {
            flushActiveChapter()
        }
        _activeChapter.value = chapter
        _chapterContent.value = chapter?.content ?: ""
        // El análisis de un capítulo largo puede tardar; se calcula fuera del
        // hilo principal en vez de bloquear la apertura del editor.
        _editorialAnalysis.value = StoryEditorialEngine.analyze("")
        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
        _saveStatus.value = null
        if (chapter != null) {
            rememberLastSession(chapter.projectId, chapter.id)
            scheduleAnalysis(chapter.content)
        }
    }

    fun updateChapterContent(newContent: String) {
        val previous = _chapterContent.value
        if (previous == newContent) return
        // Cualquier edición invalida la pila de rehacer, incluso si el
        // snapshot se agrupa por la ventana de 600 ms.
        redoStack.clear()
        _canRedo.value = false
        pushUndoSnapshot(previous)
        _chapterContent.value = newContent
        _saveStatus.value = "Cambios sin guardar"
        scheduleAnalysis(newContent)
        scheduleAutosave()
    }

    // --- Deshacer / rehacer ---

    private fun pushUndoSnapshot(previous: String) {
        val now = System.currentTimeMillis()
        if (now - lastUndoSnapshotAt < 600L) return
        lastUndoSnapshotAt = now
        undoStack.addLast(previous)
        while (undoStack.size > 60) undoStack.removeFirst()
        _canUndo.value = true
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val current = _chapterContent.value
        val restored = undoStack.removeLast()
        redoStack.addLast(current)
        _chapterContent.value = restored
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = true
        _saveStatus.value = "Cambios sin guardar"
        scheduleAnalysis(restored)
        scheduleAutosave()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = _chapterContent.value
        val restored = redoStack.removeLast()
        undoStack.addLast(current)
        _chapterContent.value = restored
        _canRedo.value = redoStack.isNotEmpty()
        _canUndo.value = true
        _saveStatus.value = "Cambios sin guardar"
        scheduleAnalysis(restored)
        scheduleAutosave()
    }

    // --- Análisis editorial (fuera del hilo principal y con retardo) ---

    private fun scheduleAnalysis(text: String) {
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            delay(450)
            val result = withContext(Dispatchers.Default) { StoryEditorialEngine.analyze(text) }
            _editorialAnalysis.value = result
        }
    }

    // --- Autoguardado ---

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        val chapterId = _activeChapter.value?.id ?: return
        val content = _chapterContent.value
        autosaveJob = viewModelScope.launch {
            delay(2500)
            persistChapter(chapterId, content)
        }
    }

    fun saveActiveChapter() {
        autosaveJob?.cancel()
        val chapterId = _activeChapter.value?.id ?: return
        // El contenido se captura AQUÍ, de forma síncrona. Antes se leía dentro
        // de la corrutina, y al navegar fuera del editor podía guardar "".
        val content = _chapterContent.value
        viewModelScope.launch { persistChapter(chapterId, content) }
    }

    /** Guardado inmediato al salir a segundo plano o al navegar fuera del editor. */
    fun flushActiveChapter() {
        autosaveJob?.cancel()
        val chapterId = _activeChapter.value?.id ?: return
        val content = _chapterContent.value
        viewModelScope.launch(Dispatchers.IO) { persistChapter(chapterId, content) }
    }

    private suspend fun persistChapter(chapterId: Long, content: String) {
        saveMutex.withLock {
            _saveStatus.value = "Guardando…"
            val updated = storyRepo.saveChapterContent(chapterId, content)
            if (updated != null) {
                if (_activeChapter.value?.id == updated.id) {
                    _activeChapter.value = updated
                }
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                _saveStatus.value = "Guardado a las $time"
            } else {
                _saveStatus.value = "No se pudo guardar"
            }
        }
    }

    fun createChapter(
        title: String,
        summary: String = "",
        pov: String = "",
        targetWords: Int = 2000,
        notes: String = ""
    ) {
        val projId = _activeStoryProjectId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val id = storyRepo.createChapter(projId, title, "", summary, pov, targetWords, notes)
            val newChap = storyRepo.getChapterById(id)
            if (newChap != null) {
                selectChapter(newChap)
            }
        }
    }

    fun updateChapterMetadata(chapter: ChapterEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            storyRepo.updateChapterStatus(chapter.id, chapter.status)
            if (_activeChapter.value?.id == chapter.id) {
                _activeChapter.value = _activeChapter.value?.copy(status = chapter.status)
            }
        }
    }

    fun updateChapterDetails(
        chapterId: Long,
        title: String,
        summary: String,
        povCharacter: String,
        targetWords: Int,
        notes: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            storyRepo.updateChapterMetadata(chapterId, title, summary, povCharacter, targetWords, notes)
            val updated = storyRepo.getChapterById(chapterId)
            if (_activeChapter.value?.id == chapterId && updated != null) {
                _activeChapter.value = updated
            }
        }
    }

    fun moveChapterUp(chapter: ChapterEntity) {
        val projId = _activeStoryProjectId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            storyRepo.moveChapterUp(projId, chapter.id)
        }
    }

    fun moveChapterDown(chapter: ChapterEntity) {
        val projId = _activeStoryProjectId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            storyRepo.moveChapterDown(projId, chapter.id)
        }
    }

    fun duplicateChapter(chapter: ChapterEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            storyRepo.duplicateChapter(chapter.id)
        }
    }

    fun cycleChapterStatus(chapter: ChapterEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = storyRepo.cycleChapterStatus(chapter)
            if (_activeChapter.value?.id == chapter.id) {
                _activeChapter.value = updated
            }
        }
    }

    fun deleteChapter(chapterId: Long) {
        val projId = _activeStoryProjectId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (_activeChapter.value?.id == chapterId) {
                // Se limpia antes de borrar para que ningún guardado pendiente
                // intente reescribir un capítulo que ya no existe.
                _activeChapter.value = null
                _chapterContent.value = ""
                _saveStatus.value = null
                undoStack.clear()
                redoStack.clear()
                _canUndo.value = false
                _canRedo.value = false
            }
            storyRepo.deleteChapter(chapterId, projId)
        }
    }

    // Editorial Suite Quick Fixes
    fun applyDialogueDashFix() {
        val current = _chapterContent.value
        val fixed = StoryEditorialEngine.fixDialogueDashes(current)
        updateChapterContent(fixed)
    }

    fun applyFormattingWhitespaceFix() {
        val current = _chapterContent.value
        val fixed = StoryEditorialEngine.cleanFormattingWhitespace(current)
        updateChapterContent(fixed)
    }

    fun applyOpeningPunctuationFix() {
        val current = _chapterContent.value
        val fixed = StoryEditorialEngine.fixOpeningPunctuation(current)
        updateChapterContent(fixed)
    }

    // Story Bible (Characters, Places, Subplots)
    fun addCharacter(
        name: String,
        role: String,
        motivation: String,
        appearance: String,
        flawAndArc: String,
        colorHex: String = "#8B5CF6"
    ) {
        val projId = _activeStoryProjectId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            storyRepo.insertBibleEntry(
                StoryBibleEntity(
                    projectId = projId,
                    category = "PERSONAJE",
                    name = name.trim(),
                    roleOrLocation = role.trim(),
                    description = motivation.trim(),
                    appearanceOrDetails = appearance.trim(),
                    notes = flawAndArc.trim(),
                    status = "ACTIVO",
                    colorHex = colorHex
                )
            )
        }
    }

    fun addSetting(
        name: String,
        locationType: String,
        atmosphere: String,
        worldRules: String,
        secrets: String,
        colorHex: String = "#06B6D4"
    ) {
        val projId = _activeStoryProjectId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            storyRepo.insertBibleEntry(
                StoryBibleEntity(
                    projectId = projId,
                    category = "ESCENARIO",
                    name = name.trim(),
                    roleOrLocation = locationType.trim(),
                    description = atmosphere.trim(),
                    appearanceOrDetails = worldRules.trim(),
                    notes = secrets.trim(),
                    status = "ACTIVO",
                    colorHex = colorHex
                )
            )
        }
    }

    fun addSubplot(
        title: String,
        subplotType: String,
        conflictStakes: String,
        charactersInvolved: String,
        resolutionNotes: String,
        status: String = "PLANTEADA",
        colorHex: String = "#F59E0B"
    ) {
        val projId = _activeStoryProjectId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            storyRepo.insertBibleEntry(
                StoryBibleEntity(
                    projectId = projId,
                    category = "TRAMA_SECUNDARIA",
                    name = title.trim(),
                    roleOrLocation = subplotType.trim(),
                    description = conflictStakes.trim(),
                    appearanceOrDetails = charactersInvolved.trim(),
                    notes = resolutionNotes.trim(),
                    status = status,
                    colorHex = colorHex
                )
            )
        }
    }

    fun addBibleEntry(
        category: String,
        name: String,
        role: String,
        desc: String,
        notes: String,
        details: String = "",
        status: String = "EN_DESARROLLO",
        colorHex: String = "#10B981"
    ) {
        val projId = _activeStoryProjectId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            storyRepo.insertBibleEntry(
                StoryBibleEntity(
                    projectId = projId,
                    category = category,
                    name = name,
                    roleOrLocation = role,
                    description = desc,
                    appearanceOrDetails = details,
                    notes = notes,
                    status = status,
                    colorHex = colorHex
                )
            )
        }
    }

    fun cycleSubplotStatus(entry: StoryBibleEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            storyRepo.cycleSubplotStatus(entry)
        }
    }

    fun updateBibleEntry(entry: StoryBibleEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            storyRepo.updateBibleEntry(entry)
        }
    }

    fun deleteBibleEntry(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            storyRepo.deleteBibleEntry(id)
        }
    }

    // Export / Compile Story
    fun compileStory(
        format: SupportedFormat,
        onResult: (File) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val proj = activeStoryProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // Nunca exportar con cambios del editor sin guardar.
            _activeChapter.value?.let { persistChapter(it.id, _chapterContent.value) }
            val chapters = _activeStoryChapters.value

            val ext = when (format) {
                SupportedFormat.MD -> "md"
                SupportedFormat.JSON -> "json"
                SupportedFormat.PDF -> "pdf"
                SupportedFormat.DOCX -> "docx"
                else -> "txt"
            }
            val safeName = proj.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val file = storageManager.createCompilationFile("historia_${safeName}_completa.$ext")

            val ok = when (format) {
                SupportedFormat.PDF -> StoryExporter().exportPdf(file, proj, chapters)
                SupportedFormat.DOCX -> StoryExporter().exportDocx(file, proj, chapters)
                else -> {
                    val text = storyRepo.buildBookManuscriptText(proj, chapters, format)
                    file.writeText(text, Charsets.UTF_8)
                    true
                }
            }

            if (!ok) {
                withContext(Dispatchers.Main) {
                    onError("No se pudo generar el archivo ${format.displayName}.")
                }
                return@launch
            }

            val relative = "compilations/${file.name}"
            val checksum = storageManager.calculateSha256(file)

            compilationRepo.saveCompilationRecord(
                projectId = proj.id,
                projectName = proj.name,
                title = "Novela Completa: ${proj.name}",
                outputFileName = file.name,
                outputFormat = ext,
                relativeFilePath = relative,
                fileSizeBytes = file.length(),
                sha256Checksum = checksum,
                sourceItemCount = chapters.size
            )

            withContext(Dispatchers.Main) {
                onResult(file)
            }
        }
    }

    // --- Copia de seguridad completa ---

    fun exportBackup(onResult: (File) -> Unit, onError: (String) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _activeChapter.value?.let { persistChapter(it.id, _chapterContent.value) }
                val json = storyRepo.exportBackupJson()
                val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                val file = storageManager.createCompilationFile("respaldo_novelas_$stamp.json")
                file.writeText(json, Charsets.UTF_8)
                withContext(Dispatchers.Main) { onResult(file) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "No se pudo crear el respaldo.")
                }
            }
        }
    }

    fun importBackup(
        uri: android.net.Uri,
        onResult: (ImportSummary) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val text = getApplication<Application>().contentResolver
                    .openInputStream(uri)
                    ?.use { it.readBytes().toString(Charsets.UTF_8) }
                    ?: throw IllegalArgumentException("No se pudo leer el archivo seleccionado.")

                val summary = storyRepo.importBackupJson(text)
                withContext(Dispatchers.Main) { onResult(summary) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "El archivo no es un respaldo válido.")
                }
            }
        }
    }

    fun deleteCompilationRecord(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            compilationRepo.deleteCompilation(id)
        }
    }

    /** Mensaje puntual para la snackbar global. */
    fun showMessage(message: String) {
        _compilationMessage.value = message
    }

    suspend fun syncWithFirestore(): Int {
        return withContext(Dispatchers.IO) {
            syncRepository.syncAllToCloud(
                projects = projects.value,
                documents = allDocuments.value,
                compilations = compilations.value
            )
        }
    }
}
