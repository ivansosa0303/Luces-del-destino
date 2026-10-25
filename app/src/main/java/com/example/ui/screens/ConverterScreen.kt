package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.DocumentItemEntity
import com.example.data.model.SupportedFormat
import com.example.engine.DocumentConverter
import com.example.engine.FileStorageManager
import com.example.ui.components.FormatBadge
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConverterScreen(
    documents: List<DocumentItemEntity>,
    selectedFile: DocumentItemEntity?,
    targetFormat: SupportedFormat,
    isConverting: Boolean,
    conversionResult: DocumentConverter.ConversionResult?,
    onSelectFile: (DocumentItemEntity?) -> Unit,
    onSelectFileUri: (android.net.Uri) -> Unit = {},
    onSelectTargetFormat: (SupportedFormat) -> Unit,
    onConvert: () -> Unit,
    storageManager: FileStorageManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var fileDropdownExpanded by remember { mutableStateOf(false) }

    val storagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            onSelectFileUri(uri)
        }
    }

    val srcFormat = selectedFile?.let { SupportedFormat.fromExtension(it.extension) }

    val availableTargets = srcFormat?.let {
        when (it) {
            SupportedFormat.TXT -> listOf(SupportedFormat.MD, SupportedFormat.PDF, SupportedFormat.DOCX, SupportedFormat.JSON)
            SupportedFormat.MD -> listOf(SupportedFormat.TXT, SupportedFormat.PDF, SupportedFormat.DOCX, SupportedFormat.JSON)
            SupportedFormat.JSON -> listOf(SupportedFormat.TXT, SupportedFormat.MD, SupportedFormat.PDF, SupportedFormat.DOCX)
            SupportedFormat.DOCX -> listOf(SupportedFormat.MD, SupportedFormat.TXT, SupportedFormat.PDF, SupportedFormat.JSON)
            SupportedFormat.PDF -> listOf(SupportedFormat.PNG, SupportedFormat.JPG, SupportedFormat.MD, SupportedFormat.TXT)
            SupportedFormat.PNG, SupportedFormat.JPG -> listOf(SupportedFormat.PDF, SupportedFormat.JSON, SupportedFormat.MD)
            SupportedFormat.ZIP -> listOf(SupportedFormat.MD, SupportedFormat.JSON)
        }
    } ?: listOf(SupportedFormat.PDF, SupportedFormat.MD, SupportedFormat.DOCX, SupportedFormat.JSON, SupportedFormat.TXT)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Conversor Universal de Formatos",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Conversión directa 1 a 1 entre TXT, MD, JSON, PDF, DOCX e Imágenes sin pérdida de integridad.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // File Selection Section
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Selecciona el archivo de origen",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Direct ContentResolver Storage Picker
                    Button(
                        onClick = {
                            storagePickerLauncher.launch(
                                arrayOf(
                                    "text/*",
                                    "application/pdf",
                                    "application/json",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "image/*",
                                    "*/*"
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_select_from_storage_for_conversion"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Seleccionar archivo del dispositivo")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        androidx.compose.material3.HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = "  o seleccionar de la memoria local  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        androidx.compose.material3.HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (documents.isEmpty()) {
                        Text(
                            text = "No hay archivos cargados aún. Usa el botón superior para seleccionar uno del dispositivo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = fileDropdownExpanded,
                            onExpandedChange = { fileDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedFile?.fileName ?: "Selecciona un archivo existente...",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fileDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                                    .testTag("dropdown_select_converter_source"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            ExposedDropdownMenu(
                                expanded = fileDropdownExpanded,
                                onDismissRequest = { fileDropdownExpanded = false }
                            ) {
                                documents.forEach { doc ->
                                    val f = SupportedFormat.fromExtension(doc.extension)
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                FormatBadge(format = f)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "${doc.title} (${doc.fileName})",
                                                    maxLines = 1
                                                )
                                            }
                                        },
                                        onClick = {
                                            onSelectFile(doc)
                                            fileDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (selectedFile != null && srcFormat != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedCard(
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Origen: ${srcFormat.displayName}",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    FormatBadge(format = srcFormat)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tamaño: ${FileStorageManager.formatFileSize(selectedFile.fileSizeBytes)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "SHA-256: ${selectedFile.sha256Checksum}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Target Format Chooser
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "2. Selecciona el formato de destino",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableTargets.forEach { fmt ->
                            FilterChip(
                                selected = targetFormat == fmt,
                                onClick = { onSelectTargetFormat(fmt) },
                                label = { Text(fmt.displayName, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = fmt.badgeColor.copy(alpha = 0.2f),
                                    selectedLabelColor = fmt.badgeColor
                                ),
                                modifier = Modifier.testTag("chip_convert_target_${fmt.extension}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = targetFormat.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Action Convert Button
        item {
            Button(
                onClick = onConvert,
                enabled = selectedFile != null && !isConverting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_run_conversion")
            ) {
                if (isConverting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Convirtiendo Archivo...")
                } else {
                    Icon(Icons.Default.Transform, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Convertir a ${targetFormat.displayName}",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Result Card if converted
        if (conversionResult != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (conversionResult.success) Color(0xFFECFDF5) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (conversionResult.success) Icons.Default.CheckCircle else Icons.Default.Description,
                                contentDescription = null,
                                tint = if (conversionResult.success) Color(0xFF059669) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (conversionResult.success) "¡Conversión Exitosa!" else "Fallo en la Conversión",
                                fontWeight = FontWeight.Bold,
                                color = if (conversionResult.success) Color(0xFF065F46) else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = conversionResult.message,
                            fontSize = 13.sp,
                            color = if (conversionResult.success) Color(0xFF047857) else MaterialTheme.colorScheme.onErrorContainer
                        )

                        if (conversionResult.success && conversionResult.outputFile != null) {
                            val outFile = conversionResult.outputFile
                            Spacer(modifier = Modifier.height(12.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.9f))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = outFile.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        conversionResult.report?.let { r ->
                                            Text(
                                                text = "${r.executionTimeMs} ms",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF047857)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tamaño final: ${FileStorageManager.formatFileSize(outFile.length())}",
                                        fontSize = 12.sp,
                                        color = Color.DarkGray
                                    )

                                    conversionResult.report?.let { r ->
                                        val sm = r.sourceMetrics
                                        val tm = r.targetMetrics
                                        if (tm != null) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Auditoría de Integridad de Contenido:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = "• Origen: ${sm.lineCount} líneas | ${sm.wordCount} palabras | ${FileStorageManager.formatFileSize(sm.byteSize)}",
                                                fontSize = 11.sp,
                                                color = Color(0xFF334155)
                                            )
                                            Text(
                                                text = "• Destino: ${tm.lineCount} líneas | ${tm.wordCount} palabras | ${FileStorageManager.formatFileSize(tm.byteSize)}",
                                                fontSize = 11.sp,
                                                color = Color(0xFF334155)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "SHA-256: ${conversionResult.sha256}",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.DarkGray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        shareConvertedFile(context, outFile, conversionResult.targetFormat.mimeType)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Compartir")
                                }

                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(conversionResult.sha256))
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copiar Hash")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun shareConvertedFile(context: Context, file: File, mimeType: String) {
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
        e.printStackTrace()
    }
}
