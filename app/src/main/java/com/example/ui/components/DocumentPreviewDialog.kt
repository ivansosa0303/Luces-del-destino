package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.data.model.DocumentItemEntity
import com.example.data.model.SupportedFormat
import com.example.engine.FileStorageManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentPreviewDialog(
    document: DocumentItemEntity,
    textPreview: String?,
    imagePreview: Bitmap?,
    pdfCurrentPage: Int,
    pdfTotalPages: Int,
    onPdfPageChange: (Int) -> Unit,
    onSaveEditedText: (String) -> Unit,
    onConvertToFormat: () -> Unit,
    onDismiss: () -> Unit,
    storageManager: FileStorageManager
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val format = SupportedFormat.fromExtension(document.extension)

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var editedContent by remember(textPreview) { mutableStateOf(textPreview ?: "") }
    var isDirty by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FormatBadge(format = format)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = document.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = "${document.fileName} • ${FileStorageManager.formatFileSize(document.fileSizeBytes)}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }

                    IconButton(
                        onClick = {
                            shareDocument(context, document, storageManager)
                        },
                        modifier = Modifier.testTag("btn_share_preview")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartir archivo"
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_preview")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar"
                        )
                    }
                }

                // Tabs: Vista Previa, Editor (si es texto), Integridad
                val tabs = if (format.isTextBased || format == SupportedFormat.DOCX) {
                    listOf("Vista de Contenido", "Editor", "Integridad SHA-256")
                } else {
                    listOf("Vista de Contenido", "Integridad SHA-256")
                }

                PrimaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                        )
                    }
                }

                // Tab Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    when (selectedTabIndex) {
                        0 -> {
                            // Vista Previa
                            when {
                                format == SupportedFormat.PDF -> {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = { if (pdfCurrentPage > 0) onPdfPageChange(pdfCurrentPage - 1) },
                                                enabled = pdfCurrentPage > 0
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Página anterior")
                                            }

                                            Text(
                                                text = "Página ${pdfCurrentPage + 1} de $pdfTotalPages",
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )

                                            IconButton(
                                                onClick = { if (pdfCurrentPage < pdfTotalPages - 1) onPdfPageChange(pdfCurrentPage + 1) },
                                                enabled = pdfCurrentPage < pdfTotalPages - 1
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Página siguiente")
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        if (imagePreview != null) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .verticalScroll(rememberScrollState())
                                            ) {
                                                Image(
                                                    bitmap = imagePreview.asImageBitmap(),
                                                    contentDescription = "Página PDF",
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier.weight(1f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator()
                                            }
                                        }
                                    }
                                }
                                format == SupportedFormat.PNG || format == SupportedFormat.JPG -> {
                                    if (imagePreview != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState()),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                bitmap = imagePreview.asImageBitmap(),
                                                contentDescription = "Imagen",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                }
                                else -> {
                                    // Text, Markdown, JSON, DOCX text
                                    if (textPreview != null) {
                                        val scrollState = rememberScrollState()
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (format == SupportedFormat.JSON) Color(0xFF0F172A)
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                                )
                                                .padding(16.dp)
                                                .verticalScroll(scrollState)
                                        ) {
                                            Text(
                                                text = textPreview,
                                                fontFamily = if (format == SupportedFormat.JSON) FontFamily.Monospace else FontFamily.Default,
                                                color = if (format == SupportedFormat.JSON) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurface,
                                                fontSize = if (format == SupportedFormat.JSON) 12.sp else 14.sp,
                                                lineHeight = 20.sp
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Editor (if text/docx) or Integrity (if binary)
                            if (format.isTextBased || format == SupportedFormat.DOCX) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Edición directa de texto",
                                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                        Button(
                                            onClick = {
                                                onSaveEditedText(editedContent)
                                                isDirty = false
                                            },
                                            enabled = isDirty,
                                            modifier = Modifier.testTag("btn_save_edited_text")
                                        ) {
                                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Guardar Cambios")
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = editedContent,
                                        onValueChange = {
                                            editedContent = it
                                            isDirty = true
                                        },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag("input_edit_document_text"),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = if (format == SupportedFormat.JSON) FontFamily.Monospace else FontFamily.Default,
                                            fontSize = 13.sp
                                        )
                                    )
                                }
                            } else {
                                IntegrityTab(document)
                            }
                        }
                        2 -> {
                            // Integrity Tab
                            IntegrityTab(document)
                        }
                    }
                }

                // Footer Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onConvertToFormat,
                        modifier = Modifier.testTag("btn_convert_from_preview")
                    ) {
                        Icon(Icons.Default.Transform, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Convertir a otro formato")
                    }

                    Text(
                        text = "100% Offline • Sin red",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun IntegrityTab(document: DocumentItemEntity) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Garantía de Integridad Criptográfica",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "DocMerge almacena y procesa cada archivo de forma local en el dispositivo preservando cada byte original.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Hash SHA-256 (Firma Digital Local):",
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = document.sha256Checksum,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(document.sha256Checksum))
                        copied = true
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copiar hash",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        if (copied) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("¡Hash copiado al portapapeles!", color = Color(0xFF059669), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Detalles Técnicos:",
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.height(8.dp))

        MetadataRow(label = "Nombre Original:", value = document.fileName)
        MetadataRow(label = "Extensión:", value = ".${document.extension}")
        MetadataRow(label = "Tipo MIME:", value = document.mimeType)
        MetadataRow(label = "Tamaño Exacto:", value = "${document.fileSizeBytes} bytes (${FileStorageManager.formatFileSize(document.fileSizeBytes)})")
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(document.createdAt))
        MetadataRow(label = "Fecha de Importación:", value = dateStr)
        MetadataRow(label = "Ruta Interna de Memoria:", value = document.relativeFilePath)
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.Monospace
            ),
            maxLines = 1
        )
    }
}

private fun shareDocument(context: Context, document: DocumentItemEntity, storageManager: FileStorageManager) {
    try {
        val file = storageManager.getFileFromRelativePath(document.relativeFilePath)
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = document.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, document.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir ${document.title}"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
