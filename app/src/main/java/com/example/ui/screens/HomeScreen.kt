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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.ConsoleCard
import com.example.ui.components.LatencyBadge
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.SurfaceDarkElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToAgents: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToChronicle: () -> Unit = {},
    onNavigateToConsole: (initialTab: Int) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val primaryServer by viewModel.primaryServer.collectAsState()
    val healthReport by viewModel.healthReport.collectAsState()
    val isHealthChecking by viewModel.isHealthChecking.collectAsState()
    val agents by viewModel.agents.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElectricCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = "Console",
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "LettaLink",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Self-hosted First. Cloud Connected. Agent Agnostic.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Active Server Hero Card
        item {
            ConsoleCard(
                borderColor = ElectricCyan.copy(alpha = 0.4f),
                modifier = Modifier.testTag("active_server_card")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = primaryServer?.name ?: "No Server Selected",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SurfaceDarkElevated
                            ) {
                                Text(
                                    text = primaryServer?.providerType ?: "NONE",
                                    color = AmberGlow,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        StatusPill(
                            status = healthReport?.status ?: primaryServer?.lastHealthStatus ?: "UNCHECKED"
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = primaryServer?.baseUrl ?: "Configure in Settings",
                        color = ElectricCyan,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Latency: ", color = TextSecondary, fontSize = 12.sp)
                            LatencyBadge(latencyMs = healthReport?.latencyMs ?: primaryServer?.lastLatencyMs ?: 0L)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Ver: ${healthReport?.serverVersion ?: primaryServer?.lastVersion?.ifEmpty { "v1.0" } ?: "-"}",
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row {
                            Button(
                                onClick = { viewModel.refreshHealth() },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDarkElevated),
                                modifier = Modifier.testTag("test_health_button")
                            ) {
                                if (isHealthChecking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = ElectricCyan,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Test Health",
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text("Ping", color = TextPrimary, fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            OutlinedButton(
                                onClick = onNavigateToSettings,
                                modifier = Modifier.testTag("switch_server_button")
                            ) {
                                Text("Servers", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Metrics Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    title = "AGENTS",
                    count = agents.size.toString(),
                    icon = Icons.Default.SmartToy,
                    accentColor = ElectricCyan,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAgents
                )
                MetricTile(
                    title = "SERVER LOGS",
                    count = auditLogs.size.toString(),
                    icon = Icons.Default.Terminal,
                    accentColor = AmberGlow,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToConsole(0) }
                )
            }
        }

        // Console Shortcuts Grid
        item {
            Text(
                text = "SYSTEM CONTROLS",
                color = TextSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ShortcutTile(
                    title = "Agent Studio",
                    subtitle = "Explore & manage agents",
                    icon = Icons.Default.SmartToy,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAgents
                )
                ShortcutTile(
                    title = "Multimodal Chat",
                    subtitle = "Stream reasoning & tools",
                    icon = Icons.Default.Chat,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val first = agents.firstOrNull()?.id ?: ""
                        onNavigateToChat(first)
                    }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ShortcutTile(
                    title = "CLI Command",
                    subtitle = "Universal agent shell",
                    icon = Icons.Default.Terminal,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToConsole(0) }
                )
                ShortcutTile(
                    title = "Raw API Client",
                    subtitle = "Direct HTTP & cURL export",
                    icon = Icons.Default.Code,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToConsole(1) }
                )
            }
        }

        item {
            ShortcutTile(
                title = "LettaChronicle Memory",
                subtitle = "Temporal Knowledge Graph & Markdown Docs",
                icon = Icons.Default.AccountTree,
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateToChronicle
            )
        }

        item {
            ShortcutTile(
                title = "LettaTalk Voice Engine",
                subtitle = "Multilingual TTS & Read Aloud Controls",
                icon = Icons.Default.RecordVoiceOver,
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateToSettings
            )
        }

        // Recent Agents Carousel
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE AGENTS (${agents.size})",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "View All",
                    color = ElectricCyan,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onNavigateToAgents() }
                )
            }
        }

        if (agents.isEmpty()) {
            item {
                ConsoleCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No agents found on active Letta server.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onNavigateToAgents,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                        ) {
                            Text("Create or Load Agents", color = VoidBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(agents) { agent ->
                        ConsoleCard(
                            modifier = Modifier
                                .width(220.dp)
                                .clickable { onNavigateToChat(agent.id) }
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(ElectricCyan.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = agent.name.take(1).uppercase(),
                                            color = ElectricCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = agent.name,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = agent.llm ?: "default model",
                                    color = AmberGlow,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = agent.description ?: "No description provided",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { onNavigateToChat(agent.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDarkElevated),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = "Chat", modifier = Modifier.size(14.dp), tint = ElectricCyan)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Chat", fontSize = 12.sp, color = TextPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live API Telemetry Preview
        item {
            Text(
                text = "RECENT API TELEMETRY",
                color = TextSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        if (auditLogs.isEmpty()) {
            item {
                ConsoleCard {
                    Text(
                        text = "No outgoing requests logged yet. Actions against Letta server will appear here.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            items(auditLogs.take(3)) { log ->
                ConsoleCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (log.isSuccess) EmeraldNeon.copy(alpha = 0.2f) else MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = log.method,
                                        color = if (log.isSuccess) EmeraldNeon else MaterialTheme.colorScheme.error,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = log.endpoint,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = log.responseSnippet.take(60),
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (log.statusCode > 0) "${log.statusCode}" else "ERR",
                                color = if (log.isSuccess) EmeraldNeon else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${log.durationMs}ms",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricTile(
    title: String,
    count: String,
    icon: ImageVector,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ConsoleCard(
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = title, color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = count, color = accentColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun ShortcutTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ConsoleCard(
        modifier = modifier.clickable { onClick() }
    ) {
        Column {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, color = TextMuted, fontSize = 11.sp, maxLines = 1)
        }
    }
}
