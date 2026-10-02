package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.TechnicianEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.components.ChecklistCard
import com.example.ui.components.TaskManagementDashboard
import com.example.ui.components.TaskPriorityBadge
import com.example.ui.components.TaskStatusBadge
import com.example.ui.theme.DjezzyRedLight
import com.example.ui.theme.HazardRed
import com.example.ui.theme.OperationalEmerald
import com.example.ui.theme.SafetyAmber
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TechBluePrimary
import com.example.ui.theme.TechBluePrimaryLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    currentUser: UserEntity,
    tasks: List<TaskEntity>,
    technicians: List<TechnicianEntity>,
    selectedTask: TaskEntity?,
    onSelectTask: (TaskEntity?) -> Unit,
    onAcceptTask: (taskId: String, techId: String, techName: String) -> Unit,
    onDeclineTask: (taskId: String, techId: String, techName: String, reason: String) -> Unit,
    onUpdateStatus: (taskId: String, newStatus: TaskStatus, techId: String, techName: String) -> Unit,
    onToggleChecklist: (taskId: String, index: Int) -> Unit,
    onCompleteTask: (taskId: String, notes: String, signature: String, techId: String, techName: String) -> Unit,
    onDispatchTask: (taskId: String, techId: String, techName: String, dispatcherName: String) -> Unit,
    onCreateTask: (
        title: String, description: String, assetTag: String, category: String,
        siteName: String, address: String, lat: Double, lng: Double,
        priority: TaskPriority, hazard: String?, tools: String, sla: String,
        techId: String?, techName: String?, actor: String
    ) -> Unit,
    onNavigateToAnalytics: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var taskToDecline by remember { mutableStateOf<TaskEntity?>(null) }
    var declineReason by remember { mutableStateOf("Lacking high-voltage arc flash certification for 13.8kV equipment") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDispatchDialogForTask by remember { mutableStateOf<TaskEntity?>(null) }

    // Completion modal state
    var showCompletionDialog by remember { mutableStateOf<TaskEntity?>(null) }
    var resolutionNotes by remember { mutableStateOf("") }
    var signatureInput by remember { mutableStateOf(currentUser.fullName) }

    // Check if there is an urgent dispatched task awaiting current technician's acceptance
    val pendingAcceptanceTask = remember(tasks, currentUser) {
        tasks.firstOrNull { it.assignedTechId == currentUser.id && it.status == TaskStatus.DISPATCHED }
    }

    Box(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                // Screen Title & Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = "FIELD DISPATCH QUEUE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "Active assignments & service orders",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            maxLines = 1
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onNavigateToAnalytics != null) {
                            OutlinedButton(
                                onClick = onNavigateToAnalytics,
                                border = BorderStroke(1.dp, DjezzyRedLight.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .testTag("view_d3_trends_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = DjezzyRedLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "D3 Trends",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DjezzyRedLight,
                                    maxLines = 1
                                )
                            }
                        }

                        if (currentUser.role == UserRole.DISPATCHER) {
                            Button(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = TechBluePrimary),
                                modifier = Modifier.testTag("create_work_order_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Order", fontSize = 12.sp, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }

            // Pending Acceptance Banner Alert
            if (pendingAcceptanceTask != null) {
                item {
                    PendingTaskAlertBanner(
                        task = pendingAcceptanceTask,
                        onAccept = {
                            onAcceptTask(pendingAcceptanceTask.id, currentUser.id, currentUser.fullName)
                        },
                        onDecline = {
                            taskToDecline = pendingAcceptanceTask
                        }
                    )
                }
            }

            // Central Task Management Dashboard Component
            item {
                TaskManagementDashboard(
                    tasks = tasks,
                    currentUser = currentUser,
                    onAcceptTask = { task ->
                        onAcceptTask(task.id, currentUser.id, currentUser.fullName)
                    },
                    onSelectTask = { task ->
                        if (selectedTask?.id == task.id) onSelectTask(null) else onSelectTask(task)
                    },
                    onStartRoute = { task ->
                        onUpdateStatus(task.id, TaskStatus.EN_ROUTE, currentUser.id, currentUser.fullName)
                    },
                    onCompleteTask = { task ->
                        showCompletionDialog = task
                        resolutionNotes = ""
                    }
                )
            }

            // Expanded Active Work Order Inspection Detail View
            if (selectedTask != null) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "WORK ORDER INSPECTION & SIGN-OFF",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = DjezzyRedLight,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TaskCardItem(
                        task = selectedTask,
                        currentUser = currentUser,
                        isExpanded = true,
                        onToggleExpand = { onSelectTask(null) },
                        onAccept = {
                            onAcceptTask(selectedTask.id, currentUser.id, currentUser.fullName)
                        },
                        onDecline = {
                            taskToDecline = selectedTask
                        },
                        onUpdateStatus = { newStatus ->
                            onUpdateStatus(selectedTask.id, newStatus, currentUser.id, currentUser.fullName)
                        },
                        onToggleChecklist = { index ->
                            onToggleChecklist(selectedTask.id, index)
                        },
                        onOpenCompleteDialog = {
                            showCompletionDialog = selectedTask
                            resolutionNotes = ""
                        },
                        onOpenDispatchDialog = {
                            showDispatchDialogForTask = selectedTask
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Decline Work Order Dialog
    if (taskToDecline != null) {
        val t = taskToDecline!!
        AlertDialog(
            onDismissRequest = { taskToDecline = null },
            title = {
                Text("Decline Work Order: ${t.id}", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Provide a documented reason for assignment refusal for dispatch audit records:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = declineReason,
                        onValueChange = { declineReason = it },
                        label = { Text("Refusal Reason") },
                        modifier = Modifier.fillMaxWidth().testTag("decline_reason_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeclineTask(t.id, currentUser.id, currentUser.fullName, declineReason)
                        taskToDecline = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HazardRed),
                    modifier = Modifier.testTag("confirm_decline_button")
                ) {
                    Text("Decline Assignment")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { taskToDecline = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Complete Work Order & Signoff Dialog
    if (showCompletionDialog != null) {
        val t = showCompletionDialog!!
        AlertDialog(
            onDismissRequest = { showCompletionDialog = null },
            title = {
                Text("Work Order Completion Sign-Off", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Order: ${t.title} (${t.id})",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = OperationalEmerald
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = resolutionNotes,
                        onValueChange = { resolutionNotes = it },
                        label = { Text("Resolution Notes & Actions Taken") },
                        placeholder = { Text("Replaced mechanical seal and calibrated torque...") },
                        modifier = Modifier.fillMaxWidth().testTag("resolution_notes_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = signatureInput,
                        onValueChange = { signatureInput = it },
                        label = { Text("Technician Electronic Signature") },
                        modifier = Modifier.fillMaxWidth().testTag("signature_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCompleteTask(t.id, resolutionNotes, signatureInput, currentUser.id, currentUser.fullName)
                        showCompletionDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OperationalEmerald),
                    modifier = Modifier.testTag("confirm_complete_button")
                ) {
                    Text("Sign & Close Order")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCompletionDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dispatcher Assign Modal
    if (showDispatchDialogForTask != null) {
        val t = showDispatchDialogForTask!!
        AlertDialog(
            onDismissRequest = { showDispatchDialogForTask = null },
            title = {
                Text("Dispatch Assignment: ${t.id}", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Select technician to dispatch this order to:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    technicians.forEach { tech ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onDispatchTask(t.id, tech.id, tech.name, currentUser.fullName)
                                    showDispatchDialogForTask = null
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = tech.name, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(text = "${tech.roleTitle} • ${tech.status.label}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                }
                                Text("Assign", color = TechBluePrimaryLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDispatchDialogForTask = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Create New Work Order Dialog
    if (showCreateDialog) {
        CreateWorkOrderDialog(
            technicians = technicians,
            onDismiss = { showCreateDialog = false },
            onCreate = { title, desc, tag, cat, site, addr, lat, lng, prio, hazard, tools, sla, techId, techName ->
                onCreateTask(title, desc, tag, cat, site, addr, lat, lng, prio, hazard, tools, sla, techId, techName, currentUser.fullName)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun PendingTaskAlertBanner(
    task: TaskEntity,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1B07)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SafetyAmber)),
        modifier = Modifier.fillMaxWidth().testTag("pending_task_banner")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SafetyAmber),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NEW DISPATCH ASSIGNMENT DISPATCHED TO YOU",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SafetyAmber,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${task.siteName} • ${task.address} • SLA: ${task.slaDeadline}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFDE68A)
            )

            if (!task.hazardAlert.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "HAZARD: ${task.hazardAlert}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFCA5A5),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onDecline,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5)),
                    modifier = Modifier.weight(1f).testTag("decline_task_button")
                ) {
                    Text("Decline")
                }
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = OperationalEmerald),
                    modifier = Modifier.weight(1.5f).testTag("accept_task_button")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ACCEPT & START ROUTE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun TaskCardItem(
    task: TaskEntity,
    currentUser: UserEntity,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onUpdateStatus: (TaskStatus) -> Unit,
    onToggleChecklist: (Int) -> Unit,
    onOpenCompleteDialog: () -> Unit,
    onOpenDispatchDialog: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161F2E)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isExpanded) TechBluePrimary else SlateCardBorder
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onToggleExpand)
            .testTag("task_item_${task.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Priority & Status header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TaskPriorityBadge(task.priority)
                TaskStatusBadge(task.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${task.siteName} (${task.equipmentCategory})",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Assigned Tech & SLA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Assigned: ${task.assignedTechName ?: "Unassigned"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFCBD5E1)
                )
                Text(
                    text = "SLA: ${task.slaDeadline}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
            }

            // Expanded Work Order Details View
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!task.hazardAlert.isNullOrEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = HazardRed.copy(alpha = 0.15f),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(HazardRed.copy(alpha = 0.4f))),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = HazardRed, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("SAFETY & HAZARD ALERT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = HazardRed)
                                    Text(task.hazardAlert, style = MaterialTheme.typography.bodySmall, color = Color(0xFFFCA5A5))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Required Tools
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("REQUIRED TOOLS & INSTRUMENTS", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                            Text(task.requiredTools, style = MaterialTheme.typography.bodySmall, color = Color(0xFFE2E8F0))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Safety Checklist
                    ChecklistCard(
                        checklistRaw = task.checklistJson,
                        onToggleItem = onToggleChecklist
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons based on state
                    if (task.status == TaskStatus.DISPATCHED && task.assignedTechId == currentUser.id) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f)) {
                                Text("Decline", color = Color(0xFFFCA5A5))
                            }
                            Button(onClick = onAccept, colors = ButtonDefaults.buttonColors(containerColor = OperationalEmerald), modifier = Modifier.weight(1.5f)) {
                                Text("Accept Task")
                            }
                        }
                    } else if (task.status == TaskStatus.ACCEPTED && task.assignedTechId == currentUser.id) {
                        Button(
                            onClick = { onUpdateStatus(TaskStatus.EN_ROUTE) },
                            colors = ButtonDefaults.buttonColors(containerColor = TechBluePrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("START EN ROUTE TO SITE")
                        }
                    } else if (task.status == TaskStatus.EN_ROUTE && task.assignedTechId == currentUser.id) {
                        Button(
                            onClick = { onUpdateStatus(TaskStatus.ON_SITE_WORKING) },
                            colors = ButtonDefaults.buttonColors(containerColor = SafetyAmber),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("CONFIRM ARRIVED ON-SITE")
                        }
                    } else if (task.status == TaskStatus.ON_SITE_WORKING && task.assignedTechId == currentUser.id) {
                        Button(
                            onClick = onOpenCompleteDialog,
                            colors = ButtonDefaults.buttonColors(containerColor = OperationalEmerald),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("COMPLETE & SIGN OFF WORK ORDER")
                        }
                    } else if (currentUser.role == UserRole.DISPATCHER && task.assignedTechId == null) {
                        Button(
                            onClick = onOpenDispatchDialog,
                            colors = ButtonDefaults.buttonColors(containerColor = TechBluePrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dispatch to Available Technician")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateWorkOrderDialog(
    technicians: List<TechnicianEntity>,
    onDismiss: () -> Unit,
    onCreate: (
        title: String, description: String, assetTag: String, category: String,
        siteName: String, address: String, lat: Double, lng: Double,
        priority: TaskPriority, hazard: String?, tools: String, sla: String,
        techId: String?, techName: String?
    ) -> Unit
) {
    var title by remember { mutableStateOf("Emergency Generator Transfer Switch Trip") }
    var description by remember { mutableStateOf("Standby diesel generator ATS failed to transfer load during weekly breaker exercise test.") }
    var assetTag by remember { mutableStateOf("GEN-ATS-04") }
    var category by remember { mutableStateOf("Power Generation") }
    var siteName by remember { mutableStateOf("West Loop Distribution Center") }
    var address by remember { mutableStateOf("500 W Madison St, Chicago") }
    var priority by remember { mutableStateOf(TaskPriority.P2_HIGH) }
    var hazard by remember { mutableStateOf("Automatic starting generator. 480V three-phase busbars.") }
    var tools by remember { mutableStateOf("Phase rotation meter, Multimeter, Insulated ratchet set") }
    var sla by remember { mutableStateOf("Today 6:00 PM") }
    var selectedTech by remember { mutableStateOf(technicians.firstOrNull()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Field Work Order", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task Title") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(value = siteName, onValueChange = { siteName = it }, label = { Text("Site Facility Name") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Site Address") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(value = assetTag, onValueChange = { assetTag = it }, label = { Text("Asset Tag / Category") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(value = hazard, onValueChange = { hazard = it }, label = { Text("Hazard Alert / PPE") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(value = tools, onValueChange = { tools = it }, label = { Text("Required Tools") }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(
                        title, description, assetTag, category, siteName, address,
                        41.8820, -87.6410, priority, hazard, tools, sla,
                        selectedTech?.id, selectedTech?.name
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = TechBluePrimary)
            ) {
                Text("Dispatch Order")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
