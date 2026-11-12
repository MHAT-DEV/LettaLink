package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CrimsonLaser
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.SurfaceDarkElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StatusPill(
    status: String,
    modifier: Modifier = Modifier
) {
    val (dotColor, labelColor, bg) = when (status.uppercase()) {
        "HEALTHY", "CONNECTED", "OK" -> Triple(EmeraldNeon, EmeraldNeon, Color(0x2200E676))
        "DEGRADED", "WARN" -> Triple(AmberGlow, AmberGlow, Color(0x22FFB300))
        "OFFLINE", "ERROR", "UNREACHABLE" -> Triple(CrimsonLaser, CrimsonLaser, Color(0x22FF3366))
        else -> Triple(TextMuted, TextSecondary, Color(0x2264748B))
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = status.uppercase(),
                color = labelColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun LatencyBadge(
    latencyMs: Long,
    modifier: Modifier = Modifier
) {
    val color = when {
        latencyMs == 0L -> TextMuted
        latencyMs < 100 -> EmeraldNeon
        latencyMs < 400 -> AmberGlow
        else -> CrimsonLaser
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Text(
            text = if (latencyMs > 0) "${latencyMs}ms" else "---",
            color = color,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun ConsoleCard(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val baseModifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, borderColor, RoundedCornerShape(14.dp))

    val finalModifier = if (onClick != null) {
        baseModifier.clickable { onClick() }
    } else {
        baseModifier
    }

    Box(modifier = finalModifier.padding(16.dp)) {
        content()
    }
}

@Composable
fun CodeBlockText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = ElectricCyan
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SurfaceDarkElevated,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.padding(10.dp)
        )
    }
}
