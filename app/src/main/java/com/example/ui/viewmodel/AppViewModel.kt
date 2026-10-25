package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CompilationRecordEntity
import com.example.data.model.DocumentItemEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.SupportedFormat
import com.example.data.repository.CompilationRepository
import com.example.data.repository.DocumentRepository
import com.example.data.repository.ProjectRepository
import com.example.engine.DocxProcessor
import com.example.engine.DocumentConverter
import com.example.engine.DocumentMerger
import com.example.engine.FileStorageManager
import com.example.engine.PdfExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val storageManager = FileStorageManager(application)
    private val projectRepo = ProjectRepository(db.projectDao(), db.documentItemDao(), storageManager)
    private val documentRepo = DocumentRepository(db.documentItemDao(), storageManager)
    private val compilationRepo = CompilationRepository(db.compilationRecordDao(), storageManager)
    private val documentMerger = DocumentMerger(application, storageManager)
    private val documentConverter = DocumentConverter(application, storageManager)
    private val docxProcessor = DocxProcessor()
    private val pdfExtractor = PdfExtractor()

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

    // Document Import & Creation
    fun importFiles(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val currentProjectId = _selectedProjectId.value ?: 0L

            for (uri in uris) {
                try {
                    val contentResolver = context.contentResolver
                    var displayName = "documento"
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex >= 0) {
                                displayName = cursor.getString(nameIndex) ?: displayName
                            }
                        }
                    }

                    contentResolver.openInputStream(uri)?.use { stream ->
                        documentRepo.importDocumentFromStream(
                            inputStream = stream,
                            originalFileName = displayName,
                            projectId = currentProjectId
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
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

    fun deleteCompilationRecord(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            compilationRepo.deleteCompilation(id)
        }
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
