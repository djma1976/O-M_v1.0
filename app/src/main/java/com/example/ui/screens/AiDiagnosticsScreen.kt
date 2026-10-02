package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.DiagnosticResult
import com.example.ui.theme.HazardRed
import com.example.ui.theme.OperationalEmerald
import com.example.ui.theme.SafetyAmber
import com.example.ui.theme.SlateCardBorder
import com.example.ui.theme.TechBluePrimary
import com.example.ui.theme.TechBluePrimaryLight

@Composable
fun AiDiagnosticsScreen(
    selectedEquipment: String,
    symptomsInput: String,
    siteConditions: String,
    hazardAlert: String,
    isAnalyzing: Boolean,
    analysisResult: DiagnosticResult?,
    onUpdateEquipment: (String) -> Unit,
    onUpdateSymptoms: (String) -> Unit,
    onUpdateConditions: (String) -> Unit,
    onUpdateHazards: (String) -> Unit,
    onLoadPreset: (String, String, String, String) -> Unit,
    onRunDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isThinkingExpanded by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = TechBluePrimaryLight, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI FIELD DIAGNOSTICS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "Powered by Gemini 3.1 Pro (Thinking Mode High)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B)
                ) {
                    Text(
                        text = "THINKING: HIGH",
                        style = MaterialTheme.typography.labelSmall,
                        color = TechBluePrimaryLight,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Preset Quick Loads
        item {
            Text(
                text = "PRESET COMPLEX INDUSTRIAL SCENARIOS",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    PresetChip(
                        title = "Substation Transformer Arc Fault",
                        onClick = {
                            onLoadPreset(
                                "High-Voltage Substation Transformer (345kV/13.8kV)",
                                "Buchholz relay gas trip. Combustible gas accumulation: H2 450ppm, C2H2 35ppm. Oil temp 78°C under 70% load.",
                                "Northside Substation Alpha Yard, Ambient 34°C, 85% Humidity",
                                "Arc Flash 40 cal/cm², 35,000 Gallons Insulating Oil"
                            )
                        }
                    )
                }
                item {
                    PresetChip(
                        title = "Centrifugal Chiller Bearing Overheat",
                        onClick = {
                            onLoadPreset(
                                "Centrifugal 450-Ton Water Chiller",
                                "Drive-end bearing #2 RMS vibration 0.48 in/s (Alarm: 0.35 in/s). Sump oil differential 14 psi. High bearing temp 92°C.",
                                "Metro Data Center Basement Plant Room",
                                "Pressurized R-134a refrigerant lines (125 psig), Rotating shaft"
                            )
                        }
                    )
                }
                item {
                    PresetChip(
                        title = "Municipal Booster Pump Cavitation",
                        onClick = {
                            onLoadPreset(
                                "Horizontal Split-Case Water Booster Pump",
                                "Mechanical seal leaking 15 GPM. Discharge pressure fluctuating erratically 30-85 PSI with crackling cavitation noise.",
                                "Canal Pumping Station #4, Confined pit basin",
                                "Confined Space Entry (O2, H2S monitoring required), High pressure spray"
                            )
                        }
                    )
                }
            }
        }

        // Equipment & Symptoms Form Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161F2E)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SlateCardBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "EQUIPMENT TELEMETRY & OBSERVATIONS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TechBluePrimaryLight
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = selectedEquipment,
                        onValueChange = onUpdateEquipment,
                        label = { Text("Asset / Equipment Class") },
                        modifier = Modifier.fillMaxWidth().testTag("ai_equipment_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = symptomsInput,
                        onValueChange = onUpdateSymptoms,
                        label = { Text("Symptoms, Sensor Alarms, or Error Codes") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("ai_symptoms_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = siteConditions,
                        onValueChange = onUpdateConditions,
                        label = { Text("Site Environmental Conditions") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = hazardAlert,
                        onValueChange = onUpdateHazards,
                        label = { Text("Identified Hazards / PPE Requirements") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onRunDiagnostics,
                        enabled = !isAnalyzing && symptomsInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = TechBluePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("run_diagnostics_button")
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Deep Reasoning in Progress...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RUN HIGH-REASONING DIAGNOSTICS", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Diagnostic Results Display
        if (analysisResult != null) {
            val res = analysisResult

            // 1. Thinking Process Accordion Card (Mandatory Thinking Mode)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TechBluePrimaryLight)),
                    modifier = Modifier.fillMaxWidth().testTag("ai_thinking_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isThinkingExpanded = !isThinkingExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(TechBluePrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Psychology, contentDescription = null, tint = TechBluePrimaryLight, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "THINKING PROCESS (GEMINI 3.1 PRO)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TechBluePrimaryLight,
                                    letterSpacing = 0.8.sp
                                )
                            }
                            Icon(
                                if (isThinkingExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = TechBluePrimaryLight
                            )
                        }

                        AnimatedVisibility(visible = isThinkingExpanded) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                Text(
                                    text = res.thinkingProcess,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF93C5FD),
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // 2. Root Cause Analysis
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161F2E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = SafetyAmber, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ROOT CAUSE & ENGINEERING EVALUATION", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = SafetyAmber)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = res.rootCauseAnalysis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFF1F5F9),
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // 3. Immediate Safety & LOTO Protocol
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF281515)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(HazardRed.copy(alpha = 0.5f))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = HazardRed, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("MANDATORY SAFETY & LOCKOUT/TAGOUT", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = HazardRed)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        res.immediateSafetyActions.forEach { action ->
                            Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                                Text("•", color = HazardRed, fontWeight = FontWeight.Bold, modifier = Modifier.width(14.dp))
                                Text(action, style = MaterialTheme.typography.bodySmall, color = Color(0xFFFECACA))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Required PPE: ${res.requiredPpeLevel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFCA5A5),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 4. Step-by-Step Remediation Plan
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161F2E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OperationalEmerald, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ENGINEER REMEDIATION SEQUENCE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = OperationalEmerald)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        res.stepByStepRemediation.forEach { step ->
                            Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                                Text("✓", color = OperationalEmerald, fontWeight = FontWeight.Bold, modifier = Modifier.width(16.dp))
                                Text(step, style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1))
                            }
                        }
                    }
                }
            }

            // 5. Parts & SLA
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("RECOMMENDED REPLACEMENT COMPONENTS & SLA", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        res.recommendedParts.forEach { part ->
                            Text("• $part", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE2E8F0))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = TechBluePrimaryLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Estimated Repair Time: ${res.estimatedRepairTime}", style = MaterialTheme.typography.bodySmall, color = TechBluePrimaryLight, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun PresetChip(title: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E293B),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SlateCardBorder)),
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = TechBluePrimaryLight,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
