package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.ChapterEntity
import com.example.data.model.DailyWritingGoalEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.StoryBibleEntity
import com.example.data.model.SupportedFormat
import com.example.ui.components.BibleEntryItemCard
import com.example.ui.components.ChapterListItemCard
import com.example.ui.components.CreateCharacterDialog
import com.example.ui.components.CreateChapterDialog
import com.example.ui.components.CreateSettingDialog
import com.example.ui.components.CreateSubplotDialog
import com.example.ui.components.EditChapterMetadataDialog
import com.example.ui.components.EditBibleEntryDialog
import com.example.ui.theme.EditorialAmber
import com.example.ui.theme.EditorialCyan
import com.example.ui.theme.EditorialPurple
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryProjectDetailScreen(
    project: ProjectEntity,
    chapters: List<ChapterEntity>,
    bibleEntries: List<StoryBibleEntity>,
    recentGoals: List<DailyWritingGoalEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    bibleSearchQuery: String,
    onBibleSearchQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onSelectChapter: (ChapterEntity) -> Unit,
    onCreateChapter: (title: String, summary: String, pov: String, targetWords: Int, notes: String) -> Unit,
    onDeleteChapter: (Long) -> Unit,
    onToggleChapterStatus: (ChapterEntity) -> Unit,
    onMoveChapterUp: (ChapterEntity) -> Unit = {},
    onMoveChapterDown: (ChapterEntity) -> Unit = {},
    onDuplicateChapter: (ChapterEntity) -> Unit = {},
    onUpdateChapterMetadata: (Long, String, String, String, Int, String) -> Unit = { _, _, _, _, _, _ -> },
    onCreateCharacter: (name: String, role: String, motivation: String, appearance: String, flawAndArc: String) -> Unit = { _, _, _, _, _ -> },
    onCreateSetting: (name: String, locationType: String, atmosphere: String, worldRules: String, secrets: String) -> Unit = { _, _, _, _, _ -> },
    onCreateSubplot: (title: String, subplotType: String, conflictStakes: String, charactersInvolved: String, resolutionNotes: String, status: String) -> Unit = { _, _, _, _, _, _ -> },
    onCycleSubplotStatus: (StoryBibleEntity) -> Unit = {},
    onUpdateBibleEntry: (StoryBibleEntity) -> Unit = {},
    onCreateBibleEntry: (category: String, name: String, role: String, desc: String, notes: String) -> Unit,
    onDeleteBibleEntry: (Long) -> Unit,
    onCompileStory: (SupportedFormat, (File) -> Unit) -> Unit,
    onShareFile: (File, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateChapterDialog by remember { mutableStateOf(false) }
    var editingChapter by remember { mutableStateOf<ChapterEntity?>(null) }
    var editingBibleEntry by remember { mutableStateOf<StoryBibleEntity?>(null) }
    var chapterPendingDelete by remember { mutableStateOf<ChapterEntity?>(null) }
    var biblePendingDelete by remember { mutableStateOf<StoryBibleEntity?>(null) }
    var showCompileDialog by remember { mutableStateOf(false) }
    var compiledMessage by remember { mutableStateOf<String?>(null) }
    var compiledFile by remember { mutableStateOf<File?>(null) }
    var compiledMime by remember { mutableStateOf("text/plain") }

    // Dialog state for Character & Plot
    var showCreateCharacterDialog by remember { mutableStateOf(false) }
    var showCreateSettingDialog by remember { mutableStateOf(false) }
    var showCreateSubplotDialog by remember { mutableStateOf(false) }
    var showBibleTypeSelector by remember { mutableStateOf(false) }

    var bibleCategoryFilter by remember { mutableStateOf("TODOS") }
    var chapterStatusFilter by remember { mutableStateOf("TODOS") }

    val totalWords = chapters.sumOf { it.wordCount }
    val progress = if (project.targetWordCount > 0) {
        (totalWords.toFloat() / project.targetWordCount).coerceIn(0f, 1f)
    } else 0f
    val totalReadingMinutes = (totalWords / 200.0).toInt()

    val charactersList = bibleEntries.filter { it.category == "PERSONAJE" }
    val settingsList = bibleEntries.filter { it.category == "ESCENARIO" }
    val subplotsList = bibleEntries.filter { it.category == "TRAMA_SECUNDARIA" }
    val characterNames = charactersList.map { it.name }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = project.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = project.genre,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "• $totalWords palabras (~$totalReadingMinutes min de lectura)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(
                            onClick = { showCompileDialog = true },
                            modifier = Modifier.testTag("btn_compile_book"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Compilar", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Progreso del libro: $totalWords / ${project.targetWordCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldLight
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = EmeraldPrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // Tabs
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Capítulos (${chapters.size})") },
                    icon = { Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Personajes & Tramas (${bibleEntries.size})") },
                    icon = { Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Métricas y Ritmo") },
                    icon = { Icon(Icons.Default.QueryStats, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // CHAPTERS MANAGEMENT TAB
                    val filteredByStatus = chapters.filter { chap ->
                        if (chapterStatusFilter == "TODOS") true
                        else chap.status == chapterStatusFilter
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                label = { Text("Buscar en capítulos, resumen, notas o POV...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_chapters_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Status Filter Chips
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "TODOS" to "Todos (${chapters.size})",
                                    "BORRADOR" to "Borradores",
                                    "EN_REVISION" to "En Revisión",
                                    "PULIDO" to "Pulidos"
                                ).forEach { (statusKey, label) ->
                                    FilterChip(
                                        selected = chapterStatusFilter == statusKey,
                                        onClick = { chapterStatusFilter = statusKey },
                                        label = { Text(label, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        if (filteredByStatus.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = if (searchQuery.isBlank()) "Aún no hay capítulos creados en esta vista." else "No se encontraron capítulos con '$searchQuery'.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(onClick = { showCreateChapterDialog = true }) {
                                            Text("Escribir Nuevo Capítulo")
                                        }
                                    }
                                }
                            }
                        } else {
                            items(filteredByStatus, key = { it.id }) { chap ->
                                ChapterListItemCard(
                                    chapter = chap,
                                    onClick = { onSelectChapter(chap) },
                                    onDelete = { chapterPendingDelete = chap },
                                    onToggleStatus = { onToggleChapterStatus(chap) },
                                    onMoveUp = { onMoveChapterUp(chap) },
                                    onMoveDown = { onMoveChapterDown(chap) },
                                    onDuplicate = { onDuplicateChapter(chap) },
                                    onEditMetadata = { editingChapter = chap }
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
                1 -> {
                    // CHARACTER & PLOT (PERSONAJES, ESCENARIOS, TRAMAS) TAB
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = bibleSearchQuery,
                                onValueChange = onBibleSearchQueryChange,
                                label = { Text("Buscar personaje, escenario o conflicto de trama...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_bible_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Category Filter Chips with Icons and Counts
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = bibleCategoryFilter == "TODOS",
                                    onClick = { bibleCategoryFilter = "TODOS" },
                                    label = { Text("Todos (${bibleEntries.size})", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = bibleCategoryFilter == "PERSONAJE",
                                    onClick = { bibleCategoryFilter = "PERSONAJE" },
                                    label = { Text("Personajes (${charactersList.size})", fontSize = 11.sp) },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                                FilterChip(
                                    selected = bibleCategoryFilter == "ESCENARIO",
                                    onClick = { bibleCategoryFilter = "ESCENARIO" },
                                    label = { Text("Escenarios (${settingsList.size})", fontSize = 11.sp) },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                                FilterChip(
                                    selected = bibleCategoryFilter == "TRAMA_SECUNDARIA",
                                    onClick = { bibleCategoryFilter = "TRAMA_SECUNDARIA" },
                                    label = { Text("Tramas (${subplotsList.size})", fontSize = 11.sp) },
                                    leadingIcon = { Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                            }
                        }

                        val filteredEntries = when (bibleCategoryFilter) {
                            "PERSONAJE" -> charactersList
                            "ESCENARIO" -> settingsList
                            "TRAMA_SECUNDARIA" -> subplotsList
                            else -> bibleEntries
                        }

                        if (filteredEntries.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "No hay registros en esta sección todavía.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(onClick = { showBibleTypeSelector = true }) {
                                            Text("Crear Ficha Narrativa")
                                        }
                                    }
                                }
                            }
                        } else {
                            items(filteredEntries, key = { it.id }) { entry ->
                                // Toda la ficha abre el editor: antes solo se
                                // podían crear y borrar, nunca corregir.
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { editingBibleEntry = entry }
                                        .testTag("bible_entry_${entry.id}")
                                ) {
                                    BibleEntryItemCard(
                                        entry = entry,
                                        onDelete = { biblePendingDelete = entry },
                                        onCycleStatus = { onCycleSubplotStatus(entry) }
                                    )
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
                2 -> {
                    // Metrics & Rhythm Tab
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Ritmo y Extensión de la Obra",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Total Palabras", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("$totalWords", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = EmeraldLight)
                                        }
                                        Column {
                                            Text("Capítulos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${chapters.size}", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = EditorialCyan)
                                        }
                                        Column {
                                            Text("Promedio / Cap.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            val avg = if (chapters.isNotEmpty()) totalWords / chapters.size else 0
                                            Text("$avg", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = EditorialPurple)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Estado Editorial de Capítulos",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val pulidos = chapters.count { it.status == "PULIDO" }
                                    val enRevision = chapters.count { it.status == "EN_REVISION" }
                                    val borradores = chapters.count { it.status == "BORRADOR" }
                                    Text("• $pulidos capítulos pulidos y listos para edición final", color = EmeraldPrimary)
                                    Text("• $enRevision capítulos en fase de revisión de estilo", color = EditorialCyan)
                                    Text("• $borradores primeros borradores en desarrollo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Desglose de Personajes & Tramas",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("• ${charactersList.size} personajes en el elenco", color = EditorialPurple)
                                    Text("• ${settingsList.size} locaciones y escenarios del mundo", color = EditorialCyan)
                                    Text("• ${subplotsList.size} tramas secundarias activas o resueltas", color = EditorialAmber)
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Hábito y Metas Diarias",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (recentGoals.isEmpty()) {
                                        Text("Escribe o edita capítulos para registrar tu racha y palabras diarias automáticamente.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        recentGoals.forEach { goal ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(goal.dateKey, style = MaterialTheme.typography.bodySmall)
                                                Text("${goal.wordsWritten} / ${goal.targetWords} palabras", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = EmeraldLight)
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

        // FAB based on active tab
        if (selectedTab == 0) {
            FloatingActionButton(
                onClick = { showCreateChapterDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("fab_add_chapter"),
                containerColor = EmeraldPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Capítulo")
            }
        } else if (selectedTab == 1) {
            FloatingActionButton(
                onClick = { showBibleTypeSelector = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("fab_add_bible_entry"),
                containerColor = EditorialPurple,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Ficha")
            }
        }
    }

    // Modal dialogs
    if (showCreateChapterDialog) {
        CreateChapterDialog(
            onDismiss = { showCreateChapterDialog = false },
            onCreate = onCreateChapter,
            availableCharacters = characterNames
        )
    }

    if (editingChapter != null) {
        EditChapterMetadataDialog(
            chapter = editingChapter!!,
            onDismiss = { editingChapter = null },
            onSave = { title, summary, pov, targetWords, notes ->
                onUpdateChapterMetadata(editingChapter!!.id, title, summary, pov, targetWords, notes)
                editingChapter = null
            },
            availableCharacters = characterNames
        )
    }

    // Dialog to choose what to create in Character & Plot section
    if (showBibleTypeSelector) {
        AlertDialog(
            onDismissRequest = { showBibleTypeSelector = false },
            title = { Text("Nueva Ficha Narrativa") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Selecciona el tipo de elemento a registrar en la base de datos:")
                    Button(
                        onClick = {
                            showBibleTypeSelector = false
                            showCreateCharacterDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_type_character")
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Perfil de Personaje (Arquetipo, Motivación, Defecto)")
                    }
                    Button(
                        onClick = {
                            showBibleTypeSelector = false
                            showCreateSettingDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_type_setting")
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Escenario & Lore (Atmósfera sensorial, Reglas)")
                    }
                    Button(
                        onClick = {
                            showBibleTypeSelector = false
                            showCreateSubplotDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_type_subplot")
                    ) {
                        Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Trama Secundaria (Conflicto, Tensión, Estado)")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBibleTypeSelector = false }) { Text("Cancelar") }
            }
        )
    }

    if (showCreateCharacterDialog) {
        CreateCharacterDialog(
            onDismiss = { showCreateCharacterDialog = false },
            onCreate = { name, role, mot, app, flaw ->
                onCreateCharacter(name, role, mot, app, flaw)
                showCreateCharacterDialog = false
            }
        )
    }

    if (showCreateSettingDialog) {
        CreateSettingDialog(
            onDismiss = { showCreateSettingDialog = false },
            onCreate = { name, locationType, atmosphere, worldRules, secrets ->
                onCreateSetting(name, locationType, atmosphere, worldRules, secrets)
                showCreateSettingDialog = false
            }
        )
    }

    if (showCreateSubplotDialog) {
        CreateSubplotDialog(
            onDismiss = { showCreateSubplotDialog = false },
            onCreate = { title, subplotType, conflictStakes, charactersInvolved, resolutionNotes, status ->
                onCreateSubplot(title, subplotType, conflictStakes, charactersInvolved, resolutionNotes, status)
                showCreateSubplotDialog = false
            }
        )
    }

    if (showCompileDialog) {
        var format by remember { mutableStateOf(SupportedFormat.PDF) }

        AlertDialog(
            onDismissRequest = { showCompileDialog = false },
            title = { Text("Exportar Novela Completa") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Selecciona el formato para unir y maquetar los ${chapters.size} capítulos en un único documento:")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = format == SupportedFormat.PDF,
                            onClick = { format = SupportedFormat.PDF },
                            label = { Text("PDF maquetado") }
                        )
                        FilterChip(
                            selected = format == SupportedFormat.DOCX,
                            onClick = { format = SupportedFormat.DOCX },
                            label = { Text("Word (.docx)") }
                        )
                        FilterChip(
                            selected = format == SupportedFormat.MD,
                            onClick = { format = SupportedFormat.MD },
                            label = { Text("Markdown") }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = format == SupportedFormat.TXT,
                            onClick = { format = SupportedFormat.TXT },
                            label = { Text("Texto (.txt)") }
                        )
                        FilterChip(
                            selected = format == SupportedFormat.JSON,
                            onClick = { format = SupportedFormat.JSON },
                            label = { Text("JSON (.json)") }
                        )
                    }
                    if (compiledMessage != null) {
                        Text(
                            text = compiledMessage ?: "",
                            color = EmeraldPrimary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (compiledFile != null) {
                        Button(
                            onClick = {
                                compiledFile?.let { onShareFile(it, compiledMime) }
                            },
                            modifier = Modifier.testTag("btn_share_compiled_story")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Compartir")
                        }
                    }
                    Button(
                        onClick = {
                            onCompileStory(format) { file ->
                                compiledFile = file
                                compiledMime = when (format) {
                                    SupportedFormat.PDF -> "application/pdf"
                                    SupportedFormat.DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                    SupportedFormat.JSON -> "application/json"
                                    SupportedFormat.MD -> "text/markdown"
                                    else -> "text/plain"
                                }
                                compiledMessage = "Listo: ${file.name}. Usa «Compartir» para enviarlo o guardarlo."
                            }
                        },
                        modifier = Modifier.testTag("confirm_compile_story")
                    ) {
                        Text("Exportar")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCompileDialog = false
                    compiledMessage = null
                    compiledFile = null
                }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Confirmación antes de borrar: un capítulo o una ficha no se pueden
    // recuperar, y antes el botón de papelera borraba al primer toque.
    if (chapterPendingDelete != null) {
        val target = chapterPendingDelete!!
        AlertDialog(
            onDismissRequest = { chapterPendingDelete = null },
            title = { Text("¿Eliminar el capítulo?") },
            text = {
                Text("Se borrará «${target.title}» y su contenido (${target.wordCount} palabras). Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteChapter(target.id)
                        chapterPendingDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_chapter")
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { chapterPendingDelete = null }) { Text("Cancelar") }
            }
        )
    }

    if (biblePendingDelete != null) {
        val target = biblePendingDelete!!
        AlertDialog(
            onDismissRequest = { biblePendingDelete = null },
            title = { Text("¿Eliminar la ficha?") },
            text = { Text("Se borrará «${target.name}» de la biblia de la novela.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteBibleEntry(target.id)
                        biblePendingDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_bible_entry")
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { biblePendingDelete = null }) { Text("Cancelar") }
            }
        )
    }

    if (editingBibleEntry != null) {
        val target = editingBibleEntry!!
        EditBibleEntryDialog(
            entry = target,
            onDismiss = { editingBibleEntry = null },
            onSave = { name, role, description, details, notes, status ->
                onUpdateBibleEntry(
                    target.copy(
                        name = name,
                        roleOrLocation = role,
                        description = description,
                        appearanceOrDetails = details,
                        notes = notes,
                        status = status
                    )
                )
                editingBibleEntry = null
            }
        )
    }
}
