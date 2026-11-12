package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChronicleConflictEntity
import com.example.data.local.ChronicleDocumentEntity
import com.example.data.local.ChronicleEntityEntity
import com.example.data.local.ChronicleFactEntity
import com.example.data.local.ChronicleRelationshipEntity
import com.example.data.local.ChronicleSyncHistoryEntity
import com.example.domain.chronicle.EntityType
import com.example.domain.chronicle.SourceType
import com.example.ui.MainViewModel
import com.example.ui.components.CodeBlockText
import com.example.ui.components.ConsoleCard
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CrimsonLaser
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceDarkElevated
import com.example.ui.theme.SurfaceDarkVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChronicleScreen(
    viewModel: MainViewModel,
    initialTab: Int = 0
) {
    val selectedAgent by viewModel.selectedAgent.collectAsState()
    val allAgents by viewModel.agents.collectAsState()
    val entities by viewModel.chronicleEntities.collectAsState()
    val relationships by viewModel.chronicleRelationships.collectAsState()
    val facts by viewModel.chronicleFacts.collectAsState()
    val conflicts by viewModel.chronicleConflicts.collectAsState()
    val documents by viewModel.chronicleDocuments.collectAsState()
    val syncHistory by viewModel.chronicleSyncHistory.collectAsState()
    val selectedDoc by viewModel.selectedChronicleDoc.collectAsState()
    val isProcessing by viewModel.isChronicleProcessing.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(initialTab) }
    var showAgentDropdown by remember { mutableStateOf(false) }
    var showIngestDialog by remember { mutableStateOf(false) }
    var showNewDocDialog by remember { mutableStateOf(false) }

    val tabs = listOf(
        "Overview",
        "Knowledge Graph",
        "Timeline",
        "Facts (${facts.size})",
        "Conflicts (${conflicts.size})",
        "Markdown Docs (${documents.size})",
        "Sync & Audit"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .testTag("chronicle_screen")
    ) {
        // Agent Context Header
        Surface(
            color = SurfaceDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LETTA CHRONICLE",
                        color = ElectricCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { showAgentDropdown = true }
                            .testTag("chronicle_agent_dropdown")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = EmeraldNeon,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedAgent?.name ?: "Select Agent",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = TextMuted
                        )
                    }

                    DropdownMenu(
                        expanded = showAgentDropdown,
                        onDismissRequest = { showAgentDropdown = false }
                    ) {
                        allAgents.forEach { agent ->
                            DropdownMenuItem(
                                text = { Text(agent.name) },
                                onClick = {
                                    viewModel.selectAgent(agent.id)
                                    showAgentDropdown = false
                                }
                            )
                        }
                    }
                }

                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = ElectricCyan,
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = {
                            selectedAgent?.let {
                                viewModel.loadChronicleForAgent(it.id, it.name)
                            }
                        },
                        modifier = Modifier.testTag("chronicle_refresh_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextMuted)
                    }
                }
            }
        }

        // Sub Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = SurfaceDarkElevated,
            contentColor = ElectricCyan,
            edgePadding = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == index) ElectricCyan else TextMuted
                        )
                    },
                    modifier = Modifier.testTag("chronicle_tab_$index")
                )
            }
        }

        // Screen Body
        if (selectedAgent == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.AccountTree,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Agent Selected",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select an active Letta agent from the top bar to inspect its Temporal Knowledge Graph and Markdown memory files.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            val agent = selectedAgent!!
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTabIndex) {
                    0 -> ChronicleOverviewTab(
                        agentName = agent.name,
                        entities = entities,
                        relationships = relationships,
                        facts = facts,
                        conflicts = conflicts,
                        documents = documents,
                        syncHistory = syncHistory,
                        onNavigateToTab = { selectedTabIndex = it }
                    )
                    1 -> ChronicleGraphTab(
                        entities = entities,
                        relationships = relationships,
                        facts = facts
                    )
                    2 -> ChronicleTimelineTab(facts = facts)
                    3 -> ChronicleFactsTab(
                        agentId = agent.id,
                        facts = facts,
                        onIngestClick = { showIngestDialog = true },
                        onArchiveFact = { factId -> viewModel.archiveChronicleFact(agent.id, factId) }
                    )
                    4 -> ChronicleConflictsTab(
                        conflicts = conflicts,
                        onResolve = { conflictId, choice ->
                            viewModel.resolveChronicleConflict(conflictId, choice)
                        }
                    )
                    5 -> ChronicleDocumentsTab(
                        documents = documents,
                        selectedDoc = selectedDoc,
                        onSelectDoc = { viewModel.selectChronicleDocument(it) },
                        onCreateDoc = { showNewDocDialog = true },
                        onUpdateDoc = { docId, content, summary ->
                            viewModel.updateChronicleDocument(docId, content, summary)
                        },
                        onDeleteDoc = { viewModel.deleteChronicleDocument(it) },
                        onCompileGraph = { viewModel.compileGraphToDocument(agent.id) }
                    )
                    6 -> ChronicleSyncTab(
                        facts = facts,
                        syncHistory = syncHistory,
                        onSyncToCore = { blockLabel ->
                            viewModel.syncChronicleToCoreMemory(agent.id, blockLabel)
                        }
                    )
                }
            }
        }
    }

    // Ingest Fact Dialog
    if (showIngestDialog && selectedAgent != null) {
        IngestFactDialog(
            onDismiss = { showIngestDialog = false },
            onIngest = { subject, predicate, objectVal, sourceRef ->
                viewModel.ingestChronicleFact(
                    agentId = selectedAgent!!.id,
                    subject = subject,
                    predicate = predicate,
                    objectValue = objectVal,
                    statement = "$subject $predicate $objectVal",
                    confidence = 0.95f,
                    sourceRef = sourceRef,
                    sourceType = SourceType.USER_MESSAGE.name
                )
                showIngestDialog = false
            }
        )
    }

    // Create Document Dialog
    if (showNewDocDialog && selectedAgent != null) {
        CreateDocDialog(
            onDismiss = { showNewDocDialog = false },
            onCreate = { title, fileName, content ->
                viewModel.createChronicleDocument(selectedAgent!!.id, title, fileName, content)
                showNewDocDialog = false
            }
        )
    }
}

// -------------------------------------------------------------
// 0. OVERVIEW TAB
// -------------------------------------------------------------
@Composable
fun ChronicleOverviewTab(
    agentName: String,
    entities: List<ChronicleEntityEntity>,
    relationships: List<ChronicleRelationshipEntity>,
    facts: List<ChronicleFactEntity>,
    conflicts: List<ChronicleConflictEntity>,
    documents: List<ChronicleDocumentEntity>,
    syncHistory: List<ChronicleSyncHistoryEntity>,
    onNavigateToTab: (Int) -> Unit
) {
    val activeFacts = facts.filter { it.validTo == null }
    val historicalFacts = facts.filter { it.validTo != null }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ConsoleCard {
                Text(
                    text = "TEMPORAL KNOWLEDGE GRAPH STATUS",
                    color = ElectricCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "LettaChronicle maintains temporal duality for '$agentName'. Every statement is bound to a Valid Time window and recorded at Transaction Time with non-destructive versioning.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricMiniCard(
                    title = "Entities",
                    count = entities.size.toString(),
                    color = ElectricCyan,
                    modifier = Modifier.weight(1f).clickable { onNavigateToTab(1) }
                )
                MetricMiniCard(
                    title = "Relationships",
                    count = relationships.size.toString(),
                    color = EmeraldNeon,
                    modifier = Modifier.weight(1f).clickable { onNavigateToTab(1) }
                )
                MetricMiniCard(
                    title = "Active Facts",
                    count = activeFacts.size.toString(),
                    color = EmeraldNeon,
                    modifier = Modifier.weight(1f).clickable { onNavigateToTab(3) }
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricMiniCard(
                    title = "Superseded",
                    count = historicalFacts.size.toString(),
                    color = TextMuted,
                    modifier = Modifier.weight(1f).clickable { onNavigateToTab(2) }
                )
                MetricMiniCard(
                    title = "Open Conflicts",
                    count = conflicts.size.toString(),
                    color = if (conflicts.isNotEmpty()) CrimsonLaser else TextMuted,
                    modifier = Modifier.weight(1f).clickable { onNavigateToTab(4) }
                )
                MetricMiniCard(
                    title = "Markdown Docs",
                    count = documents.size.toString(),
                    color = AmberGlow,
                    modifier = Modifier.weight(1f).clickable { onNavigateToTab(5) }
                )
            }
        }

        // Quick Conflict Warning
        if (conflicts.isNotEmpty()) {
            item {
                ConsoleCard(borderColor = CrimsonLaser) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = CrimsonLaser)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${conflicts.size} Temporal Conflict(s) Detected",
                            color = CrimsonLaser,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Contradictory or overlapping facts require operator review before syncing to Letta Core Memory.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onNavigateToTab(4) },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonLaser)
                    ) {
                        Text("Review Conflicts", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }

        // Recent Sync Status
        item {
            ConsoleCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CORE MEMORY SYNC AUDIT",
                        color = ElectricCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { onNavigateToTab(6) }) {
                        Text("View Sync Tab", fontSize = 11.sp, color = ElectricCyan)
                    }
                }
                if (syncHistory.isEmpty()) {
                    Text(
                        text = "No sync events recorded yet. Go to 'Sync & Audit' to synthesize active facts into Letta Core Memory.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                } else {
                    val latest = syncHistory.first()
                    Text(
                        text = "Latest Sync: ${formatTime(latest.timestamp)} • Target: ${latest.blockLabel}",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Status: ${latest.syncStatus} • Facts included: ${latest.factsCount}",
                        color = if (latest.syncStatus == "SUCCESS") EmeraldNeon else CrimsonLaser,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MetricMiniCard(
    title: String,
    count: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = count, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, color = TextMuted, fontSize = 11.sp)
        }
    }
}

// -------------------------------------------------------------
// 1. KNOWLEDGE GRAPH CANVAS TAB
// -------------------------------------------------------------
@Composable
fun ChronicleGraphTab(
    entities: List<ChronicleEntityEntity>,
    relationships: List<ChronicleRelationshipEntity>,
    facts: List<ChronicleFactEntity>
) {
    var selectedEntity by remember { mutableStateOf<ChronicleEntityEntity?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 3.0f)
        panOffset += panChange
    }

    if (entities.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Knowledge Graph is currently empty. Ingest facts in the 'Facts' tab to construct the graph.",
                color = TextMuted,
                fontSize = 13.sp
            )
        }
        return
    }

    // Assign positions in a circle/orbit layout
    val nodePositions = remember(entities) {
        val map = mutableMapOf<String, Offset>()
        val count = entities.size
        val radius = 220f
        val centerX = 400f
        val centerY = 400f

        entities.forEachIndexed { i, e ->
            val angle = (2.0 * Math.PI * i / count.coerceAtLeast(1)).toFloat()
            val x = centerX + radius * cos(angle)
            val y = centerY + radius * sin(angle)
            map[e.entityId] = Offset(x, y)
        }
        map
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = transformState)
                .pointerInput(entities) {
                    detectTapGestures { tapOffset ->
                        val localOffset = (tapOffset - panOffset) / scale
                        var hit: ChronicleEntityEntity? = null
                        for (e in entities) {
                            val pos = nodePositions[e.entityId] ?: continue
                            val dist = (pos - localOffset).getDistance()
                            if (dist < 40f) {
                                hit = e
                                break
                            }
                        }
                        selectedEntity = hit
                    }
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = panOffset.x,
                    translationY = panOffset.y
                )
        ) {
            // Draw Relationship Edges
            relationships.forEach { rel ->
                val start = nodePositions[rel.sourceEntityId]
                val end = nodePositions[rel.targetEntityId]
                if (start != null && end != null) {
                    val edgeColor = if (rel.status == "ACTIVE") ElectricCyan.copy(alpha = 0.6f) else TextMuted.copy(alpha = 0.3f)
                    drawLine(
                        color = edgeColor,
                        start = start,
                        end = end,
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Draw Nodes
            entities.forEach { entity ->
                val pos = nodePositions[entity.entityId] ?: return@forEach
                val isSelected = selectedEntity?.entityId == entity.entityId
                val nodeColor = when (entity.entityType) {
                    EntityType.PERSON.name -> ElectricCyan
                    EntityType.CONCEPT.name -> AmberGlow
                    EntityType.PROJECT.name -> Color(0xFFBD93F9)
                    else -> Color(0xFFFF79C6)
                }

                // Outer halo if selected
                if (isSelected) {
                    drawCircle(
                        color = ElectricCyan.copy(alpha = 0.35f),
                        radius = 32f,
                        center = pos
                    )
                }

                // Base node circle
                drawCircle(
                    color = nodeColor,
                    radius = 20f,
                    center = pos
                )
                drawCircle(
                    color = VoidBlack,
                    radius = 16f,
                    center = pos
                )
                drawCircle(
                    color = nodeColor,
                    radius = 8f,
                    center = pos
                )
            }
        }

        // Selected Node Inspection Drawer
        AnimatedVisibility(
            visible = selectedEntity != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            selectedEntity?.let { entity ->
                Surface(
                    color = SurfaceDarkElevated,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = entity.canonicalName,
                                    color = ElectricCyan,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Type: ${entity.entityType} • ID: ${entity.entityId.take(8)}",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(onClick = { selectedEntity = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        val relatedFacts = facts.filter { it.subject == entity.canonicalName || it.objectValue == entity.canonicalName }
                        Text(
                            text = "Linked Facts (${relatedFacts.size}):",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        relatedFacts.take(3).forEach { f ->
                            val isValid = f.validTo == null
                            Text(
                                text = "• ${f.subject} ${f.predicate} ${f.objectValue} (${if (isValid) "ACTIVE" else "SUPERSEDED"})",
                                color = if (isValid) TextPrimary else TextMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. TIMELINE TAB
// -------------------------------------------------------------
@Composable
fun ChronicleTimelineTab(facts: List<ChronicleFactEntity>) {
    if (facts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No temporal facts recorded yet.", color = TextMuted, fontSize = 13.sp)
        }
        return
    }

    val sorted = facts.sortedByDescending { it.validFrom ?: it.recordedAt }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(sorted) { fact ->
            val isActive = fact.validTo == null
            ConsoleCard(
                borderColor = if (isActive) EmeraldNeon else BorderSubtle
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (isActive) EmeraldNeon.copy(alpha = 0.15f) else SurfaceDarkVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (isActive) "ACTIVE VALID" else "HISTORICAL SUPERSEDED",
                            color = if (isActive) EmeraldNeon else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "Confidence: ${(fact.confidence * 100).toInt()}%",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${fact.subject} ${fact.predicate.lowercase().replace('_', ' ')} ${fact.objectValue}",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${fact.subject} ➔ [${fact.predicate}] ➔ ${fact.objectValue}",
                    color = ElectricCyan,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Valid: ${formatTime(fact.validFrom)}${if (fact.validTo != null) " to ${formatTime(fact.validTo)}" else " (Current)"}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Recorded: ${formatTime(fact.recordedAt)}",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Source: ${fact.sourceType} (${fact.source})",
                    color = TextMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 3. FACTS TAB
// -------------------------------------------------------------
@Composable
fun ChronicleFactsTab(
    agentId: String,
    facts: List<ChronicleFactEntity>,
    onIngestClick: () -> Unit,
    onArchiveFact: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterActiveOnly by remember { mutableStateOf(false) }

    val filtered = facts.filter { f ->
        (if (filterActiveOnly) f.validTo == null else true) &&
                (searchQuery.isBlank() ||
                        f.subject.contains(searchQuery, ignoreCase = true) ||
                        f.predicate.contains(searchQuery, ignoreCase = true) ||
                        f.objectValue.contains(searchQuery, ignoreCase = true))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search facts by subject, predicate, or object...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (filterActiveOnly) ElectricCyan else SurfaceDark,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .clickable { filterActiveOnly = !filterActiveOnly }
                            .padding(4.dp)
                    ) {
                        Text(
                            text = if (filterActiveOnly) "Active Facts Only" else "All (Including Superseded)",
                            color = if (filterActiveOnly) VoidBlack else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "${filtered.size} facts found",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered) { fact ->
                    val isActive = fact.validTo == null
                    ConsoleCard(borderColor = if (isActive) BorderSubtle else Color.Transparent) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${fact.subject} • ${fact.predicate}",
                                color = ElectricCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { onArchiveFact(fact.factId) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Archive,
                                    contentDescription = "Archive to Letta Vector Memory",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${fact.subject} ${fact.predicate.lowercase().replace('_', ' ')} ${fact.objectValue}",
                            color = TextPrimary,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Object: ${fact.objectValue}",
                            color = AmberGlow,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Status: ${if (isActive) "ACTIVE" else "SUPERSEDED"} • Source: ${fact.source}",
                            color = if (isActive) EmeraldNeon else TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onIngestClick,
            containerColor = ElectricCyan,
            contentColor = VoidBlack,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("ingest_fact_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Ingest Fact")
        }
    }
}

// -------------------------------------------------------------
// 4. CONFLICTS TAB
// -------------------------------------------------------------
@Composable
fun ChronicleConflictsTab(
    conflicts: List<ChronicleConflictEntity>,
    onResolve: (conflictId: String, choice: String) -> Unit
) {
    if (conflicts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldNeon,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No Active Conflicts",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "All facts in the temporal knowledge graph are consistent and unambiguous.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(conflicts) { conflict ->
            ConsoleCard(borderColor = CrimsonLaser) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = CrimsonLaser, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = conflict.conflictType,
                        color = CrimsonLaser,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = conflict.description,
                    color = TextPrimary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Resolution Actions:",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onResolve(conflict.conflictId, "RESOLVED_A") },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Accept A", color = VoidBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onResolve(conflict.conflictId, "RESOLVED_B") },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberGlow),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Accept B", color = VoidBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onResolve(conflict.conflictId, "DISMISSED") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Dismiss", color = TextMuted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 5. DOCUMENTS & EDITOR TAB
// -------------------------------------------------------------
@Composable
fun ChronicleDocumentsTab(
    documents: List<ChronicleDocumentEntity>,
    selectedDoc: ChronicleDocumentEntity?,
    onSelectDoc: (String) -> Unit,
    onCreateDoc: () -> Unit,
    onUpdateDoc: (docId: String, newContent: String, summary: String) -> Unit,
    onDeleteDoc: (String) -> Unit,
    onCompileGraph: () -> Unit
) {
    var editContent by remember(selectedDoc) { mutableStateOf(selectedDoc?.content ?: "") }
    var changeSummary by remember { mutableStateOf("Updated via Chronicle Editor") }
    var isEditing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Document selection header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MARKDOWN MEMORY DOCUMENTS",
                color = ElectricCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = onCompileGraph) {
                    Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Compile Graph", fontSize = 11.sp, color = ElectricCyan)
                }

                Button(
                    onClick = onCreateDoc,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = VoidBlack, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Doc", fontSize = 11.sp, color = VoidBlack, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Document tabs
        ScrollableTabRow(
            selectedTabIndex = documents.indexOfFirst { it.documentId == selectedDoc?.documentId }.coerceAtLeast(0),
            containerColor = SurfaceDark,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            documents.forEach { doc ->
                val isSelected = doc.documentId == selectedDoc?.documentId
                Tab(
                    selected = isSelected,
                    onClick = {
                        onSelectDoc(doc.documentId)
                        isEditing = false
                    },
                    text = {
                        Text(
                            text = "${doc.fileName} (Rev ${doc.knowledgeRevision})",
                            color = if (isSelected) ElectricCyan else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedDoc == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No document selected. Tap an existing document or create a new one.", color = TextMuted, fontSize = 13.sp)
            }
        } else {
            // Editor / Viewer Panel
            ConsoleCard(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = selectedDoc.title,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "File: ${selectedDoc.fileName} • Rev ${selectedDoc.knowledgeRevision} • Updated: ${formatTime(selectedDoc.updatedAt)}",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }

                        Row {
                            IconButton(onClick = { isEditing = !isEditing }) {
                                Icon(
                                    if (isEditing) Icons.Default.Description else Icons.Default.Edit,
                                    contentDescription = "Toggle Edit",
                                    tint = ElectricCyan
                                )
                            }
                            IconButton(onClick = { onDeleteDoc(selectedDoc.documentId) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Doc", tint = CrimsonLaser)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isEditing) {
                        OutlinedTextField(
                            value = editContent,
                            onValueChange = { editContent = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = TextPrimary
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = BorderSubtle
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = changeSummary,
                                onValueChange = { changeSummary = it },
                                placeholder = { Text("Change summary...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricCyan,
                                    unfocusedBorderColor = BorderSubtle
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    onUpdateDoc(selectedDoc.documentId, editContent, changeSummary)
                                    isEditing = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, tint = VoidBlack)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save", color = VoidBlack, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f)) {
                            CodeBlockText(
                                text = selectedDoc.content,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 6. SYNC & AUDIT TAB
// -------------------------------------------------------------
@Composable
fun ChronicleSyncTab(
    facts: List<ChronicleFactEntity>,
    syncHistory: List<ChronicleSyncHistoryEntity>,
    onSyncToCore: (String) -> Unit
) {
    var blockLabel by remember { mutableStateOf("human") }
    val activeFacts = facts.filter { it.validTo == null }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ConsoleCard {
                Text(
                    text = "SYNCHRONIZE TO LETTA CORE MEMORY",
                    color = ElectricCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "LettaChronicle converts currently active facts into a compact, structured Markdown memory block and sends a real PATCH request to Letta's Core Memory API.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = blockLabel,
                    onValueChange = { blockLabel = it },
                    label = { Text("Core Memory Block Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = BorderSubtle
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onSyncToCore(blockLabel) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sync_to_core_button")
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = VoidBlack)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sync ${activeFacts.size} Active Facts to Letta Core Memory",
                        color = VoidBlack,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            Text(
                text = "AUDIT HISTORY",
                color = ElectricCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (syncHistory.isEmpty()) {
            item {
                Text(
                    text = "No sync transactions logged yet.",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        } else {
            items(syncHistory) { record ->
                val isSuccess = record.syncStatus == "SUCCESS"
                ConsoleCard(borderColor = if (isSuccess) BorderSubtle else CrimsonLaser) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Target: ${record.blockLabel}",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = record.syncStatus,
                            color = if (isSuccess) EmeraldNeon else CrimsonLaser,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Timestamp: ${formatTime(record.timestamp)} • Facts: ${record.factsCount}",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    if (record.details.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Details: ${record.details}",
                            color = if (isSuccess) TextSecondary else CrimsonLaser,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DIALOGS
// -------------------------------------------------------------
@Composable
fun IngestFactDialog(
    onDismiss: () -> Unit,
    onIngest: (
        subject: String,
        predicate: String,
        obj: String,
        sourceRef: String
    ) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var predicate by remember { mutableStateOf("") }
    var obj by remember { mutableStateOf("") }
    var sourceRef by remember { mutableStateOf("Console operator input") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ingest Fact to Chronicle", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject (e.g. User, ProjectAlpha)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = predicate,
                    onValueChange = { predicate = it },
                    label = { Text("Predicate (e.g. PREFERS_LANGUAGE, ASSIGNED_TO)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = obj,
                    onValueChange = { obj = it },
                    label = { Text("Object (e.g. Kotlin, Alice)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sourceRef,
                    onValueChange = { sourceRef = it },
                    label = { Text("Source Reference (Provenance)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subject.isNotBlank() && predicate.isNotBlank() && obj.isNotBlank()) {
                        onIngest(
                            subject,
                            predicate,
                            obj,
                            sourceRef
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
            ) {
                Text("Ingest", color = VoidBlack, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        },
        containerColor = SurfaceDark
    )
}

@Composable
fun CreateDocDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, fileName: String, content: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("# Document Title\n\nWrite knowledge notes here...") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Markdown Memory Document", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (fileName.isBlank()) fileName = "${it.lowercase().replace(" ", "_").filter { c -> c.isLetterOrDigit() || c == '_' }}.md"
                    },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File Name (e.g. notes.md)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Initial Content") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && fileName.isNotBlank()) {
                        onCreate(title, fileName, content)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
            ) {
                Text("Create", color = VoidBlack, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        },
        containerColor = SurfaceDark
    )
}

private fun formatTime(millis: Long?): String {
    if (millis == null) return "N/A"
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}
