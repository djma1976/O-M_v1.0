package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.TechStatus
import com.example.ui.theme.HazardRed
import com.example.ui.theme.OperationalEmerald
import com.example.ui.theme.SafetyAmber
import com.example.ui.theme.TechBluePrimary
import com.example.ui.theme.TechBluePrimaryLight

@Composable
fun TaskPriorityBadge(priority: TaskPriority, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (priority) {
        TaskPriority.P1_CRITICAL -> HazardRed.copy(alpha = 0.2f) to HazardRed
        TaskPriority.P2_HIGH -> SafetyAmber.copy(alpha = 0.2f) to SafetyAmber
        TaskPriority.P3_MEDIUM -> TechBluePrimary.copy(alpha = 0.2f) to TechBluePrimaryLight
        TaskPriority.P4_ROUTINE -> OperationalEmerald.copy(alpha = 0.2f) to OperationalEmerald
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = priority.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun TaskStatusBadge(status: TaskStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (status) {
        TaskStatus.PENDING_DISPATCH -> Color(0xFF334155) to Color(0xFFCBD5E1)
        TaskStatus.DISPATCHED -> SafetyAmber.copy(alpha = 0.2f) to SafetyAmber
        TaskStatus.ACCEPTED -> TechBluePrimary.copy(alpha = 0.2f) to TechBluePrimaryLight
        TaskStatus.EN_ROUTE -> TechBluePrimary.copy(alpha = 0.25f) to TechBluePrimaryLight
        TaskStatus.ON_SITE_WORKING -> SafetyAmber.copy(alpha = 0.25f) to SafetyAmber
        TaskStatus.AWAITING_PARTS -> Color(0xFF6B21A8).copy(alpha = 0.25f) to Color(0xFFD8B4FE)
        TaskStatus.COMPLETED -> OperationalEmerald.copy(alpha = 0.2f) to OperationalEmerald
        TaskStatus.REJECTED -> HazardRed.copy(alpha = 0.2f) to HazardRed
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun TechStatusBadge(status: TechStatus, modifier: Modifier = Modifier) {
    val (dotColor, textColor, bgColor) = when (status) {
        TechStatus.AVAILABLE -> Triple(OperationalEmerald, OperationalEmerald, OperationalEmerald.copy(alpha = 0.15f))
        TechStatus.EN_ROUTE -> Triple(TechBluePrimaryLight, TechBluePrimaryLight, TechBluePrimary.copy(alpha = 0.15f))
        TechStatus.ON_SITE -> Triple(SafetyAmber, SafetyAmber, SafetyAmber.copy(alpha = 0.15f))
        TechStatus.AWAITING_PARTS -> Triple(Color(0xFFD8B4FE), Color(0xFFD8B4FE), Color(0xFF6B21A8).copy(alpha = 0.15f))
        TechStatus.ON_BREAK -> Triple(Color(0xFF94A3B8), Color(0xFF94A3B8), Color(0xFF334155))
        TechStatus.OFFLINE,
        TechStatus.OFF_DUTY -> Triple(Color(0xFF64748B), Color(0xFF94A3B8), Color(0xFF1E293B))
        TechStatus.EMERGENCY_ASSIST -> Triple(HazardRed, HazardRed, HazardRed.copy(alpha = 0.2f))
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = 11.sp
            )
        }
    }
}
