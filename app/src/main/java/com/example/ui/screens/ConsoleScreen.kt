package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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
fun ConsoleScreen(
    viewModel: MainViewModel,
    initialTab: Int = 0
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val tabTitles = listOf("CLI Terminal", "Raw API Explorer")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SurfaceDark,
            contentColor = ElectricCyan
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    icon = {
                        Icon(
                            imageVector = if (index == 0) Icons.Default.Terminal else Icons.Default.Code,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    text = {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> CliTerminalTab(viewModel)
            1 -> RawApiExplorerTab(viewModel)
        }
    }
}

@Composable
fun CliTerminalTab(viewModel: MainViewModel) {
    val cliLogs by viewModel.cliLog.collectAsState()
    var commandInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val suggestionChips = listOf(
        "help",
        "provider list",
        "provider health",
        "agent list",
        "agent tools list",
        "agent sources list"
    )

    LaunchedEffect(cliLogs.size) {
        if (cliLogs.isNotEmpty()) {
            listState.animateScrollToItem(cliLogs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LETTA TERMINAL INTERFACE",
                color = ElectricCyan,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Row {
                IconButton(
                    onClick = {
                        val text = cliLogs.joinToString("\n")
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Terminal Logs", text))
                        viewModel.showNotice("Logs copied to clipboard!")
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = { viewModel.clearCli() },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Suggestion Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestionChips.forEach { chip ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceDarkElevated,
                    modifier = Modifier.clickable {
                        viewModel.runCliCommand(chip)
                    }
                ) {
                    Text(
                        text = chip,
                        color = ElectricCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Terminal Log Viewport
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(cliLogs) { line ->
                    val color = when {
                        line.startsWith(">") -> ElectricCyan
                        line.contains("Error", ignoreCase = true) || line.contains("Failed", ignoreCase = true) -> CrimsonLaser
                        line.contains("OK", ignoreCase = true) || line.contains("Found", ignoreCase = true) -> EmeraldNeon
                        else -> TextPrimary
                    }
                    Text(
                        text = line,
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Prompt Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                placeholder = { Text("Enter command (e.g. agent list, help)...", color = TextMuted, fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("cli_command_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (commandInput.isNotBlank()) {
                        val cmd = commandInput
                        commandInput = ""
                        viewModel.runCliCommand(cmd)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(52.dp)
                    .testTag("cli_run_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = VoidBlack)
            }
        }
    }
}

@Composable
fun RawApiExplorerTab(viewModel: MainViewModel) {
    val rawResult by viewModel.rawResult.collectAsState()
    val isLoading by viewModel.isRawLoading.collectAsState()
    val context = LocalContext.current

    var method by remember { mutableStateOf("GET") }
    var endpoint by remember { mutableStateOf("v1/health") }
    var requestBody by remember { mutableStateOf("") }
    var showMethodDropdown by remember { mutableStateOf(false) }

    val methods = listOf("GET", "POST", "PATCH", "PUT", "DELETE")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "DIRECT HTTP API CLIENT",
                color = ElectricCyan,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // Method & Endpoint Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Button(
                        onClick = { showMethodDropdown = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDarkElevated),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text(
                            text = method,
                            color = when (method) {
                                "GET" -> EmeraldNeon
                                "POST" -> ElectricCyan
                                "PATCH", "PUT" -> AmberGlow
                                else -> CrimsonLaser
                            },
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Icon(Icons.Default.ExpandMore, contentDescription = null, tint = TextSecondary)
                    }

                    DropdownMenu(
                        expanded = showMethodDropdown,
                        onDismissRequest = { showMethodDropdown = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        methods.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m, color = TextPrimary, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    method = m
                                    showMethodDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = endpoint,
                    onValueChange = { endpoint = it },
                    label = { Text("Endpoint") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("raw_endpoint_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Common Endpoint Shortcuts
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("v1/health", "v1/agents", "v1/models", "v1/tools", "v1/sources").forEach { ep ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SurfaceDarkElevated,
                        modifier = Modifier.clickable {
                            endpoint = ep
                            method = "GET"
                        }
                    ) {
                        Text(
                            text = ep,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Request Body Editor (shown for POST/PATCH/PUT)
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "JSON Request Body",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        Text(
                            text = "Sample Agent",
                            color = ElectricCyan,
                            fontSize = 11.sp,
                            modifier = Modifier.clickable {
                                requestBody = """{"name":"Console-Agent","system":"Helpful assistant","llm":"letta-free"}"""
                                method = "POST"
                                endpoint = "v1/agents"
                            }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Clear",
                            color = TextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.clickable { requestBody = "" }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = requestBody,
                    onValueChange = { requestBody = it },
                    placeholder = { Text("{\n  \"key\": \"value\"\n}", color = TextMuted, fontFamily = FontFamily.Monospace) },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("raw_body_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Send Button
        item {
            Button(
                onClick = {
                    viewModel.executeRawApi(method, endpoint, emptyMap(), requestBody.ifBlank { null })
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("send_raw_api_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = VoidBlack)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Executing HTTP Request...", color = VoidBlack, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = VoidBlack)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Execute Request", color = VoidBlack, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Response Section
        rawResult?.let { res ->
            item {
                ConsoleCard(
                    borderColor = if (res.isSuccessful) EmeraldNeon else CrimsonLaser
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (res.isSuccessful) EmeraldNeon.copy(alpha = 0.2f) else CrimsonLaser.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${res.statusCode} ${res.statusMessage}",
                                        color = if (res.isSuccessful) EmeraldNeon else CrimsonLaser,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                LatencyBadge(latencyMs = res.durationMs)
                            }

                            Row {
                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("cURL Command", res.curlCommand))
                                        viewModel.showNotice("cURL command copied!")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDarkElevated),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy cURL", fontSize = 11.sp, color = TextPrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Response Body:",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceDarkElevated)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = res.body.ifBlank { "(empty body)" },
                                color = if (res.isSuccessful) ElectricCyan else CrimsonLaser,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
