package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.LettaAgentDto
import com.example.ui.MainViewModel
import com.example.ui.components.CodeBlockText
import com.example.ui.components.ConsoleCard
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CrimsonLaser
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceDarkElevated
import com.example.ui.theme.SurfaceDarkVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack

@Composable
fun AgentsScreen(
    viewModel: MainViewModel,
    onNavigateToChat: (String) -> Unit
) {
    val agents by viewModel.agents.collectAsState()
    val isLoading by viewModel.isLoadingAgents.collectAsState()
    val errorMessage by viewModel.agentErrorMessage.collectAsState()
    val selectedAgent by viewModel.selectedAgent.collectAsState()
    val coreBlocks by viewModel.coreMemoryBlocks.collectAsState()
    val archivalItems by viewModel.archivalItems.collectAsState()
    val tools by viewModel.tools.collectAsState()
    val sources by viewModel.sources.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var agentToDelete by remember { mutableStateOf<LettaAgentDto?>(null) }
    var workspaceAgent by remember { mutableStateOf<LettaAgentDto?>(null) }

    val filteredAgents = remember(agents, searchQuery) {
        if (searchQuery.isBlank()) agents
        else agents.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            (it.description?.contains(searchQuery, ignoreCase = true) == true) ||
            (it.tags?.any { t -> t.contains(searchQuery, ignoreCase = true) } == true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Agent Studio",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real Letta stateful agents & memory blocks",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = { viewModel.loadAgents() },
                    modifier = Modifier.testTag("refresh_agents_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ElectricCyan)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("agent_search_input"),
                placeholder = { Text("Search agents by name or tag...", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ElectricCyan)
                }
            } else if (errorMessage != null) {
                ConsoleCard(borderColor = CrimsonLaser) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Failed to load agents from server", color = CrimsonLaser, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = errorMessage ?: "", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.loadAgents() },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonLaser)
                        ) {
                            Text("Retry Connection", color = VoidBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (filteredAgents.isEmpty()) {
                ConsoleCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.SmartToy, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No agents matching '$searchQuery'" else "No agents on this server yet.",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Create your first stateful agent with persistent core memory.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                            modifier = Modifier.testTag("create_agent_empty_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = VoidBlack)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Agent", color = VoidBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredAgents, key = { it.id }) { agent ->
                        ConsoleCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(ElectricCyan.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = agent.name.take(1).uppercase(),
                                                color = ElectricCyan,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = agent.name,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                            Text(
                                                text = "ID: ${agent.id}",
                                                color = TextMuted,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = AmberGlow.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = agent.llm ?: "default",
                                            color = AmberGlow,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (!agent.description.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = agent.description,
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                }

                                if (!agent.tags.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        agent.tags.take(4).forEach { tag ->
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = SurfaceDarkElevated
                                            ) {
                                                Text(
                                                    text = "#$tag",
                                                    color = TextSecondary,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { agentToDelete = agent },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CrimsonLaser, modifier = Modifier.size(18.dp))
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    OutlinedButton(
                                        onClick = {
                                            workspaceAgent = agent
                                            viewModel.loadAgentDetails(agent.id)
                                        },
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(Icons.Default.Memory, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Memory & Tools", fontSize = 11.sp, color = TextPrimary)
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = {
                                            viewModel.selectAgent(agent.id)
                                            onNavigateToChat(agent.id)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = null, tint = VoidBlack, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Chat", fontSize = 12.sp, color = VoidBlack, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button for Create Agent
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = ElectricCyan,
            contentColor = VoidBlack,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("fab_create_agent")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create Agent")
        }
    }

    // Create Agent Dialog
    if (showCreateDialog) {
        CreateAgentDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc, sys, llm, tags ->
                viewModel.createAgent(name, desc, sys, llm, tags)
                showCreateDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (agentToDelete != null) {
        AlertDialog(
            onDismissRequest = { agentToDelete = null },
            title = { Text("Delete Agent", color = TextPrimary) },
            text = { Text("Are you sure you want to permanently delete agent '${agentToDelete?.name}' from the Letta server? This cannot be undone.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        agentToDelete?.id?.let { viewModel.deleteAgent(it) }
                        agentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonLaser)
                ) {
                    Text("Delete", color = VoidBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { agentToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Agent Workspace Dialog (Memory, Tools, Archival, Sources)
    workspaceAgent?.let { agent ->
        AgentWorkspaceDialog(
            agent = agent,
            coreBlocks = coreBlocks,
            archivalItems = archivalItems,
            tools = tools,
            sources = sources,
            onDismiss = { workspaceAgent = null },
            onUpdateBlock = { label, value -> viewModel.updateCoreMemory(label, value) },
            onInsertArchival = { text -> viewModel.insertArchivalMemory(text) }
        )
    }
}

@Composable
fun CreateAgentDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, desc: String, system: String, llm: String, tags: List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var systemPrompt by remember { mutableStateOf("You are a helpful and stateful AI assistant powered by Letta.") }
    var llm by remember { mutableStateOf("letta-free") }
    var tagsInput by remember { mutableStateOf("research, assistant") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Stateful Letta Agent", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Agent Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricCyan, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("new_agent_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricCyan, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = llm,
                    onValueChange = { llm = it },
                    label = { Text("LLM Model (e.g. gpt-4o, letta-free)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricCyan, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text("System Instructions") },
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricCyan, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text("Tags (comma separated)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricCyan, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        onCreate(name, description, systemPrompt, llm, tags)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                modifier = Modifier.testTag("confirm_create_agent_button")
            ) {
                Text("Create on Server", color = VoidBlack, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceDark
    )
}

@Composable
fun AgentWorkspaceDialog(
    agent: LettaAgentDto,
    coreBlocks: List<com.example.data.remote.BlockDto>,
    archivalItems: List<com.example.data.remote.ArchivalMemoryItem>,
    tools: List<com.example.data.remote.ToolDto>,
    sources: List<com.example.data.remote.SourceDto>,
    onDismiss: () -> Unit,
    onUpdateBlock: (label: String, value: String) -> Unit,
    onInsertArchival: (text: String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Core Memory", "Archival", "Tools", "Sources")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark,
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = agent.name,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Memory & Architecture Workspace",
                            color = ElectricCyan,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                // Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SurfaceDarkElevated,
                    contentColor = ElectricCyan,
                    edgePadding = 8.dp
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                // Tab Content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> CoreMemoryTab(coreBlocks, onUpdateBlock)
                        1 -> ArchivalMemoryTab(archivalItems, onInsertArchival)
                        2 -> ToolsTab(tools)
                        3 -> SourcesTab(sources)
                    }
                }
            }
        }
    }
}

@Composable
fun CoreMemoryTab(
    blocks: List<com.example.data.remote.BlockDto>,
    onUpdateBlock: (label: String, value: String) -> Unit
) {
    if (blocks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No core memory blocks attached to this agent.", color = TextMuted)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(blocks) { block ->
                var isEditing by remember { mutableStateOf(false) }
                var editValue by remember { mutableStateOf(block.value) }

                ConsoleCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = block.blockName.uppercase(),
                                color = ElectricCyan,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${block.value.length} chars",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (isEditing) {
                                            onUpdateBlock(block.blockName, editValue)
                                            isEditing = false
                                        } else {
                                            editValue = block.value
                                            isEditing = true
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isEditing) Icons.Default.Add else Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = if (isEditing) EmeraldNeon else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (isEditing) {
                            OutlinedTextField(
                                value = editValue,
                                onValueChange = { editValue = it },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 4,
                                maxLines = 8,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricCyan,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { isEditing = false }) {
                                    Text("Cancel", color = TextSecondary, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = {
                                        onUpdateBlock(block.blockName, editValue)
                                        isEditing = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                                ) {
                                    Text("Save to Letta Server", color = VoidBlack, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        } else {
                            CodeBlockText(text = block.value.ifBlank { "(empty block)" })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArchivalMemoryTab(
    items: List<com.example.data.remote.ArchivalMemoryItem>,
    onInsertArchival: (text: String) -> Unit
) {
    var newPassage by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newPassage,
                onValueChange = { newPassage = it },
                placeholder = { Text("Insert new text passage to vector archival memory...", color = TextMuted, fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricCyan, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newPassage.isNotBlank()) {
                        onInsertArchival(newPassage)
                        newPassage = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberGlow)
            ) {
                Text("Insert", color = VoidBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No archival passages stored yet.", color = TextMuted)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items) { item ->
                    ConsoleCard {
                        Column {
                            Text(
                                text = item.text,
                                color = TextPrimary,
                                fontSize = 12.sp
                            )
                            if (item.timestamp != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.timestamp,
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolsTab(tools: List<com.example.data.remote.ToolDto>) {
    if (tools.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No tools installed on server.", color = TextMuted)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tools) { tool ->
                ConsoleCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = tool.name,
                                color = EmeraldNeon,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                            tool.sourceType?.let {
                                Text(
                                    text = it,
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        tool.description?.let {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = it, color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SourcesTab(sources: List<com.example.data.remote.SourceDto>) {
    if (sources.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No document/RAG sources attached.", color = TextMuted)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sources) { src ->
                ConsoleCard {
                    Column {
                        Text(
                            text = src.name,
                            color = ElectricCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        src.description?.let {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = it, color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
