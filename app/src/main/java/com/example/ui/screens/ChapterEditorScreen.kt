package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChapterEntity
import com.example.data.model.StoryBibleEntity
import com.example.engine.EditorialAnalysisResult
import com.example.ui.components.BibleEntryItemCard
import com.example.ui.theme.EditorialAmber
import com.example.ui.theme.EditorialCyan
import com.example.ui.theme.EditorialPurple
import com.example.ui.theme.EditorialRose
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NightBackground
import com.example.ui.theme.NightSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterEditorScreen(
    chapter: ChapterEntity,
    content: String,
    editorialAnalysis: EditorialAnalysisResult,
    bibleEntries: List<StoryBibleEntity>,
    saveStatus: String?,
    canUndo: Boolean,
    canRedo: Boolean,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onBack: () -> Unit,
    onToggleStatus: (ChapterEntity) -> Unit,
    onApplyDialogueDashFix: () -> Unit,
    onApplyWhitespaceFix: () -> Unit,
    onApplyOpeningPunctuationFix: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditorialSheet by remember { mutableStateOf(false) }
    var selectedEditorialTab by remember { mutableIntStateOf(0) }
    var saveFeedback by remember { mutableStateOf(false) }
    var focusMode by remember { mutableStateOf(false) }
    var showFindReplace by remember { mutableStateOf(false) }

    // En modo enfoque, "atrás" sale del modo en vez de cerrar el capítulo.
    BackHandler(enabled = focusMode) { focusMode = false }

    // El icono de guardado vuelve a su estado normal tras confirmar.
    LaunchedEffect(saveStatus) {
        if (saveStatus?.startsWith("Guardado") == true) {
            saveFeedback = true
            kotlinx.coroutines.delay(1600)
            saveFeedback = false
        }
    }

    val statusColor = when (chapter.status) {
        "PULIDO" -> EmeraldPrimary
        "EN_REVISION" -> EditorialCyan
        else -> EditorialAmber
    }

    val totalIssuesCount = editorialAnalysis.echoes.size +
            editorialAnalysis.styleAlerts.size +
            editorialAnalysis.formattingIssues.size

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NightBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Toolbar
            if (!focusMode) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Guardar y volver")
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Cap. ${chapter.chapterNumber}: ${chapter.title}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Status Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(statusColor.copy(alpha = 0.2f))
                                    .clickable { onToggleStatus(chapter) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = chapter.status.replace("_", " "),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = statusColor,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${editorialAnalysis.wordCount} palabras • ~${(editorialAnalysis.readingTimeMinutes).toInt().coerceAtLeast(1)} min de lectura",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!saveStatus.isNullOrBlank()) {
                                Text(
                                    text = "  ·  $saveStatus",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (saveStatus.startsWith("Guardado")) {
                                        EmeraldPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.testTag("save_status_text")
                                )
                            }
                        }
                    }

                    // Character & Plot Quick Lookup Button
                    Button(
                        onClick = {
                            selectedEditorialTab = 3
                            showEditorialSheet = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showEditorialSheet && selectedEditorialTab == 3) EditorialPurple.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = EditorialPurple
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_lookup_characters_plots")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Personajes y Tramas",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Fichas (${bibleEntries.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Editorial Suite Toggle Button
                    Button(
                        onClick = {
                            if (showEditorialSheet && selectedEditorialTab != 3) {
                                showEditorialSheet = false
                            } else {
                                selectedEditorialTab = 0
                                showEditorialSheet = true
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showEditorialSheet && selectedEditorialTab != 3) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (showEditorialSheet && selectedEditorialTab != 3) EmeraldLight else MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_toggle_editorial_suite")
                    ) {
                        BadgedBox(
                            badge = {
                                if (totalIssuesCount > 0) {
                                    Badge(containerColor = EditorialAmber) {
                                        Text("$totalIssuesCount", color = Color.Black, fontSize = 9.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = "Suite Editorial",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Suite", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Save Button
                    IconButton(
                        onClick = {
                            onSave()
                            saveFeedback = true
                        },
                        modifier = Modifier.testTag("btn_save_chapter")
                    ) {
                        Icon(
                            imageVector = if (saveFeedback) Icons.Default.Check else Icons.Default.Save,
                            contentDescription = "Guardar",
                            tint = if (saveFeedback) EmeraldPrimary else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Barra de acciones: deshacer, rehacer, buscar y modo enfoque.
            // Antes no existía ninguna forma de recuperar texto borrado.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier.testTag("btn_undo")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Deshacer",
                        tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier.testTag("btn_redo")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Rehacer",
                        tint = if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
                IconButton(
                    onClick = { showFindReplace = true },
                    modifier = Modifier.testTag("btn_find_replace")
                ) {
                    Icon(Icons.Default.FindReplace, contentDescription = "Buscar y reemplazar")
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { focusMode = true },
                    modifier = Modifier.testTag("btn_focus_mode")
                ) {
                    Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Enfoque", fontSize = 12.sp)
                }
            }
            } else {
                // Modo enfoque: solo el texto. Salir con el botón o con atrás.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cap. ${chapter.chapterNumber} · ${editorialAnalysis.wordCount} palabras",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { focusMode = false },
                        modifier = Modifier.testTag("btn_exit_focus_mode")
                    ) {
                        Icon(Icons.Default.FullscreenExit, contentDescription = "Salir del modo enfoque")
                    }
                }
            }

            // Main Editor Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                OutlinedTextField(
                    value = content,
                    onValueChange = onContentChange,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("chapter_content_input"),
                    placeholder = {
                        Text(
                            "Comienza a escribir tu escena o capítulo aquí...\n\nUsa raya de diálogo (—) para conversaciones. La Suite Editorial analizará tu ritmo, repeticiones y ortografía en tiempo real.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            style = TextStyle(fontSize = 16.sp, lineHeight = 24.sp)
                        )
                    },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 26.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Default
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = NightBackground,
                        unfocusedContainerColor = NightBackground
                    )
                )
            }

            // Editorial Suite Drawer / Bottom Sheet
            if (showEditorialSheet) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.48f),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Sheet Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = EmeraldLight)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Suite Editorial & Pulido",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            IconButton(onClick = { showEditorialSheet = false }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar panel")
                            }
                        }

                        SecondaryTabRow(
                            selectedTabIndex = selectedEditorialTab,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Tab(
                                selected = selectedEditorialTab == 0,
                                onClick = { selectedEditorialTab = 0 },
                                text = { Text("Formato & Puntuación", fontSize = 11.sp) }
                            )
                            Tab(
                                selected = selectedEditorialTab == 1,
                                onClick = { selectedEditorialTab = 1 },
                                text = { Text("Repeticiones & Estilo", fontSize = 11.sp) }
                            )
                            Tab(
                                selected = selectedEditorialTab == 2,
                                onClick = { selectedEditorialTab = 2 },
                                text = { Text("Ritmo & Diálogo", fontSize = 11.sp) }
                            )
                            Tab(
                                selected = selectedEditorialTab == 3,
                                onClick = { selectedEditorialTab = 3 },
                                text = { Text("Fichas Rápidas", fontSize = 11.sp) }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                        // Tab Content
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            when (selectedEditorialTab) {
                                0 -> {
                                    // Formato y Ortografía Tab
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        item {
                                            Text(
                                                "Acciones de Corrección en 1 Toque:",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = EmeraldLight
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = onApplyDialogueDashFix,
                                                    modifier = Modifier.weight(1f),
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                                ) {
                                                    Text("⚡ Raya Diálogo (—)", fontSize = 11.sp)
                                                }
                                                Button(
                                                    onClick = onApplyWhitespaceFix,
                                                    modifier = Modifier.weight(1f),
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                                ) {
                                                    Text("⚡ Limpiar Espacios", fontSize = 11.sp)
                                                }
                                                Button(
                                                    onClick = onApplyOpeningPunctuationFix,
                                                    modifier = Modifier.weight(1f),
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                                ) {
                                                    Text("⚡ Signos ¿ y ¡", fontSize = 11.sp)
                                                }
                                            }
                                        }

                                        item {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                "Detalles de Maquetación Detectados:",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }

                                        if (editorialAnalysis.formattingIssues.isEmpty()) {
                                            item {
                                                Text(
                                                    "✅ El formato y las rayas de diálogo lucen limpios y consistentes.",
                                                    color = EmeraldPrimary,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        } else {
                                            items(editorialAnalysis.formattingIssues) { issue ->
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp)) {
                                                        Text(issue.title, fontWeight = FontWeight.Bold, color = EditorialAmber, fontSize = 12.sp)
                                                        Text(issue.description, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                1 -> {
                                    // Repeticiones y Estilo Tab
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        item {
                                            Text(
                                                "Ecos Léxicos (Palabras Repetidas en Cercanías):",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = EditorialCyan
                                            )
                                        }

                                        if (editorialAnalysis.echoes.isEmpty()) {
                                            item {
                                                Text(
                                                    "✅ No se detectaron palabras repetidas con proximidad excesiva.",
                                                    color = EmeraldPrimary,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        } else {
                                            items(editorialAnalysis.echoes) { echo ->
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(EditorialRose.copy(alpha = 0.2f))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = "\"${echo.word}\" x${echo.countInWindow}",
                                                                color = EditorialRose,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 11.sp
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Text(
                                                            text = echo.sampleSnippet,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        item {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "Muletillas y Ritmo:",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = EditorialAmber
                                            )
                                        }

                                        if (editorialAnalysis.styleAlerts.isEmpty()) {
                                            item {
                                                Text(
                                                    "✅ Sin muletillas o frases kilométricas problemáticas.",
                                                    color = EmeraldPrimary,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        } else {
                                            items(editorialAnalysis.styleAlerts) { alert ->
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp)) {
                                                        Text(alert.title, fontWeight = FontWeight.Bold, color = EditorialAmber, fontSize = 12.sp)
                                                        Text(alert.message, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                2 -> {
                                    // Ritmo y Diálogo Tab
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        item {
                                            Text(
                                                "Proporción de Diálogo vs. Narración:",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    "Diálogo: ${editorialAnalysis.dialoguePercentage}%",
                                                    color = EditorialCyan,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    "Narración: ${100 - editorialAnalysis.dialoguePercentage}%",
                                                    color = EmeraldLight,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { editorialAnalysis.dialoguePercentage / 100f },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp)),
                                                color = EditorialCyan,
                                                trackColor = EmeraldPrimary.copy(alpha = 0.5f)
                                            )
                                            Text(
                                                text = when {
                                                    editorialAnalysis.dialoguePercentage > 60 -> "Capítulo muy dinámico y enfocado en interacción y voces de personajes."
                                                    editorialAnalysis.dialoguePercentage < 20 -> "Capítulo descriptivo o reflexivo. Si sientes el ritmo lento, introduce diálogo o acción."
                                                    else -> "Buen equilibrio entre descripción de escenario y conversación."
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        item {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Text(
                                                        "Índice de Agilidad Narrativa (Fernández-Huerta):",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            "${editorialAnalysis.readabilityScore}/100",
                                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                                            color = EmeraldLight
                                                        )
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Text(
                                                            editorialAnalysis.readabilityLabel,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                3 -> {
                                    // Fichas Rápidas Tab
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (bibleEntries.isEmpty()) {
                                            item {
                                                Text(
                                                    "No tienes fichas registradas aún en esta historia. Agrégalas desde la pestaña Biblia de la Novela.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        } else {
                                            items(bibleEntries) { entry ->
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                                ) {
                                                    Column(modifier = Modifier.padding(8.dp)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                entry.name,
                                                                fontWeight = FontWeight.Bold,
                                                                color = EditorialPurple,
                                                                fontSize = 12.sp
                                                            )
                                                            if (entry.roleOrLocation.isNotBlank()) {
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text("(${entry.roleOrLocation})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            }
                                                        }
                                                        if (entry.description.isNotBlank()) {
                                                            Text(entry.description, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFindReplace) {
        FindReplaceDialog(
            content = content,
            onDismiss = { showFindReplace = false },
            onReplaceAll = { query, replacement, ignoreCase ->
                val updated = if (ignoreCase) {
                    Regex(Regex.escape(query), RegexOption.IGNORE_CASE)
                        .replace(content, Regex.escapeReplacement(replacement))
                } else {
                    content.replace(query, replacement)
                }
                onContentChange(updated)
            }
        )
    }
}

/**
 * Buscar y reemplazar dentro del capítulo abierto.
 *
 * Trabaja sobre el texto completo en lugar de sobre la selección porque
 * `OutlinedTextField` con `String` no expone la posición del cursor; así el
 * usuario puede corregir un nombre de personaje mal escrito en todo el
 * capítulo de una vez.
 */
@Composable
private fun FindReplaceDialog(
    content: String,
    onDismiss: () -> Unit,
    onReplaceAll: (query: String, replacement: String, ignoreCase: Boolean) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf("") }
    var ignoreCase by remember { mutableStateOf(true) }

    val matches = remember(content, query, ignoreCase) {
        if (query.isEmpty()) {
            0
        } else if (ignoreCase) {
            Regex(Regex.escape(query), RegexOption.IGNORE_CASE).findAll(content).count()
        } else {
            Regex(Regex.escape(query)).findAll(content).count()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buscar y reemplazar") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Buscar") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("find_query_input")
                )
                OutlinedTextField(
                    value = replacement,
                    onValueChange = { replacement = it },
                    label = { Text("Reemplazar por") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("replace_query_input")
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { ignoreCase = !ignoreCase }
                ) {
                    Checkbox(
                        checked = ignoreCase,
                        onCheckedChange = { ignoreCase = it }
                    )
                    Text("Ignorar mayúsculas y minúsculas", fontSize = 12.sp)
                }
                Text(
                    text = if (query.isEmpty()) {
                        "Escribe el texto que quieres localizar."
                    } else {
                        "$matches coincidencia(s) en este capítulo."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onReplaceAll(query, replacement, ignoreCase)
                    onDismiss()
                },
                enabled = query.isNotEmpty() && matches > 0,
                modifier = Modifier.testTag("confirm_replace_all")
            ) {
                Text("Reemplazar todo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
