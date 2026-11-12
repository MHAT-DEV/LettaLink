package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.tts.PlaybackStatus
import com.example.domain.tts.TalkPlaybackState
import com.example.ui.MainViewModel
import com.example.ui.UiChatMessage
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CrimsonLaser
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceDarkElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack

@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    targetAgentId: String? = null
) {
    val agents by viewModel.agents.collectAsState()
    val selectedAgent by viewModel.selectedAgent.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isSending by viewModel.isSendingMessage.collectAsState()
    val talkPlayback by viewModel.talkPlaybackState.collectAsState()
    val ttsSettings by viewModel.ttsSettings.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var useStreaming by remember { mutableStateOf(true) }
    var showAgentMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(targetAgentId) {
        if (!targetAgentId.isNullOrBlank() && targetAgentId != selectedAgent?.id) {
            viewModel.selectAgent(targetAgentId)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        // Chat Top Bar
        Surface(
            color = SurfaceDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Agent Selector Dropdown
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceDarkElevated)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                            .clickable { showAgentMenu = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("agent_selector_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (selectedAgent != null) EmeraldNeon else CrimsonLaser)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedAgent?.name ?: "Select Agent",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showAgentMenu,
                        onDismissRequest = { showAgentMenu = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        agents.forEach { agent ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(text = agent.name, color = TextPrimary, fontWeight = FontWeight.Bold)
                                        Text(text = agent.llm ?: "default model", color = AmberGlow, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                },
                                onClick = {
                                    viewModel.selectAgent(agent.id)
                                    showAgentMenu = false
                                }
                            )
                        }
                    }
                }

                // Streaming Toggle, TTS Auto Read Status & Clear History
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // LettaTalk Auto Status Pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (ttsSettings.autoReadAloudMode != "OFF") EmeraldNeon.copy(alpha = 0.15f) else SurfaceDarkElevated,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = if (ttsSettings.autoReadAloudMode != "OFF") Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = "TTS Status",
                                tint = if (ttsSettings.autoReadAloudMode != "OFF") EmeraldNeon else TextMuted,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "TALK",
                                color = if (ttsSettings.autoReadAloudMode != "OFF") EmeraldNeon else TextMuted,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "SSE",
                        color = if (useStreaming) ElectricCyan else TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = useStreaming,
                        onCheckedChange = { useStreaming = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ElectricCyan,
                            checkedTrackColor = ElectricCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceDarkElevated
                        ),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { viewModel.clearChat() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Chat", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Messages Timeline
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
        ) {
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (selectedAgent != null) "Ready to chat with ${selectedAgent?.name}" else "No agent selected",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Real Letta memory blocks and LettaTalk speech synthesis enabled.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 12.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        ChatMessageBubble(
                            message = message,
                            isSpeaking = talkPlayback.status == PlaybackStatus.PLAYING && talkPlayback.currentMessageId == message.id,
                            onPlayTts = {
                                viewModel.speakMessage(
                                    messageId = message.id,
                                    text = message.content,
                                    agentName = selectedAgent?.name ?: "Agent"
                                )
                            },
                            onStopTts = {
                                viewModel.stopSpeaking()
                            }
                        )
                    }
                }
            }
        }

        // LettaTalk Floating Mini Player (Docked above input bar)
        if (talkPlayback.status == PlaybackStatus.PLAYING || talkPlayback.status == PlaybackStatus.BUFFERING) {
            Surface(
                color = SurfaceDarkElevated,
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                modifier = Modifier.fillMaxWidth().testTag("lettatalk_mini_player")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(EmeraldNeon.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = EmeraldNeon,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = talkPlayback.currentAgentName ?: "Agent",
                                    color = ElectricCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• ${talkPlayback.activeProviderTitle}",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = talkPlayback.currentSentence ?: "Reading message aloud...",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.stopSpeaking() },
                        modifier = Modifier.size(32.dp).testTag("stop_mini_player_btn")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop Speech", tint = CrimsonLaser)
                    }
                }
            }
        }

        // Input Bar
        Surface(
            color = SurfaceDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = if (selectedAgent != null) "Message ${selectedAgent?.name}..." else "Select an agent first...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                if (isSending) {
                    IconButton(
                        onClick = { viewModel.stopStreaming() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CrimsonLaser)
                            .testTag("stop_streaming_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = VoidBlack)
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val toSend = inputText
                                inputText = ""
                                viewModel.sendMessage(toSend, useStreaming)
                            }
                        },
                        enabled = inputText.isNotBlank() && selectedAgent != null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank() && selectedAgent != null) ElectricCyan else SurfaceDarkElevated)
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank() && selectedAgent != null) VoidBlack else TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: UiChatMessage,
    isSpeaking: Boolean = false,
    onPlayTts: () -> Unit = {},
    onStopTts: () -> Unit = {}
) {
    val isUser = message.role == "user"
    val isReasoning = message.role == "reasoning"
    val isTool = message.role == "tool"
    val isSystem = message.role == "system"

    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        when {
            isUser -> {
                Surface(
                    shape = RoundedCornerShape(topStart = 14.dp, topEnd = 4.dp, bottomStart = 14.dp, bottomEnd = 14.dp),
                    color = ElectricCyan.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.padding(start = 48.dp)
                ) {
                    Text(
                        text = message.content,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            isReasoning -> {
                var expanded by remember { mutableStateOf(false) }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AmberGlow.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, AmberGlow.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clickable { expanded = !expanded }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = AmberGlow, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AGENT REASONING / THOUGHT",
                                    color = AmberGlow,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = if (expanded) "Hide" else "Show",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        if (expanded) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = message.content,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            isTool -> {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldNeon.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, EmeraldNeon.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth(0.92f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Build, contentDescription = null, tint = EmeraldNeon, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TOOL EXECUTION: ${message.toolCallName ?: "tool"}",
                                color = EmeraldNeon,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (!message.toolCallArgs.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Args: ${message.toolCallArgs}",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            isSystem -> {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CrimsonLaser.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, CrimsonLaser.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message.content,
                        color = CrimsonLaser,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            else -> {
                // Assistant message
                Surface(
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp),
                    color = SurfaceDarkElevated,
                    border = BorderStroke(1.dp, if (isSpeaking) EmeraldNeon.copy(alpha = 0.6f) else BorderSubtle),
                    modifier = Modifier.padding(end = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(
                            text = message.content,
                            color = TextPrimary,
                            fontSize = 14.sp
                        )

                        if (message.isStreamingChunk) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(10.dp),
                                    color = ElectricCyan,
                                    strokeWidth = 1.5.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Streaming...",
                                    color = ElectricCyan,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // LettaTalk Read Aloud Action Row
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSpeaking) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(EmeraldNeon.copy(alpha = 0.2f))
                                        .clickable { onStopTts() }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                        .testTag("tts_speaking_indicator")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Speaking",
                                        tint = EmeraldNeon,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Reading Aloud",
                                        color = EmeraldNeon,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop",
                                        tint = CrimsonLaser,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = onPlayTts,
                                    modifier = Modifier.size(28.dp).testTag("tts_play_btn_${message.id.take(6)}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Read Aloud",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
