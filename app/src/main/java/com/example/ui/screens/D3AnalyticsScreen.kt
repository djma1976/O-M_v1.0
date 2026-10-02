package com.example.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.AnalyticsDataFactory
import com.example.data.model.AnalyticsTimeframe
import com.example.data.model.EfficiencyMetricType
import com.example.data.model.TaskEntity
import com.example.data.model.TechnicianEntity
import com.example.ui.components.AnalyticsJavaScriptInterface
import com.example.ui.components.D3ChartHtmlBuilder
import com.example.ui.components.DjezzyEmblem
import com.example.ui.theme.DjezzyRed
import com.example.ui.theme.DjezzyRedLight
import com.example.ui.theme.OperationalEmerald
import com.example.ui.theme.OperationalEmeraldLight
import com.example.ui.theme.SafetyAmber
import com.example.ui.theme.TechBluePrimary
import com.example.ui.theme.TechBluePrimaryLight

/**
 * Interactive D3.js powered Field Operations Analytics Dashboard.
 * Visualizes weekly task completion trends and technician efficiency rankings with dual-axis curves,
 * live SLA gauges, and touch-interactive metric switching.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun D3AnalyticsScreen(
    technicians: List<TechnicianEntity>,
    tasks: List<TaskEntity>,
    onSelectTechnicianOnMap: (TechnicianEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTimeframe by remember { mutableStateOf(AnalyticsTimeframe.WEEK_7_DAYS) }
    var selectedMetric by remember { mutableStateOf(EfficiencyMetricType.SCORE) }
    var inspectedTechId by remember { mutableStateOf<String?>(null) }

    // Derive technician efficiency metrics & trends based on live DB data
    val techMetrics = remember(technicians, tasks) {
        AnalyticsDataFactory.computeTechMetrics(technicians, tasks)
    }

    val trends = remember(selectedTimeframe) {
        AnalyticsDataFactory.getTrendsForTimeframe(selectedTimeframe)
    }

    val jsonPayload = remember(trends, techMetrics, selectedMetric) {
        AnalyticsDataFactory.buildD3AnalyticsJson(trends, techMetrics, selectedMetric)
    }

    val inspectedTech = remember(inspectedTechId, techMetrics, technicians) {
        inspectedTechId?.let { id ->
            technicians.firstOrNull { it.id == id }
        }
    }

    val inspectedMetric = remember(inspectedTechId, techMetrics) {
        inspectedTechId?.let { id ->
            techMetrics.firstOrNull { it.techId == id }
        }
    }

    // Bridge instance for bidirectional communication
    val analyticsBridge = remember {
        AnalyticsJavaScriptInterface(
            onTechnicianSelected = { techId ->
                inspectedTechId = techId
            },
            onTimeframeSelected = { timeframeKey ->
                when (timeframeKey) {
                    "14" -> selectedTimeframe = AnalyticsTimeframe.BIWEEKLY_14_DAYS
                    "30" -> selectedTimeframe = AnalyticsTimeframe.MONTH_30_DAYS
                    else -> selectedTimeframe = AnalyticsTimeframe.WEEK_7_DAYS
                }
            },
            onMetricSelected = { metricKey ->
                when (metricKey) {
                    "tasks" -> selectedMetric = EfficiencyMetricType.CLOSED_TASKS
                    "sla" -> selectedMetric = EfficiencyMetricType.SLA_ON_TIME
                    "mttr" -> selectedMetric = EfficiencyMetricType.RESOLUTION_HOURS
                    else -> selectedMetric = EfficiencyMetricType.SCORE
                }
            }
        )
    }

    LaunchedEffect(jsonPayload) {
        analyticsBridge.updateCachedJson(jsonPayload)
        analyticsBridge.pushDataToD3(jsonPayload)
    }

    DisposableEffect(Unit) {
        onDispose {
            analyticsBridge.detachWebView()
        }
    }

    val totalCompletedCount = trends.sumOf { it.completed }
    val avgSlaRate = if (trends.isNotEmpty()) trends.map { it.onTimeSlaPct }.average().toInt() else 94

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Top Header HUD Bar
        Surface(
            color = Color(0xFF111827),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DjezzyEmblem(size = 36.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "D3 ANALYTICS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = DjezzyRed.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "LIVE TRENDS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DjezzyRedLight,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Work Order Velocity & Technician Efficiency",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        analyticsBridge.pushDataToD3(jsonPayload)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("refresh_analytics_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Analytics",
                        tint = TechBluePrimaryLight
                    )
                }
            }
        }

        // Scrollable Analytics Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // KPI Highlight Tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KpiMetricTile(
                    title = "RESOLVED",
                    value = "$totalCompletedCount",
                    sub = "+18% vs prev",
                    accentColor = OperationalEmeraldLight,
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricTile(
                    title = "FLEET SLA",
                    value = "$avgSlaRate%",
                    sub = "Target: 92%",
                    accentColor = SafetyAmber,
                    icon = Icons.Default.Speed,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricTile(
                    title = "AVG MTTR",
                    value = "1.8h",
                    sub = "-0.4h optimal",
                    accentColor = TechBluePrimaryLight,
                    icon = Icons.Default.Schedule,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Timeframe Chip Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TIMEFRAME:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )
                AnalyticsTimeframe.values().forEach { timeframe ->
                    val isSelected = selectedTimeframe == timeframe
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) DjezzyRed else Color(0xFF1E293B),
                        border = BorderStroke(1.dp, if (isSelected) DjezzyRedLight else Color(0xFF334155)),
                        modifier = Modifier
                            .clickable { selectedTimeframe = timeframe }
                            .testTag("timeframe_chip_${timeframe.name.lowercase()}")
                    ) {
                        Text(
                            text = timeframe.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metric Dimension Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "METRIC:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )
                EfficiencyMetricType.values().forEach { metric ->
                    val isSelected = selectedMetric == metric
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) TechBluePrimary else Color(0xFF1E293B),
                        border = BorderStroke(1.dp, if (isSelected) TechBluePrimaryLight else Color(0xFF334155)),
                        modifier = Modifier
                            .clickable {
                                selectedMetric = metric
                                analyticsBridge.setMetricInD3(metric.key)
                            }
                            .testTag("metric_chip_${metric.key}")
                    ) {
                        Text(
                            text = metric.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // D3.js Charts WebView Card Container
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(640.dp)
                    .testTag("d3_analytics_webview_card")
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                allowFileAccess = true
                                cacheMode = WebSettings.LOAD_DEFAULT
                                useWideViewPort = true
                                loadWithOverviewMode = true
                            }
                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    analyticsBridge.pushDataToD3(jsonPayload)
                                }
                            }
                            addJavascriptInterface(analyticsBridge, "AndroidAnalyticsBridge")
                            analyticsBridge.attachWebView(this)
                            loadDataWithBaseURL(
                                "https://djezzy-ops-analytics.internal/",
                                D3ChartHtmlBuilder.buildAnalyticsHtml(),
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    },
                    update = {
                        analyticsBridge.pushDataToD3(jsonPayload)
                    }
                )
            }

            // Inspected Technician Deep-Dive Card
            AnimatedVisibility(visible = inspectedTech != null && inspectedMetric != null) {
                if (inspectedTech != null && inspectedMetric != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF162032)),
                        border = BorderStroke(1.dp, DjezzyRed.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inspected_tech_efficiency_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(DjezzyRed.copy(alpha = 0.2f))
                                    ) {
                                        Text(
                                            text = inspectedTech.name.split(" ").map { it.take(1) }.joinToString(""),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = DjezzyRedLight
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = inspectedTech.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${inspectedTech.roleTitle} • Badge #${inspectedTech.badgeNumber}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = OperationalEmerald.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${inspectedMetric.efficiencyScore}% SCORE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = OperationalEmeraldLight,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TechStatPill("Closed Tasks", "${inspectedMetric.closedTasks}")
                                TechStatPill("On-Time SLA", "${inspectedMetric.onTimeSlaPct}%")
                                TechStatPill("MTTR", "${inspectedMetric.avgResolutionHours}h")
                                TechStatPill("1st Time Fix", "${inspectedMetric.firstTimeFixPct}%")
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { onSelectTechnicianOnMap(inspectedTech) },
                                colors = ButtonDefaults.buttonColors(containerColor = DjezzyRed),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("locate_inspected_tech_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Track ${inspectedTech.name.substringBefore(" ")} on Geo Map",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun KpiMetricTile(
    title: String,
    value: String,
    sub: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF111827),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Text(
                text = sub,
                fontSize = 9.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun TechStatPill(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(text = label, fontSize = 9.sp, color = Color(0xFF64748B))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
    }
}
