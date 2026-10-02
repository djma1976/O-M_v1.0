package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskEntity
import com.example.data.model.TaskStatus
import com.example.data.model.UserEntity
import com.example.ui.theme.DjezzyRed
import com.example.ui.theme.DjezzyRedLight
import com.example.ui.theme.HazardRed
import com.example.ui.theme.OperationalEmerald
import com.example.ui.theme.SafetyAmber
import com.example.ui.theme.SlateCardBorder

enum class TaskDashboardFilter(val label: String) {
    ALL("All Assignments"),
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    MINE("Assigned To Me")
}

@Composable
fun TaskManagementDashboard(
    tasks: List<TaskEntity>,
    currentUser: UserEntity,
    onAcceptTask: (TaskEntity) -> Unit,
    onSelectTask: (TaskEntity) -> Unit,
    onStartRoute: ((TaskEntity) -> Unit)? = null,
    onCompleteTask: ((TaskEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var activeFilter by remember { mutableStateOf(TaskDashboardFilter.ALL) }

    // Aggregate status counts
    val pendingCount = tasks.count { it.status == TaskStatus.PENDING_DISPATCH || it.status == TaskStatus.DISPATCHED }
    val inProgressCount = tasks.count {
        it.status == TaskStatus.ACCEPTED || it.status == TaskStatus.EN_ROUTE || it.status == TaskStatus.ON_SITE_WORKING
    }
    val completedCount = tasks.count { it.status == TaskStatus.COMPLETED }

    val filteredList = remember(tasks, activeFilter, currentUser) {
        when (activeFilter) {
            TaskDashboardFilter.ALL -> tasks
            TaskDashboardFilter.PENDING -> tasks.filter {
                it.status == TaskStatus.PENDING_DISPATCH || it.status == TaskStatus.DISPATCHED
            }
            TaskDashboardFilter.IN_PROGRESS -> tasks.filter {
                it.status == TaskStatus.ACCEPTED || it.status == TaskStatus.EN_ROUTE || it.status == TaskStatus.ON_SITE_WORKING
            }
            TaskDashboardFilter.COMPLETED -> tasks.filter { it.status == TaskStatus.COMPLETED }
            TaskDashboardFilter.MINE -> tasks.filter { it.assignedTechId == currentUser.id }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_management_dashboard")
    ) {
        // 1. Executive Metrics Strip (Pending / In Progress / Completed)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusKpiCard(
                label = "Pending",
                count = pendingCount,
                accentColor = SafetyAmber,
                icon = Icons.Default.HourglassEmpty,
                isSelected = activeFilter == TaskDashboardFilter.PENDING,
                onClick = { activeFilter = TaskDashboardFilter.PENDING },
                modifier = Modifier.weight(1f)
            )
            StatusKpiCard(
                label = "In Progress",
                count = inProgressCount,
                accentColor = DjezzyRed,
                icon = Icons.Default.PlayArrow,
                isSelected = activeFilter == TaskDashboardFilter.IN_PROGRESS,
                onClick = { activeFilter = TaskDashboardFilter.IN_PROGRESS },
                modifier = Modifier.weight(1f)
            )
            StatusKpiCard(
                label = "Completed",
                count = completedCount,
                accentColor = OperationalEmerald,
                icon = Icons.Default.CheckCircle,
                isSelected = activeFilter == TaskDashboardFilter.COMPLETED,
                onClick = { activeFilter = TaskDashboardFilter.COMPLETED },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Filter Pills Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(TaskDashboardFilter.values()) { filter ->
                FilterChip(
                    selected = activeFilter == filter,
                    onClick = { activeFilter = filter },
                    label = {
                        Text(
                            text = when (filter) {
                                TaskDashboardFilter.ALL -> "All (${tasks.size})"
                                TaskDashboardFilter.PENDING -> "Pending ($pendingCount)"
                                TaskDashboardFilter.IN_PROGRESS -> "In Progress ($inProgressCount)"
                                TaskDashboardFilter.COMPLETED -> "Completed ($completedCount)"
                                TaskDashboardFilter.MINE -> "My Orders"
                            },
                            fontSize = 11.sp,
                            fontWeight = if (activeFilter == filter) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DjezzyRed,
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Maintenance Assignments List
        if (filteredList.isEmpty()) {
            EmptyTasksState(filter = activeFilter)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredList.forEach { task ->
                    MaintenanceTaskItemCard(
                        task = task,
                        currentUser = currentUser,
                        onAccept = { onAcceptTask(task) },
                        onSelect = { onSelectTask(task) },
                        onStartRoute = onStartRoute?.let { { it(task) } },
                        onComplete = onCompleteTask?.let { { it(task) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusKpiCard(
    label: String,
    count: Int,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF161F2E),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isSelected) accentColor else SlateCardBorder
            )
        ),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("kpi_card_${label.lowercase().replace(" ", "_")}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(15.dp))
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun MaintenanceTaskItemCard(
    task: TaskEntity,
    currentUser: UserEntity,
    onAccept: () -> Unit,
    onSelect: () -> Unit,
    onStartRoute: (() -> Unit)? = null,
    onComplete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isAssignedToMe = task.assignedTechId == currentUser.id
    val isPendingAcceptance = task.status == TaskStatus.DISPATCHED || task.status == TaskStatus.PENDING_DISPATCH

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161F2E)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isPendingAcceptance && isAssignedToMe) DjezzyRed else SlateCardBorder
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect)
            .testTag("maintenance_task_card_${task.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Status indicator badge & Priority
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DashboardStatusPill(status = task.status)
                TaskPriorityBadge(priority = task.priority)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Task Title
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Site and Equipment Category
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = DjezzyRedLight, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${task.siteName} • ${task.equipmentCategory}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // SLA & Assigned Technician
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SLA: ${task.slaDeadline}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
                Text(
                    text = if (task.assignedTechName != null) "Tech: ${task.assignedTechName}" else "Unassigned",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isAssignedToMe) DjezzyRedLight else Color(0xFF64748B),
                    fontWeight = if (isAssignedToMe) FontWeight.Bold else FontWeight.Normal
                )
            }

            // Hazard warning strip if applicable
            if (!task.hazardAlert.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = HazardRed.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = HazardRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = task.hazardAlert,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFCA5A5),
                            maxLines = 1,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: prominent "Accept Task" for technicians
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSelect,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Inspect", fontSize = 12.sp, color = Color(0xFFCBD5E1))
                }

                if (task.status == TaskStatus.DISPATCHED && (isAssignedToMe || currentUser.role.name.contains("DISPATCHER"))) {
                    // ACCEPT TASK ACTION BUTTON
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = DjezzyRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.6f)
                            .testTag("accept_task_action_button_${task.id}")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Accept Task",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                } else if (task.status == TaskStatus.PENDING_DISPATCH) {
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = DjezzyRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.6f)
                            .testTag("claim_task_action_button_${task.id}")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Accept & Claim",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                } else if (task.status == TaskStatus.ACCEPTED && isAssignedToMe && onStartRoute != null) {
                    Button(
                        onClick = onStartRoute,
                        colors = ButtonDefaults.buttonColors(containerColor = DjezzyRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.6f)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Route", fontSize = 12.sp)
                    }
                } else if (task.status == TaskStatus.ON_SITE_WORKING && isAssignedToMe && onComplete != null) {
                    Button(
                        onClick = onComplete,
                        colors = ButtonDefaults.buttonColors(containerColor = OperationalEmerald),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.6f)
                    ) {
                        Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign & Complete", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardStatusPill(status: TaskStatus) {
    // Normalizes status into: 'Pending', 'In Progress', 'Completed'
    val (displayText, dotColor, bgColor, textColor) = when (status) {
        TaskStatus.PENDING_DISPATCH,
        TaskStatus.DISPATCHED -> Quadruple(
            "Pending",
            SafetyAmber,
            SafetyAmber.copy(alpha = 0.15f),
            SafetyAmber
        )
        TaskStatus.ACCEPTED,
        TaskStatus.EN_ROUTE,
        TaskStatus.ON_SITE_WORKING,
        TaskStatus.AWAITING_PARTS -> Quadruple(
            "In Progress",
            DjezzyRedLight,
            DjezzyRed.copy(alpha = 0.15f),
            DjezzyRedLight
        )
        TaskStatus.COMPLETED -> Quadruple(
            "Completed",
            OperationalEmerald,
            OperationalEmerald.copy(alpha = 0.15f),
            OperationalEmerald
        )
        TaskStatus.REJECTED -> Quadruple(
            "Declined",
            HazardRed,
            HazardRed.copy(alpha = 0.15f),
            HazardRed
        )
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = displayText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun EmptyTasksState(filter: TaskDashboardFilter) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF131D2D),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Assignment, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No ${filter.label} Tasks",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "All maintenance assignments in this category are up to date.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
