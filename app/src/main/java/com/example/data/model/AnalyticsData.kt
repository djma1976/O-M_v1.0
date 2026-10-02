package com.example.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class DailyTaskTrend(
    val dayLabel: String,
    val dateIso: String,
    val completed: Int,
    val created: Int,
    val onTimeSlaPct: Int
)

@Serializable
data class TechEfficiencyMetric(
    val techId: String,
    val name: String,
    val badge: String,
    val role: String,
    val closedTasks: Int,
    val efficiencyScore: Int,
    val onTimeSlaPct: Int,
    val avgResolutionHours: Double,
    val firstTimeFixPct: Int,
    val activeStatus: String
)

enum class AnalyticsTimeframe(val label: String, val days: Int) {
    WEEK_7_DAYS("Last 7 Days", 7),
    BIWEEKLY_14_DAYS("Last 14 Days", 14),
    MONTH_30_DAYS("Last 30 Days", 30)
}

enum class EfficiencyMetricType(val key: String, val label: String, val unit: String) {
    SCORE("score", "Efficiency Score", "%"),
    CLOSED_TASKS("tasks", "Tasks Completed", "tasks"),
    SLA_ON_TIME("sla", "SLA On-Time Rate", "%"),
    RESOLUTION_HOURS("mttr", "Mean Time to Resolve", "hrs")
}

object AnalyticsDataFactory {
    fun getWeeklyTaskTrends(): List<DailyTaskTrend> = listOf(
        DailyTaskTrend("Mon", "2026-09-28", completed = 14, created = 16, onTimeSlaPct = 93),
        DailyTaskTrend("Tue", "2026-09-29", completed = 19, created = 21, onTimeSlaPct = 95),
        DailyTaskTrend("Wed", "2026-09-30", completed = 26, created = 25, onTimeSlaPct = 96),
        DailyTaskTrend("Thu", "2026-10-01", completed = 22, created = 24, onTimeSlaPct = 91),
        DailyTaskTrend("Fri", "2026-10-02", completed = 31, created = 29, onTimeSlaPct = 98),
        DailyTaskTrend("Sat", "2026-10-03", completed = 17, created = 18, onTimeSlaPct = 94),
        DailyTaskTrend("Sun", "2026-10-04", completed = 11, created = 12, onTimeSlaPct = 90)
    )

    fun getBiWeeklyTaskTrends(): List<DailyTaskTrend> = listOf(
        DailyTaskTrend("W1-M", "2026-09-21", completed = 12, created = 15, onTimeSlaPct = 91),
        DailyTaskTrend("W1-T", "2026-09-22", completed = 16, created = 18, onTimeSlaPct = 94),
        DailyTaskTrend("W1-W", "2026-09-23", completed = 22, created = 20, onTimeSlaPct = 95),
        DailyTaskTrend("W1-T", "2026-09-24", completed = 19, created = 22, onTimeSlaPct = 89),
        DailyTaskTrend("W1-F", "2026-09-25", completed = 27, created = 25, onTimeSlaPct = 97),
        DailyTaskTrend("W1-S", "2026-09-26", completed = 14, created = 15, onTimeSlaPct = 92),
        DailyTaskTrend("W1-S", "2026-09-27", completed = 9, created = 11, onTimeSlaPct = 88),
        DailyTaskTrend("Mon", "2026-09-28", completed = 14, created = 16, onTimeSlaPct = 93),
        DailyTaskTrend("Tue", "2026-09-29", completed = 19, created = 21, onTimeSlaPct = 95),
        DailyTaskTrend("Wed", "2026-09-30", completed = 26, created = 25, onTimeSlaPct = 96),
        DailyTaskTrend("Thu", "2026-10-01", completed = 22, created = 24, onTimeSlaPct = 91),
        DailyTaskTrend("Fri", "2026-10-02", completed = 31, created = 29, onTimeSlaPct = 98),
        DailyTaskTrend("Sat", "2026-10-03", completed = 17, created = 18, onTimeSlaPct = 94),
        DailyTaskTrend("Sun", "2026-10-04", completed = 11, created = 12, onTimeSlaPct = 90)
    )

    fun getMonthlyTaskTrends(): List<DailyTaskTrend> = listOf(
        DailyTaskTrend("09/05", "2026-09-05", 14, 16, 92),
        DailyTaskTrend("09/08", "2026-09-08", 18, 19, 94),
        DailyTaskTrend("09/11", "2026-09-11", 24, 22, 95),
        DailyTaskTrend("09/14", "2026-09-14", 21, 23, 90),
        DailyTaskTrend("09/17", "2026-09-17", 28, 26, 96),
        DailyTaskTrend("09/20", "2026-09-20", 19, 18, 93),
        DailyTaskTrend("09/23", "2026-09-23", 26, 25, 95),
        DailyTaskTrend("09/26", "2026-09-26", 22, 24, 91),
        DailyTaskTrend("09/29", "2026-09-29", 30, 28, 97),
        DailyTaskTrend("10/02", "2026-10-02", 34, 31, 98),
        DailyTaskTrend("10/04", "2026-10-04", 16, 15, 94)
    )

    fun getTrendsForTimeframe(timeframe: AnalyticsTimeframe): List<DailyTaskTrend> = when (timeframe) {
        AnalyticsTimeframe.WEEK_7_DAYS -> getWeeklyTaskTrends()
        AnalyticsTimeframe.BIWEEKLY_14_DAYS -> getBiWeeklyTaskTrends()
        AnalyticsTimeframe.MONTH_30_DAYS -> getMonthlyTaskTrends()
    }

    fun computeTechMetrics(
        technicians: List<TechnicianEntity>,
        tasks: List<TaskEntity>
    ): List<TechEfficiencyMetric> {
        val completedTasksCount = tasks.count { it.status == TaskStatus.COMPLETED }
        return technicians.mapIndexed { index, tech ->
            val techAssignedCount = tasks.count { it.assignedTechId == tech.id }
            val closedCount = when (index) {
                0 -> 28 + (techAssignedCount * 2)
                1 -> 24 + techAssignedCount
                2 -> 21 + techAssignedCount
                3 -> 18 + techAssignedCount
                else -> 15 + techAssignedCount
            }
            val score = when (index) {
                0 -> 96
                1 -> 94
                2 -> 91
                3 -> 88
                else -> 85
            }
            val sla = when (index) {
                0 -> 98
                1 -> 96
                2 -> 93
                3 -> 89
                else -> 91
            }
            val mttr = when (index) {
                0 -> 1.4
                1 -> 1.8
                2 -> 2.1
                3 -> 2.6
                else -> 2.3
            }
            val firstFix = when (index) {
                0 -> 96
                1 -> 92
                2 -> 88
                3 -> 85
                else -> 87
            }
            TechEfficiencyMetric(
                techId = tech.id,
                name = tech.name,
                badge = tech.badgeNumber,
                role = tech.roleTitle,
                closedTasks = closedCount,
                efficiencyScore = score,
                onTimeSlaPct = sla,
                avgResolutionHours = mttr,
                firstTimeFixPct = firstFix,
                activeStatus = tech.status.name
            )
        }
    }

    fun buildD3AnalyticsJson(
        trends: List<DailyTaskTrend>,
        techMetrics: List<TechEfficiencyMetric>,
        selectedMetric: EfficiencyMetricType
    ): String {
        return buildJsonObject {
            put("metricType", selectedMetric.key)
            put("metricLabel", selectedMetric.label)
            put("metricUnit", selectedMetric.unit)
            put("weeklyTrends", buildJsonArray {
                trends.forEach { trend ->
                    add(buildJsonObject {
                        put("day", trend.dayLabel)
                        put("date", trend.dateIso)
                        put("completed", trend.completed)
                        put("created", trend.created)
                        put("slaPct", trend.onTimeSlaPct)
                    })
                }
            })
            put("techMetrics", buildJsonArray {
                techMetrics.forEach { tech ->
                    add(buildJsonObject {
                        put("id", tech.techId)
                        put("name", tech.name)
                        put("badge", tech.badge)
                        put("role", tech.role)
                        put("closedTasks", tech.closedTasks)
                        put("score", tech.efficiencyScore)
                        put("sla", tech.onTimeSlaPct)
                        put("mttr", tech.avgResolutionHours)
                        put("firstFix", tech.firstTimeFixPct)
                        put("status", tech.activeStatus)
                    })
                }
            })
        }.toString()
    }
}
