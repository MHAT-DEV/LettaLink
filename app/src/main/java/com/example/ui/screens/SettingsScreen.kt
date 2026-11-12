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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.data.local.ServerEntity
import com.example.data.local.TtsProviderEntity
import com.example.ui.MainViewModel
import com.example.ui.components.ConsoleCard
import com.example.ui.components.LatencyBadge
import com.example.ui.components.StatusPill
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

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val servers by viewModel.servers.collectAsState()
    val discoveredServers by viewModel.discoveredServers.collectAsState()
    val isScanningLan by viewModel.isScanningLan.collectAsState()
    val ttsSettings by viewModel.ttsSettings.collectAsState()
    val ttsProviders by viewModel.ttsProviders.collectAsState()
    val talkPlayback by viewModel.talkPlaybackState.collectAsState()

    var showAddServerDialog by remember { mutableStateOf(false) }
    var showAddTtsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "System Settings",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Server profiles, LettaTalk TTS & security controls",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Section: Configured Letta Servers
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONFIGURED SERVERS (${servers.size})",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Button(
                    onClick = { showAddServerDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    modifier = Modifier.testTag("add_server_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = VoidBlack, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Server", color = VoidBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        items(servers, key = { it.id }) { server ->
            ServerItemCard(
                server = server,
                onSelectActive = { viewModel.selectServer(server.id) },
                onDelete = { viewModel.deleteServer(server.id) },
                onTest = {
                    viewModel.testServer(server) { res ->
                        viewModel.showNotice(res.message)
                    }
                }
            )
        }

        // Section: Local Network Discovery
        item {
            Text(
                text = "LOCAL NETWORK DISCOVERY (mDNS / NSD)",
                color = TextSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        item {
            ConsoleCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Scan Wi-Fi / Local Subnet", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "Finds self-hosted Letta instances on port 8283", color = TextMuted, fontSize = 11.sp)
                        }

                        Row {
                            Button(
                                onClick = { viewModel.quickSubnetScan() },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDarkElevated)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Probe", fontSize = 11.sp, color = TextPrimary)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Button(
                                onClick = {
                                    if (isScanningLan) viewModel.stopLanScan() else viewModel.startLanScan()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isScanningLan) CrimsonLaser else ElectricCyan)
                            ) {
                                if (isScanningLan) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = VoidBlack)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Stop", color = VoidBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.Lan, contentDescription = null, tint = VoidBlack, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Scan", color = VoidBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (discoveredServers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Discovered Services:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        discoveredServers.forEach { d ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceDarkElevated,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = d.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(text = d.fullUrl, color = ElectricCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.addServer(
                                                name = d.name,
                                                providerType = "SELF_HOSTED",
                                                baseUrl = d.fullUrl,
                                                apiKey = "",
                                                isPrimary = true
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon)
                                    ) {
                                        Text("Add & Connect", color = VoidBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: LettaTalk Universal TTS & Read Aloud
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LETTATALK — UNIVERSAL TTS & READ ALOUD",
                    color = EmeraldNeon,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Button(
                    onClick = { showAddTtsDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDarkElevated),
                    modifier = Modifier.testTag("add_custom_tts_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldNeon, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add TTS", color = TextPrimary, fontSize = 11.sp)
                }
            }
        }

        item {
            ConsoleCard(borderColor = EmeraldNeon.copy(alpha = 0.4f)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Active Provider Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldNeon.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = EmeraldNeon, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = talkPlayback.activeProviderTitle,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (ttsProviders.none { it.isDefault }) "Device Local Engine • 100% Offline • Free" else "Custom Speech API Endpoint",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.testVoice("LettaTalk is ready. ระบบอ่านออกเสียงพร้อมใช้งาน")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = VoidBlack, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Voice", color = VoidBlack, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Automatic Read Aloud Selector
                    Text(
                        text = "Automatic Read Aloud Mode",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val modes = listOf(
                        "OFF" to "Off",
                        "EVERY_RESPONSE" to "Every Response",
                        "SELECTED_AGENT_ONLY" to "Selected Agent",
                        "MANUAL_ONLY" to "Manual Only"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        modes.forEach { (key, label) ->
                            val isSelected = ttsSettings.autoReadAloudMode == key
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) EmeraldNeon.copy(alpha = 0.2f) else SurfaceDarkElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) EmeraldNeon else BorderSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.updateTtsSettings(ttsSettings.copy(autoReadAloudMode = key))
                                    }
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) EmeraldNeon else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Speech Rate Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Speech Rate (Speed)", color = TextSecondary, fontSize = 12.sp)
                        Text(
                            text = "%.1fx".format(ttsSettings.speechRate),
                            color = ElectricCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = ttsSettings.speechRate,
                        onValueChange = { newRate ->
                            viewModel.updateTtsSettings(ttsSettings.copy(speechRate = newRate))
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 5,
                        colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Speech Pitch Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Speech Pitch", color = TextSecondary, fontSize = 12.sp)
                        Text(
                            text = "%.1fx".format(ttsSettings.pitch),
                            color = AmberGlow,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = ttsSettings.pitch,
                        onValueChange = { newPitch ->
                            viewModel.updateTtsSettings(ttsSettings.copy(pitch = newPitch))
                        },
                        valueRange = 0.5f..1.5f,
                        steps = 4,
                        colors = SliderDefaults.colors(thumbColor = AmberGlow, activeTrackColor = AmberGlow)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Content Normalization Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Skip Markdown Code Blocks", color = TextPrimary, fontSize = 13.sp)
                            Text(text = "Filters out code syntax so it isn't read aloud", color = TextMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = ttsSettings.skipCodeBlocks,
                            onCheckedChange = { viewModel.updateTtsSettings(ttsSettings.copy(skipCodeBlocks = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldNeon, checkedTrackColor = EmeraldNeon.copy(alpha = 0.3f))
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Skip Web URLs", color = TextPrimary, fontSize = 13.sp)
                            Text(text = "Prevents reading long http links aloud", color = TextMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = ttsSettings.skipUrls,
                            onCheckedChange = { viewModel.updateTtsSettings(ttsSettings.copy(skipUrls = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldNeon, checkedTrackColor = EmeraldNeon.copy(alpha = 0.3f))
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Auto Language Detection", color = TextPrimary, fontSize = 13.sp)
                            Text(text = "Adapts locale dynamically (Thai, English, CJK, Japanese)", color = TextMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = ttsSettings.autoLanguageDetection,
                            onCheckedChange = { viewModel.updateTtsSettings(ttsSettings.copy(autoLanguageDetection = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldNeon, checkedTrackColor = EmeraldNeon.copy(alpha = 0.3f))
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Audio Cache Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Audio Cache Management", color = TextSecondary, fontSize = 12.sp)
                        Button(
                            onClick = { viewModel.clearAudioCache() },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDarkElevated)
                        ) {
                            Text("Clear Audio Cache", color = TextPrimary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Section: Configured Custom TTS Providers List
        if (ttsProviders.isNotEmpty()) {
            item {
                Text(
                    text = "CUSTOM TTS PROVIDERS (${ttsProviders.size})",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            items(ttsProviders) { provider ->
                ConsoleCard(
                    borderColor = if (provider.isDefault) EmeraldNeon else BorderSubtle
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = provider.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(shape = RoundedCornerShape(4.dp), color = SurfaceDarkElevated) {
                                    Text(text = provider.providerType, color = AmberGlow, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                            Text(text = "${provider.baseUrl}/${provider.endpoint}", color = ElectricCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text(text = "Model: ${provider.model} • Voice: ${provider.defaultVoice}", color = TextMuted, fontSize = 11.sp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.deleteTtsProvider(provider.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CrimsonLaser, modifier = Modifier.size(16.dp))
                            }
                            if (!provider.isDefault) {
                                Button(
                                    onClick = { viewModel.setDefaultTtsProvider(provider.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon)
                                ) {
                                    Text("Set Active", color = VoidBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.setDefaultTtsProvider(-1L) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDarkElevated)
                                ) {
                                    Text("Use Device TTS", color = TextPrimary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Security & Privacy
        item {
            Text(
                text = "SECURITY & STORAGE",
                color = TextSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        item {
            ConsoleCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldNeon)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "Credential Storage", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "Tokens stored in private app storage. Redacted from logs.", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = ElectricCyan)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "Production-First Architecture", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "No fake mocks in runtime path. Real HTTP & SSE to Letta.", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Section: About & Specifications
        item {
            ConsoleCard {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = AmberGlow)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "LettaLink & LettaTalk v1.1.0", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Built for Letta (formerly MemGPT) REST API v1. Supports stateful agents, dual-tier memory blocks (core & archival), dynamic tool execution, direct API telemetry, and LettaTalk Universal Multilingual TTS.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    if (showAddServerDialog) {
        AddServerDialog(
            onDismiss = { showAddServerDialog = false },
            onAdd = { name, type, url, key, isPrimary ->
                viewModel.addServer(name, type, url, key, isPrimary)
                showAddServerDialog = false
            }
        )
    }

    if (showAddTtsDialog) {
        AddCustomTtsDialog(
            onDismiss = { showAddTtsDialog = false },
            onAdd = { name, baseUrl, endpoint, apiKey, model, voice ->
                viewModel.addCustomTtsProvider(name, baseUrl, endpoint, apiKey, model, voice)
                showAddTtsDialog = false
            }
        )
    }
}

@Composable
fun AddCustomTtsDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, baseUrl: String, endpoint: String, apiKey: String, model: String, voice: String) -> Unit
) {
    var name by remember { mutableStateOf("OpenAI / Self-Hosted TTS") }
    var baseUrl by remember { mutableStateOf("https://api.openai.com") }
    var endpoint by remember { mutableStateOf("v1/audio/speech") }
    var apiKey by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("tts-1") }
    var voice by remember { mutableStateOf("alloy") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom TTS Service", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Provider Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldNeon, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("custom_tts_name_input")
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL (e.g. http://10.0.2.2:8000)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldNeon, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("custom_tts_url_input")
                )
                OutlinedTextField(
                    value = endpoint,
                    onValueChange = { endpoint = it },
                    label = { Text("Endpoint") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldNeon, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key (optional for self-hosted)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldNeon, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("custom_tts_key_input")
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Model") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldNeon, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = voice,
                        onValueChange = { voice = it },
                        label = { Text("Voice") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldNeon, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && baseUrl.isNotBlank()) {
                        onAdd(name, baseUrl, endpoint, apiKey, model, voice)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon),
                modifier = Modifier.testTag("confirm_add_tts_btn")
            ) {
                Text("Save Provider", color = VoidBlack, fontWeight = FontWeight.Bold)
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
fun ServerItemCard(
    server: ServerEntity,
    onSelectActive: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    ConsoleCard(
        borderColor = if (server.isPrimary) ElectricCyan else BorderSubtle
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (server.isPrimary) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ElectricCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = server.name,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SurfaceDarkElevated
                    ) {
                        Text(
                            text = server.providerType,
                            color = AmberGlow,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                StatusPill(status = server.lastHealthStatus)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = server.baseUrl,
                color = ElectricCyan,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LatencyBadge(latencyMs = server.lastLatencyMs)
                    if (server.lastVersion.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "v${server.lastVersion}",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CrimsonLaser, modifier = Modifier.size(16.dp))
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Button(
                        onClick = onTest,
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDarkElevated),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Test Ping", fontSize = 11.sp, color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    if (!server.isPrimary) {
                        Button(
                            onClick = onSelectActive,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Set Active", fontSize = 11.sp, color = VoidBlack, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ElectricCyan.copy(alpha = 0.2f),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                color = ElectricCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddServerDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, type: String, url: String, key: String, isPrimary: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("My Letta Server") }
    var providerType by remember { mutableStateOf("SELF_HOSTED") }
    var baseUrl by remember { mutableStateOf("http://10.0.2.2:8283") }
    var apiKey by remember { mutableStateOf("") }
    var setPrimary by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register Letta Server", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Server Label") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricCyan, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("server_name_input")
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL (e.g. http://192.168.1.100:8283)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricCyan, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("server_url_input")
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key / Bearer Token (optional for self-hosted)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricCyan, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("server_token_input")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Set as Primary Active Server", color = TextSecondary, fontSize = 13.sp)
                    Switch(
                        checked = setPrimary,
                        onCheckedChange = { setPrimary = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = ElectricCyan, checkedTrackColor = ElectricCyan.copy(alpha = 0.3f))
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && baseUrl.isNotBlank()) {
                        onAdd(name, providerType, baseUrl, apiKey, setPrimary)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                modifier = Modifier.testTag("confirm_add_server_btn")
            ) {
                Text("Save Server", color = VoidBlack, fontWeight = FontWeight.Bold)
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
