package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChapterEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.StoryBibleEntity
import com.example.ui.theme.EditorialAmber
import com.example.ui.theme.EditorialCyan
import com.example.ui.theme.EditorialPurple
import com.example.ui.theme.EditorialRose
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary

@Composable
fun StoryProjectCard(
    project: ProjectEntity,
    chaptersCount: Int,
    totalWords: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (project.targetWordCount > 0) {
        (totalWords.toFloat() / project.targetWordCount).coerceIn(0f, 1f)
    } else 0f

    val accentColor = try {
        Color(android.graphics.Color.parseColor(project.colorHex))
    } catch (_: Exception) {
        EmeraldPrimary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("story_card_${project.id}")
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = project.genre,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$chaptersCount capítulos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_story_${project.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Eliminar historia",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (project.synopsis.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = project.synopsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress towards book goal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$totalWords / ${project.targetWordCount} palabras",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldLight
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
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
}

@Composable
fun ChapterListItemCard(
    chapter: ChapterEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: () -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onEditMetadata: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val statusColor = when (chapter.status) {
        "PULIDO" -> EmeraldPrimary
        "EN_REVISION" -> EditorialCyan
        else -> EditorialAmber
    }

    val readingMinutes = (chapter.wordCount / 200.0).coerceAtLeast(1.0).toInt()
    val targetWords = if (chapter.targetWords > 0) chapter.targetWords else 2000
    val targetProgress = (chapter.wordCount.toFloat() / targetWords).coerceIn(0f, 1f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("chapter_item_${chapter.id}")
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chapter Number Badge
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${chapter.chapterNumber}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chapter.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Status Pill (clickable to cycle status)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusColor.copy(alpha = 0.15f))
                                .clickable { onToggleStatus() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = chapter.status.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = statusColor,
                                fontSize = 10.sp
                            )
                        }

                        Text(
                            text = "${chapter.wordCount} / $targetWords pal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )

                        Text(
                            text = "• ~$readingMinutes min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )

                        if (chapter.povCharacter.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(EditorialPurple.copy(alpha = 0.15f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "POV: ${chapter.povCharacter}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EditorialPurple,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Move Up / Down Buttons for quick chapter reordering
                if (onMoveUp != null) {
                    IconButton(
                        onClick = onMoveUp,
                        modifier = Modifier.size(32.dp).testTag("btn_move_up_${chapter.id}")
                    ) {
                        Text("▲", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (onMoveDown != null) {
                    IconButton(
                        onClick = onMoveDown,
                        modifier = Modifier.size(32.dp).testTag("btn_move_down_${chapter.id}")
                    ) {
                        Text("▼", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (onEditMetadata != null) {
                    IconButton(
                        onClick = onEditMetadata,
                        modifier = Modifier.size(32.dp).testTag("btn_edit_meta_${chapter.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar detalles",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp).testTag("btn_delete_chap_${chapter.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Eliminar capítulo",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Target word count mini progress
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { targetProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (targetProgress >= 1f) EmeraldPrimary else EditorialCyan,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Scene Notes or Objective Preview
            if (chapter.notes.isNotBlank() || chapter.summary.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = EditorialAmber,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (chapter.notes.isNotBlank()) "Objetivo: ${chapter.notes}" else chapter.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun CharacterProfileCard(
    character: StoryBibleEntity,
    onDelete: () -> Unit,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, EditorialPurple.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EditorialPurple.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = EditorialPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = character.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (character.roleOrLocation.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(EditorialPurple.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = character.roleOrLocation,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = EditorialPurple,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                if (onEdit != null) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar personaje",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Eliminar personaje",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Motivation
            if (character.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🎯 Motivación / Deseo:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldLight,
                    fontSize = 11.sp
                )
                Text(
                    text = character.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp
                )
            }

            // Internal Conflict / Flaw / Arc
            if (character.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "⚡ Conflicto Interno & Arco:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = EditorialAmber,
                    fontSize = 11.sp
                )
                Text(
                    text = character.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            // Appearance & Quirks
            if (character.appearanceOrDetails.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "👁️ Rasgos Distintivos:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = EditorialCyan,
                    fontSize = 11.sp
                )
                Text(
                    text = character.appearanceOrDetails,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun SettingProfileCard(
    setting: StoryBibleEntity,
    onDelete: () -> Unit,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, EditorialCyan.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EditorialCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = EditorialCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = setting.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (setting.roleOrLocation.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(EditorialCyan.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = setting.roleOrLocation,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = EditorialCyan,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                if (onEdit != null) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar escenario",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Eliminar escenario",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Sensory Atmosphere
            if (setting.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🌿 Atmósfera Sensorial & Entorno:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldLight,
                    fontSize = 11.sp
                )
                Text(
                    text = setting.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp
                )
            }

            // World Lore & Rules
            if (setting.appearanceOrDetails.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "📜 Lore & Reglas del Mundo:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = EditorialCyan,
                    fontSize = 11.sp
                )
                Text(
                    text = setting.appearanceOrDetails,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            // Secrets & Hazards
            if (setting.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "⚠️ Secretos o Peligros Ocultos:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = EditorialAmber,
                    fontSize = 11.sp
                )
                Text(
                    text = setting.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun SubplotProfileCard(
    subplot: StoryBibleEntity,
    onDelete: () -> Unit,
    onCycleStatus: () -> Unit,
    onEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val statusColor = when (subplot.status) {
        "RESUELTA" -> EmeraldPrimary
        "CLIMAX" -> EditorialRose
        "EN_DESARROLLO" -> EditorialCyan
        else -> EditorialAmber // PLANTEADA
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, EditorialAmber.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EditorialAmber.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = EditorialAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subplot.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Clickable status pill to cycle subplot state
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusColor.copy(alpha = 0.2f))
                                .clickable { onCycleStatus() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = subplot.status.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = statusColor,
                                fontSize = 10.sp
                            )
                        }

                        if (subplot.roleOrLocation.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${subplot.roleOrLocation}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                if (onEdit != null) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar trama",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Eliminar trama",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Conflict & Stakes
            if (subplot.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🔥 Conflicto & Lo que está en juego:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = EditorialAmber,
                    fontSize = 11.sp
                )
                Text(
                    text = subplot.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp
                )
            }

            // Characters Involved
            if (subplot.appearanceOrDetails.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "👥 Personajes implicados:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = EditorialPurple,
                    fontSize = 11.sp
                )
                Text(
                    text = subplot.appearanceOrDetails,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            // Resolution / Notes
            if (subplot.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "🏁 Resolución prevista / Pistas:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldLight,
                    fontSize = 11.sp
                )
                Text(
                    text = subplot.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun BibleEntryItemCard(
    entry: StoryBibleEntity,
    onDelete: () -> Unit,
    onCycleStatus: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (entry.category) {
        "PERSONAJE" -> CharacterProfileCard(character = entry, onDelete = onDelete, modifier = modifier)
        "ESCENARIO" -> SettingProfileCard(setting = entry, onDelete = onDelete, modifier = modifier)
        "TRAMA_SECUNDARIA" -> SubplotProfileCard(subplot = entry, onDelete = onDelete, onCycleStatus = onCycleStatus, modifier = modifier)
        else -> {
            Card(
                modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(entry.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (entry.description.isNotBlank()) Text(entry.description, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// Dialogs
@Composable
fun CreateChapterDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, summary: String, pov: String, targetWords: Int, notes: String) -> Unit,
    availableCharacters: List<String> = emptyList()
) {
    var title by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var pov by remember { mutableStateOf("") }
    var targetWordsText by remember { mutableStateOf("2000") }
    var sceneObjective by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar Nuevo Capítulo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título del capítulo (ej. El Despertar)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("chapter_title_input")
                )

                // POV character selection
                OutlinedTextField(
                    value = pov,
                    onValueChange = { pov = it },
                    label = { Text("Personaje POV (Punto de Vista)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (availableCharacters.isNotEmpty()) {
                    Text("Sugerencias de personajes:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        availableCharacters.take(4).forEach { charName ->
                            FilterChip(
                                selected = pov == charName,
                                onClick = { pov = charName },
                                label = { Text(charName, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = targetWordsText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) targetWordsText = it },
                    label = { Text("Meta de palabras del capítulo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sceneObjective,
                    onValueChange = { sceneObjective = it },
                    label = { Text("Objetivo de escena / gancho dramático") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Resumen preliminar de eventos") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val words = targetWordsText.toIntOrNull() ?: 2000
                        onCreate(title, summary, pov, words, sceneObjective)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_create_chapter")
            ) {
                Text("Crear Capítulo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun EditChapterMetadataDialog(
    chapter: ChapterEntity,
    onDismiss: () -> Unit,
    onSave: (title: String, summary: String, pov: String, targetWords: Int, notes: String) -> Unit,
    availableCharacters: List<String> = emptyList()
) {
    var title by remember { mutableStateOf(chapter.title) }
    var summary by remember { mutableStateOf(chapter.summary) }
    var pov by remember { mutableStateOf(chapter.povCharacter) }
    var targetWordsText by remember { mutableStateOf(chapter.targetWords.toString()) }
    var notes by remember { mutableStateOf(chapter.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gestionar Capítulo ${chapter.chapterNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pov,
                    onValueChange = { pov = it },
                    label = { Text("Punto de Vista (POV)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (availableCharacters.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        availableCharacters.take(4).forEach { charName ->
                            FilterChip(
                                selected = pov == charName,
                                onClick = { pov = charName },
                                label = { Text(charName, fontSize = 10.sp) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = targetWordsText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) targetWordsText = it },
                    label = { Text("Meta de palabras") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Objetivo de escena / conflicto") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Resumen de trama") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val words = targetWordsText.toIntOrNull() ?: 2000
                    onSave(title, summary, pov, words, notes)
                    onDismiss()
                }
            ) {
                Text("Guardar Cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun CreateCharacterDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, role: String, motivation: String, appearance: String, flawAndArc: String) -> Unit
) {
    val archetypes = listOf("Protagonista", "Antagonista", "Secundario Clave", "Mentor", "Aliado", "Rival")
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(archetypes[0]) }
    var motivation by remember { mutableStateOf("") }
    var appearance by remember { mutableStateOf("") }
    var flawAndArc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Perfil de Personaje") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del personaje") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("character_name_input")
                )

                Text("Rol / Arquetipo narrativo:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    archetypes.take(3).forEach { arc ->
                        FilterChip(
                            selected = role == arc,
                            onClick = { role = arc },
                            label = { Text(arc, fontSize = 10.sp) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    archetypes.drop(3).forEach { arc ->
                        FilterChip(
                            selected = role == arc,
                            onClick = { role = arc },
                            label = { Text(arc, fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = motivation,
                    onValueChange = { motivation = it },
                    label = { Text("Motivación principal (¿Qué desea?)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = flawAndArc,
                    onValueChange = { flawAndArc = it },
                    label = { Text("Defecto trágico, conflicto interno o arco") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = appearance,
                    onValueChange = { appearance = it },
                    label = { Text("Rasgos físicos y voz distintiva") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name, role, motivation, appearance, flawAndArc)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_create_character")
            ) {
                Text("Guardar Personaje")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun CreateSettingDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, locationType: String, atmosphere: String, worldRules: String, secrets: String) -> Unit
) {
    val types = listOf("Ciudad", "Fortaleza", "Reino", "Naturaleza", "Interior")
    var name by remember { mutableStateOf("") }
    var locationType by remember { mutableStateOf(types[0]) }
    var atmosphere by remember { mutableStateOf("") }
    var worldRules by remember { mutableStateOf("") }
    var secrets by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Escenario & Lore") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del lugar / región") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("setting_name_input")
                )

                Text("Tipo de escenario:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    types.forEach { t ->
                        FilterChip(
                            selected = locationType == t,
                            onClick = { locationType = t },
                            label = { Text(t, fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = atmosphere,
                    onValueChange = { atmosphere = it },
                    label = { Text("Atmósfera sensorial (olores, sonidos, luz)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = worldRules,
                    onValueChange = { worldRules = it },
                    label = { Text("Reglas del mundo & historia del lugar") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = secrets,
                    onValueChange = { secrets = it },
                    label = { Text("Peligros o secretos ocultos") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name, locationType, atmosphere, worldRules, secrets)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_create_setting")
            ) {
                Text("Guardar Escenario")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun CreateSubplotDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, subplotType: String, conflictStakes: String, charactersInvolved: String, resolutionNotes: String, status: String) -> Unit
) {
    val types = listOf("Misterio", "Rivalidad", "Romance", "Traición", "Conspiración")
    var title by remember { mutableStateOf("") }
    var subplotType by remember { mutableStateOf(types[0]) }
    var conflictStakes by remember { mutableStateOf("") }
    var charactersInvolved by remember { mutableStateOf("") }
    var resolutionNotes by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("PLANTEADA") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Trama Secundaria (Subplot)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título de la subtrama") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("subplot_title_input")
                )

                Text("Tipo de conflicto:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    types.take(3).forEach { t ->
                        FilterChip(
                            selected = subplotType == t,
                            onClick = { subplotType = t },
                            label = { Text(t, fontSize = 10.sp) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    types.drop(3).forEach { t ->
                        FilterChip(
                            selected = subplotType == t,
                            onClick = { subplotType = t },
                            label = { Text(t, fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = conflictStakes,
                    onValueChange = { conflictStakes = it },
                    label = { Text("Conflicto central (¿Qué está en juego?)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = charactersInvolved,
                    onValueChange = { charactersInvolved = it },
                    label = { Text("Personajes involucrados") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = resolutionNotes,
                    onValueChange = { resolutionNotes = it },
                    label = { Text("Resolución prevista o giro dramático") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreate(title, subplotType, conflictStakes, charactersInvolved, resolutionNotes, status)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_create_subplot")
            ) {
                Text("Guardar Trama")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun CreateBibleEntryDialog(
    onDismiss: () -> Unit,
    onCreate: (category: String, name: String, role: String, desc: String, notes: String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    when (selectedTab) {
        0 -> CreateCharacterDialog(
            onDismiss = onDismiss,
            onCreate = { name, role, mot, app, flaw ->
                onCreate("PERSONAJE", name, role, mot, "$flaw | $app")
            }
        )
    }
}

/**
 * Diálogo de creación de una novela.
 *
 * Faltaba por completo: [StoryProjectsScreen] lo importaba y el proyecto no
 * compilaba. Incluye género, sinopsis, meta de palabras y color identificador.
 */
@Composable
fun CreateStoryDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, genre: String, synopsis: String, targetWords: Int, colorHex: String) -> Unit
) {
    val genres = listOf("Fantasía", "Ciencia Ficción", "Romance", "Novela Negra", "Histórico", "Thriller", "Juvenil")
    val targets = listOf(50000, 80000, 100000, 120000)
    val palette = listOf("#10B981", "#38BDF8", "#8B5CF6", "#F59E0B", "#F43F5E", "#14B8A6")

    var title by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf(genres[0]) }
    var synopsis by remember { mutableStateOf("") }
    var targetWords by remember { mutableIntStateOf(80000) }
    var colorHex by remember { mutableStateOf(palette[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Novela") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título de la obra") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("story_title_input")
                )

                OutlinedTextField(
                    value = synopsis,
                    onValueChange = { synopsis = it },
                    label = { Text("Sinopsis o premisa (opcional)") },
                    modifier = Modifier.fillMaxWidth().height(96.dp).testTag("story_synopsis_input")
                )

                Text("Género:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    genres.take(4).forEach { g ->
                        FilterChip(
                            selected = genre == g,
                            onClick = { genre = g },
                            label = { Text(g, fontSize = 10.sp) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    genres.drop(4).forEach { g ->
                        FilterChip(
                            selected = genre == g,
                            onClick = { genre = g },
                            label = { Text(g, fontSize = 10.sp) }
                        )
                    }
                }

                Text("Meta de palabras:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    targets.forEach { t ->
                        FilterChip(
                            selected = targetWords == t,
                            onClick = { targetWords = t },
                            label = { Text("${t / 1000}k", fontSize = 10.sp) }
                        )
                    }
                }

                Text("Color identificador:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    palette.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (colorHex == hex) 2.dp else 0.dp,
                                    color = if (colorHex == hex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreate(title.trim(), genre, synopsis.trim(), targetWords, colorHex)
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_story")
            ) {
                Text("Crear Novela")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

/**
 * Edición de una ficha narrativa existente.
 *
 * El ViewModel ya tenía `updateBibleEntry`, pero ninguna pantalla lo llamaba:
 * las fichas solo se podían crear y borrar.
 */
@Composable
fun EditBibleEntryDialog(
    entry: StoryBibleEntity,
    onDismiss: () -> Unit,
    onSave: (name: String, role: String, description: String, details: String, notes: String, status: String) -> Unit
) {
    var name by remember { mutableStateOf(entry.name) }
    var role by remember { mutableStateOf(entry.roleOrLocation) }
    var description by remember { mutableStateOf(entry.description) }
    var details by remember { mutableStateOf(entry.appearanceOrDetails) }
    var notes by remember { mutableStateOf(entry.notes) }
    var status by remember { mutableStateOf(entry.status) }

    val (roleLabel, descLabel, detailsLabel, notesLabel) = when (entry.category) {
        "PERSONAJE" -> listOf("Rol / Arquetipo", "Motivación", "Apariencia", "Defecto y arco")
        "ESCENARIO" -> listOf("Tipo de lugar", "Atmósfera", "Reglas del mundo", "Secretos")
        "TRAMA_SECUNDARIA" -> listOf("Tipo de trama", "Conflicto y apuestas", "Personajes implicados", "Resolución / notas")
        else -> listOf("Rol", "Descripción", "Detalles", "Notas")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar ficha: ${entry.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_bible_name")
                )
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text(roleLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(descLabel) },
                    modifier = Modifier.fillMaxWidth().height(88.dp)
                )
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text(detailsLabel) },
                    modifier = Modifier.fillMaxWidth().height(88.dp)
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(notesLabel) },
                    modifier = Modifier.fillMaxWidth().height(88.dp)
                )
                OutlinedTextField(
                    value = status,
                    onValueChange = { status = it },
                    label = { Text("Estado") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), role.trim(), description.trim(), details.trim(), notes.trim(), status.trim())
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Guardar cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
