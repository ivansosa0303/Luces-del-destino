package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocumentItemEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.SupportedFormat
import com.example.ui.components.CompileOptionsDialog
import com.example.ui.components.CreateDocumentDialog
import com.example.ui.components.DocumentItemCard

@Composable
fun ExplorerScreen(
    documents: List<DocumentItemEntity>,
    projects: List<ProjectEntity>,
    selectedProjectId: Long?,
    searchQuery: String,
    formatFilter: SupportedFormat?,
    selectedDocIds: Set<Long>,
    isCompiling: Boolean,
    onSelectProject: (Long?) -> Unit,
    onSearchChange: (String) -> Unit,
    onFilterChange: (SupportedFormat?) -> Unit,
    onToggleSelect: (Long) -> Unit,
    onSelectAll: (List<Long>) -> Unit,
    onClearSelection: () -> Unit,
    onImportFiles: (List<Uri>) -> Unit,
    onCreateDocument: (title: String, content: String, format: SupportedFormat) -> Unit,
    onPreviewDocument: (DocumentItemEntity) -> Unit,
    onConvertDocument: (DocumentItemEntity) -> Unit,
    onDeleteDocument: (Long) -> Unit,
    onCompileConfirm: (title: String, format: SupportedFormat, toc: Boolean, sha: Boolean, div: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDocDialog by remember { mutableStateOf(false) }
    var showCompileDialog by remember { mutableStateOf(false) }

    // SAF File Picker
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportFiles(uris)
        }
    }

    val currentProject = projects.firstOrNull { it.id == selectedProjectId }

    // Filter documents
    val filteredDocs = documents.filter { doc ->
        val matchesProject = selectedProjectId == null || doc.projectId == selectedProjectId
        val matchesSearch = searchQuery.isBlank() ||
                doc.title.contains(searchQuery, ignoreCase = true) ||
                doc.fileName.contains(searchQuery, ignoreCase = true)
        val matchesFormat = formatFilter == null || doc.extension.equals(formatFilter.extension, ignoreCase = true)
        matchesProject && matchesSearch && matchesFormat
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Project Context Header
            if (currentProject != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Proyecto: ${currentProject.name}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        OutlinedButton(
                            onClick = { onSelectProject(null) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Ver Todos", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Search Bar & Import Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_explorer_search"),
                    placeholder = { Text("Buscar por nombre o contenido...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons Row: Import SAF, New Doc, Select All
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            openDocumentLauncher.launch(
                                arrayOf(
                                    "*/*",
                                    "text/plain",
                                    "text/markdown",
                                    "application/json",
                                    "application/pdf",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "image/*"
                                )
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_import_files")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Importar")
                    }

                    OutlinedButton(
                        onClick = { showCreateDocDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_create_new_doc")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Nuevo")
                    }

                    if (filteredDocs.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                if (selectedDocIds.size == filteredDocs.size) {
                                    onClearSelection()
                                } else {
                                    onSelectAll(filteredDocs.map { it.id })
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(
                                if (selectedDocIds.size == filteredDocs.size) "Deseleccionar" else "Todos",
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Format Filter Chips Scrollable Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = formatFilter == null,
                        onClick = { onFilterChange(null) },
                        label = { Text("Todos (${documents.size})") }
                    )

                    val formats = listOf(
                        SupportedFormat.TXT,
                        SupportedFormat.MD,
                        SupportedFormat.JSON,
                        SupportedFormat.PDF,
                        SupportedFormat.DOCX,
                        SupportedFormat.PNG,
                        SupportedFormat.JPG
                    )

                    formats.forEach { fmt ->
                        val count = documents.count { it.extension.equals(fmt.extension, ignoreCase = true) }
                        FilterChip(
                            selected = formatFilter == fmt,
                            onClick = { onFilterChange(if (formatFilter == fmt) null else fmt) },
                            label = { Text("${fmt.extension.uppercase()} ($count)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = fmt.badgeColor.copy(alpha = 0.2f),
                                selectedLabelColor = fmt.badgeColor
                            )
                        )
                    }
                }
            }

            // Documents List
            if (filteredDocs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron documentos en esta vista.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredDocs, key = { it.id }) { doc ->
                        DocumentItemCard(
                            item = doc,
                            isSelectedForCompilation = selectedDocIds.contains(doc.id),
                            onToggleSelect = { onToggleSelect(doc.id) },
                            onPreview = { onPreviewDocument(doc) },
                            onConvert = { onConvertDocument(doc) },
                            onDelete = { onDeleteDocument(doc.id) }
                        )
                    }
                }
            }
        }

        // Sticky Bottom Bar for Compilation when items selected
        if (selectedDocIds.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${selectedDocIds.size} archivo${if (selectedDocIds.size == 1) "" else "s"} seleccionado${if (selectedDocIds.size == 1) "" else "s"}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Listos para unificar y compilar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Button(
                        onClick = { showCompileDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("btn_open_compile_dialog")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compilar")
                    }
                }
            }
        }

        if (showCreateDocDialog) {
            CreateDocumentDialog(
                onConfirm = { title, content, format ->
                    onCreateDocument(title, content, format)
                    showCreateDocDialog = false
                },
                onDismiss = { showCreateDocDialog = false }
            )
        }

        if (showCompileDialog) {
            CompileOptionsDialog(
                selectedCount = selectedDocIds.size,
                isCompiling = isCompiling,
                onConfirm = { title, format, toc, sha, div ->
                    onCompileConfirm(title, format, toc, sha, div)
                    showCompileDialog = false
                },
                onDismiss = { showCompileDialog = false }
            )
        }
    }
}
