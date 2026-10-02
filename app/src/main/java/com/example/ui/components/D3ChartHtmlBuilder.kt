package com.example.ui.components

/**
 * Generates the self-contained HTML/CSS/JavaScript document running D3.js (v7)
 * with robust SVG fallback rendering for field operations task trends and technician efficiency.
 */
object D3ChartHtmlBuilder {

    fun buildAnalyticsHtml(): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>Djezzy Field Operations D3 Analytics</title>
    <!-- D3.js v7 CDN with fallback to embedded engine -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/d3/7.8.5/d3.min.js"></script>
    <style>
        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            -webkit-tap-highlight-color: transparent;
            user-select: none;
        }
        body {
            background-color: #0B0F19;
            color: #E2E8F0;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
            padding: 12px 14px 40px 14px;
            overflow-x: hidden;
        }

        .chart-card {
            background: #111827;
            border: 1px solid #1E293B;
            border-radius: 16px;
            padding: 14px 14px 16px 14px;
            margin-bottom: 16px;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.35);
        }

        .card-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 12px;
            padding-bottom: 8px;
            border-bottom: 1px solid #1E293B;
        }

        .card-title-group {
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .indicator-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background-color: #E11D48;
            box-shadow: 0 0 8px #E11D48;
        }

        .card-title {
            font-size: 13px;
            font-weight: 700;
            letter-spacing: 0.5px;
            text-transform: uppercase;
            color: #F8FAFC;
        }

        .card-subtitle {
            font-size: 10px;
            color: #94A3B8;
            margin-top: 2px;
        }

        .legend-row {
            display: flex;
            align-items: center;
            gap: 12px;
            font-size: 10px;
            color: #94A3B8;
        }

        .legend-item {
            display: flex;
            align-items: center;
            gap: 5px;
        }

        .legend-color {
            width: 12px;
            height: 4px;
            border-radius: 2px;
        }
        .color-completed { background: #10B981; }
        .color-created { background: #3B82F6; }
        .color-sla { background: #F59E0B; }

        /* SVG Chart Styles */
        svg {
            width: 100%;
            height: auto;
            overflow: visible;
            display: block;
        }

        .grid-line {
            stroke: #1E293B;
            stroke-dasharray: 3 3;
            stroke-width: 1;
        }

        .axis-text {
            fill: #64748B;
            font-size: 10px;
            font-weight: 500;
        }

        .trend-area {
            fill: url(#completedAreaGrad);
        }

        .trend-line-completed {
            fill: none;
            stroke: #10B981;
            stroke-width: 2.5;
            stroke-linecap: round;
        }

        .trend-line-created {
            fill: none;
            stroke: #3B82F6;
            stroke-width: 2;
            stroke-dasharray: 4 3;
            stroke-linecap: round;
        }

        .data-point {
            cursor: pointer;
            transition: transform 0.2s ease;
        }

        .data-point:active {
            transform: scale(1.3);
        }

        /* Tooltip */
        #d3-tooltip {
            position: absolute;
            display: none;
            background: rgba(15, 23, 42, 0.95);
            border: 1px solid #334155;
            border-radius: 10px;
            padding: 8px 12px;
            font-size: 11px;
            color: #F8FAFC;
            pointer-events: none;
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.6);
            backdrop-filter: blur(8px);
            z-index: 100;
            white-space: nowrap;
        }

        #d3-tooltip .tt-title {
            font-weight: 700;
            color: #F1F5F9;
            margin-bottom: 4px;
            font-size: 11px;
            border-bottom: 1px solid #334155;
            padding-bottom: 3px;
        }

        #d3-tooltip .tt-row {
            display: flex;
            justify-content: space-between;
            gap: 12px;
            margin-top: 3px;
        }

        /* Tech Efficiency List */
        .tech-row {
            background: #162032;
            border: 1px solid #1E293B;
            border-radius: 12px;
            padding: 10px 12px;
            margin-bottom: 8px;
            display: flex;
            flex-direction: column;
            gap: 6px;
            cursor: pointer;
            transition: all 0.2s ease;
        }

        .tech-row:active, .tech-row.selected {
            background: #1E293B;
            border-color: #E11D48;
            transform: translateY(-1px);
        }

        .tech-info-line {
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .tech-avatar-group {
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .tech-badge-avatar {
            width: 28px;
            height: 28px;
            border-radius: 8px;
            background: #0F172A;
            border: 1px solid #334155;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 10px;
            font-weight: 700;
            color: #E11D48;
        }

        .tech-name {
            font-size: 12px;
            font-weight: 600;
            color: #F8FAFC;
        }

        .tech-role {
            font-size: 10px;
            color: #94A3B8;
        }

        .tech-metric-val {
            font-size: 13px;
            font-weight: 700;
            color: #34D399;
        }

        .tech-bar-track {
            width: 100%;
            height: 7px;
            background: #0F172A;
            border-radius: 4px;
            overflow: hidden;
            position: relative;
        }

        .tech-bar-fill {
            height: 100%;
            border-radius: 4px;
            transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
        }

        .tech-meta-row {
            display: flex;
            justify-content: space-between;
            font-size: 9.5px;
            color: #64748B;
        }

        .status-chip {
            padding: 2px 6px;
            border-radius: 4px;
            font-size: 9px;
            font-weight: 600;
            text-transform: uppercase;
        }
        .status-AVAILABLE { background: rgba(16, 185, 129, 0.2); color: #34D399; }
        .status-EN_ROUTE { background: rgba(59, 130, 246, 0.2); color: #60A5FA; }
        .status-ON_SITE { background: rgba(245, 158, 11, 0.2); color: #FBBF24; }
        .status-OFF_DUTY { background: rgba(100, 116, 139, 0.2); color: #94A3B8; }

        /* Donut KPI section */
        .gauge-container {
            display: flex;
            align-items: center;
            justify-content: space-around;
            padding: 8px 0;
        }

        .gauge-svg-wrap {
            width: 110px;
            height: 110px;
            position: relative;
        }

        .gauge-center-text {
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            text-align: center;
        }

        .gauge-pct {
            font-size: 18px;
            font-weight: 800;
            color: #10B981;
        }

        .gauge-label {
            font-size: 9px;
            color: #94A3B8;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .gauge-stats {
            display: flex;
            flex-direction: column;
            gap: 6px;
            font-size: 11px;
        }

        .gauge-stat-pill {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 4px 8px;
            background: #0F172A;
            border-radius: 6px;
            border: 1px solid #1E293B;
        }
    </style>
</head>
<body>

    <div id="d3-tooltip"></div>

    <!-- Chart 1: Task Completion Trends -->
    <div class="chart-card">
        <div class="card-header">
            <div class="card-title-group">
                <div class="indicator-dot"></div>
                <div>
                    <div class="card-title">Task Completion Trends</div>
                    <div class="card-subtitle">Weekly Completed vs Created Work Orders</div>
                </div>
            </div>
            <div class="legend-row">
                <div class="legend-item"><div class="legend-color color-completed"></div>Completed</div>
                <div class="legend-item"><div class="legend-color color-created"></div>Created</div>
            </div>
        </div>

        <div id="trends-chart-container">
            <svg id="trends-svg" viewBox="0 0 360 170"></svg>
        </div>
    </div>

    <!-- Chart 2: Fleet SLA Performance Gauge -->
    <div class="chart-card">
        <div class="card-header">
            <div class="card-title-group">
                <div class="indicator-dot" style="background:#10B981; box-shadow:0 0 8px #10B981;"></div>
                <div>
                    <div class="card-title">Fleet SLA Compliance</div>
                    <div class="card-subtitle">On-Time Performance & Resolution Quality</div>
                </div>
            </div>
        </div>
        <div class="gauge-container">
            <div class="gauge-svg-wrap">
                <svg id="sla-donut-svg" viewBox="0 0 110 110"></svg>
                <div class="gauge-center-text">
                    <div class="gauge-pct" id="sla-pct-display">94.8%</div>
                    <div class="gauge-label">On-Time</div>
                </div>
            </div>
            <div class="gauge-stats">
                <div class="gauge-stat-pill">
                    <span style="color:#10B981; font-weight:700;">●</span>
                    <span style="color:#CBD5E1;">Within SLA:</span>
                    <strong style="color:#F8FAFC;" id="stat-within-sla">94.8%</strong>
                </div>
                <div class="gauge-stat-pill">
                    <span style="color:#F59E0B; font-weight:700;">●</span>
                    <span style="color:#CBD5E1;">Minor Delay:</span>
                    <strong style="color:#F8FAFC;" id="stat-delay">3.8%</strong>
                </div>
                <div class="gauge-stat-pill">
                    <span style="color:#EF4444; font-weight:700;">●</span>
                    <span style="color:#CBD5E1;">Overdue:</span>
                    <strong style="color:#F8FAFC;" id="stat-overdue">1.4%</strong>
                </div>
            </div>
        </div>
    </div>

    <!-- Chart 3: Technician Efficiency Metrics -->
    <div class="chart-card">
        <div class="card-header">
            <div class="card-title-group">
                <div class="indicator-dot" style="background:#3B82F6; box-shadow:0 0 8px #3B82F6;"></div>
                <div>
                    <div class="card-title">Technician Efficiency Ranking</div>
                    <div class="card-subtitle" id="tech-ranking-subtitle">Ranked by Efficiency Score</div>
                </div>
            </div>
            <div style="font-size:10px; color:#60A5FA; font-weight:600;">Tap tech to inspect</div>
        </div>

        <div id="tech-efficiency-list">
            <!-- Populated reactively via D3 / JS -->
        </div>
    </div>

    <script>
        // Global state
        var currentData = {
            metricType: 'score',
            metricLabel: 'Efficiency Score',
            metricUnit: '%',
            weeklyTrends: [
                { day: 'Mon', date: '2026-09-28', completed: 14, created: 16, slaPct: 93 },
                { day: 'Tue', date: '2026-09-29', completed: 19, created: 21, slaPct: 95 },
                { day: 'Wed', date: '2026-09-30', completed: 26, created: 25, slaPct: 96 },
                { day: 'Thu', date: '2026-10-01', completed: 22, created: 24, slaPct: 91 },
                { day: 'Fri', date: '2026-10-02', completed: 31, created: 29, slaPct: 98 },
                { day: 'Sat', date: '2026-10-03', completed: 17, created: 18, slaPct: 94 },
                { day: 'Sun', date: '2026-10-04', completed: 11, created: 12, slaPct: 90 }
            ],
            techMetrics: [
                { id: 'tech-1', name: 'Karim Benali', badge: 'DZ-041', role: 'Lead Microwave Engineer', closedTasks: 32, score: 96, sla: 98, mttr: 1.4, firstFix: 96, status: 'AVAILABLE' },
                { id: 'tech-2', name: 'Amina Zerrouki', badge: 'DZ-088', role: '5G Core Specialist', closedTasks: 28, score: 94, sla: 96, mttr: 1.8, firstFix: 92, status: 'EN_ROUTE' },
                { id: 'tech-3', name: 'Youcef Kaci', badge: 'DZ-112', role: 'Fiber Optic Splicer', closedTasks: 24, score: 91, sla: 93, mttr: 2.1, firstFix: 88, status: 'ON_SITE' },
                { id: 'tech-4', name: 'Sofia Merabet', badge: 'DZ-167', role: 'Power & Generator Tech', closedTasks: 20, score: 88, sla: 89, mttr: 2.6, firstFix: 85, status: 'AVAILABLE' },
                { id: 'tech-5', name: 'Tariq Hamidi', badge: 'DZ-204', role: 'Tower Climber & Rigger', closedTasks: 17, score: 85, sla: 91, mttr: 2.3, firstFix: 87, status: 'AVAILABLE' }
            ]
        };

        var selectedTechId = null;

        function logToAndroid(msg) {
            if (window.AndroidAnalyticsBridge && window.AndroidAnalyticsBridge.onLog) {
                window.AndroidAnalyticsBridge.onLog(msg);
            } else {
                console.log(msg);
            }
        }

        // Render D3 Trend Area & Lines
        function renderTrendsChart() {
            var trends = currentData.weeklyTrends || [];
            if (!trends.length) return;

            var svg = document.getElementById('trends-svg');
            var width = 360;
            var height = 170;
            var padL = 28;
            var padR = 16;
            var padT = 16;
            var padB = 26;
            var plotW = width - padL - padR;
            var plotH = height - padT - padB;

            var maxVal = Math.max(
                d3.max(trends, function(d) { return Math.max(d.completed, d.created); }) || 30,
                30
            );

            // Compute scales
            var stepX = plotW / (trends.length - 1);
            function getX(i) { return padL + i * stepX; }
            function getY(val) { return padT + plotH - (val / maxVal) * plotH; }

            // Build SVG inner HTML with defs
            var svgHtml = '';
            svgHtml += '<defs>';
            svgHtml += '  <linearGradient id="completedAreaGrad" x1="0" y1="0" x2="0" y2="1">';
            svgHtml += '    <stop offset="0%" stop-color="#10B981" stop-opacity="0.35"/>';
            svgHtml += '    <stop offset="100%" stop-color="#10B981" stop-opacity="0.0"/>';
            svgHtml += '  </linearGradient>';
            svgHtml += '  <filter id="glow" x="-20%" y="-20%" width="140%" height="140%">';
            svgHtml += '    <feGaussianBlur stdDeviation="2" result="blur" />';
            svgHtml += '    <feComposite in="SourceGraphic" in2="blur" operator="over" />';
            svgHtml += '  </filter>';
            svgHtml += '</defs>';

            // Grid lines (3 horizontal lines)
            for (var g = 0; g <= 3; g++) {
                var gVal = Math.round((maxVal / 3) * g);
                var gy = getY(gVal);
                svgHtml += '<line class="grid-line" x1="' + padL + '" y1="' + gy + '" x2="' + (width - padR) + '" y2="' + gy + '"/>';
                svgHtml += '<text class="axis-text" x="' + (padL - 6) + '" y="' + (gy + 3) + '" text-anchor="end">' + gVal + '</text>';
            }

            // Target SLA Guideline at 92%
            var slaTargetY = getY(maxVal * 0.92);
            svgHtml += '<line x1="' + padL + '" y1="' + slaTargetY + '" x2="' + (width - padR) + '" y2="' + slaTargetY + '" stroke="#F59E0B" stroke-width="1" stroke-dasharray="2 2" opacity="0.6"/>';

            // Area path for Completed
            var areaD = 'M ' + getX(0) + ' ' + getY(trends[0].completed);
            for (var i = 1; i < trends.length; i++) {
                var cx1 = getX(i - 1) + stepX * 0.45;
                var cy1 = getY(trends[i - 1].completed);
                var cx2 = getX(i) - stepX * 0.45;
                var cy2 = getY(trends[i].completed);
                areaD += ' C ' + cx1 + ' ' + cy1 + ', ' + cx2 + ' ' + cy2 + ', ' + getX(i) + ' ' + getY(trends[i].completed);
            }
            areaD += ' L ' + getX(trends.length - 1) + ' ' + (padT + plotH);
            areaD += ' L ' + getX(0) + ' ' + (padT + plotH) + ' Z';
            svgHtml += '<path class="trend-area" d="' + areaD + '"/>';

            // Line path for Completed (solid green)
            var compLineD = 'M ' + getX(0) + ' ' + getY(trends[0].completed);
            for (var i = 1; i < trends.length; i++) {
                var cx1 = getX(i - 1) + stepX * 0.45;
                var cy1 = getY(trends[i - 1].completed);
                var cx2 = getX(i) - stepX * 0.45;
                var cy2 = getY(trends[i].completed);
                compLineD += ' C ' + cx1 + ' ' + cy1 + ', ' + cx2 + ' ' + cy2 + ', ' + getX(i) + ' ' + getY(trends[i].completed);
            }
            svgHtml += '<path class="trend-line-completed" d="' + compLineD + '"/>';

            // Line path for Created (dashed blue)
            var creatLineD = 'M ' + getX(0) + ' ' + getY(trends[0].created);
            for (var i = 1; i < trends.length; i++) {
                var cx1 = getX(i - 1) + stepX * 0.45;
                var cy1 = getY(trends[i - 1].created);
                var cx2 = getX(i) - stepX * 0.45;
                var cy2 = getY(trends[i].created);
                creatLineD += ' C ' + cx1 + ' ' + cy1 + ', ' + cx2 + ' ' + cy2 + ', ' + getX(i) + ' ' + getY(trends[i].created);
            }
            svgHtml += '<path class="trend-line-created" d="' + creatLineD + '"/>';

            // Data Points and X labels
            for (var i = 0; i < trends.length; i++) {
                var t = trends[i];
                var px = getX(i);
                var pyComp = getY(t.completed);
                var pyCreat = getY(t.created);

                // Completed point (green with white center)
                svgHtml += '<circle class="data-point" cx="' + px + '" cy="' + pyComp + '" r="4.5" fill="#10B981" stroke="#0B0F19" stroke-width="2" onclick="showTooltip(' + i + ', event)"/>';

                // Created point (blue)
                svgHtml += '<circle class="data-point" cx="' + px + '" cy="' + pyCreat + '" r="3.5" fill="#3B82F6" stroke="#0B0F19" stroke-width="1.5" onclick="showTooltip(' + i + ', event)"/>';

                // Day Label
                svgHtml += '<text class="axis-text" x="' + px + '" y="' + (height - 6) + '" text-anchor="middle">' + t.day + '</text>';
            }

            svg.innerHTML = svgHtml;
        }

        // Tooltip handler
        function showTooltip(index, evt) {
            var t = currentData.weeklyTrends[index];
            if (!t) return;
            var tt = document.getElementById('d3-tooltip');
            tt.innerHTML = '<div class="tt-title">' + t.day + ' (' + t.date + ')</div>' +
                '<div class="tt-row"><span style="color:#10B981;">● Completed:</span><strong>' + t.completed + ' tasks</strong></div>' +
                '<div class="tt-row"><span style="color:#3B82F6;">● Created:</span><strong>' + t.created + ' tasks</strong></div>' +
                '<div class="tt-row"><span style="color:#F59E0B;">● On-Time SLA:</span><strong>' + t.slaPct + '%</strong></div>';
            tt.style.display = 'block';

            var x = evt.pageX || (evt.touches && evt.touches[0].pageX) || 120;
            var y = evt.pageY || (evt.touches && evt.touches[0].pageY) || 80;
            tt.style.left = Math.min(x - 50, window.innerWidth - 170) + 'px';
            tt.style.top = (y - 75) + 'px';

            setTimeout(function() {
                tt.style.display = 'none';
            }, 3500);
        }

        // Render SLA Donut Gauge
        function renderSlaDonut() {
            var svg = document.getElementById('sla-donut-svg');
            var size = 110;
            var strokeW = 10;
            var radius = (size - strokeW) / 2;
            var cx = size / 2;
            var cy = size / 2;
            var circ = 2 * Math.PI * radius;

            var slaPct = 94.8;
            var delayPct = 3.8;
            var overduePct = 1.4;

            // Recalculate if we have trends
            if (currentData.weeklyTrends && currentData.weeklyTrends.length) {
                var totalSla = 0;
                currentData.weeklyTrends.forEach(function(d) { totalSla += (d.slaPct || 92); });
                slaPct = Math.round((totalSla / currentData.weeklyTrends.length) * 10) / 10;
                delayPct = Math.round(((100 - slaPct) * 0.7) * 10) / 10;
                overduePct = Math.round((100 - slaPct - delayPct) * 10) / 10;
            }

            document.getElementById('sla-pct-display').innerText = slaPct + '%';
            document.getElementById('stat-within-sla').innerText = slaPct + '%';
            document.getElementById('stat-delay').innerText = delayPct + '%';
            document.getElementById('stat-overdue').innerText = overduePct + '%';

            var offset1 = circ * (1 - slaPct / 100);

            var html = '';
            // Background track
            html += '<circle cx="' + cx + '" cy="' + cy + '" r="' + radius + '" fill="none" stroke="#1E293B" stroke-width="' + strokeW + '"/>';
            // Progress arc
            html += '<circle cx="' + cx + '" cy="' + cy + '" r="' + radius + '" fill="none" stroke="#10B981" stroke-width="' + strokeW + '" ' +
                'stroke-dasharray="' + circ + '" stroke-dashoffset="' + offset1 + '" stroke-linecap="round" ' +
                'transform="rotate(-90 ' + cx + ' ' + cy + ')"/>';

            svg.innerHTML = html;
        }

        // Render Technician Efficiency List
        function renderTechEfficiencyList() {
            var listEl = document.getElementById('tech-efficiency-list');
            var techs = currentData.techMetrics || [];
            var metricType = currentData.metricType || 'score';
            var metricLabel = currentData.metricLabel || 'Efficiency Score';
            var metricUnit = currentData.metricUnit || '%';

            document.getElementById('tech-ranking-subtitle').innerText = 'Ranked by ' + metricLabel;

            // Sort techs descending by current metric
            techs.sort(function(a, b) {
                var valA = getTechMetricValue(a, metricType);
                var valB = getTechMetricValue(b, metricType);
                return metricType === 'mttr' ? (valA - valB) : (valB - valA);
            });

            var maxVal = 100;
            if (metricType === 'tasks') {
                maxVal = d3.max(techs, function(t) { return t.closedTasks; }) || 40;
            } else if (metricType === 'mttr') {
                maxVal = 4.0;
            }

            var html = '';
            techs.forEach(function(tech) {
                var val = getTechMetricValue(tech, metricType);
                var pct = Math.min(100, Math.max(5, (val / maxVal) * 100));
                if (metricType === 'mttr') {
                    // For MTTR, lower is better: invert percentage for visual bar
                    pct = Math.min(100, Math.max(10, ((4.0 - val) / 4.0) * 100));
                }

                var barColor = '#10B981';
                if (metricType === 'score' || metricType === 'sla') {
                    barColor = val >= 90 ? '#10B981' : (val >= 80 ? '#F59E0B' : '#EF4444');
                } else if (metricType === 'mttr') {
                    barColor = val <= 1.8 ? '#10B981' : (val <= 2.5 ? '#F59E0B' : '#EF4444');
                } else {
                    barColor = '#3B82F6';
                }

                var displayValStr = val + (metricUnit ? ' ' + metricUnit : '');
                var isSelected = tech.id === selectedTechId;
                var initials = tech.name.split(' ').map(function(s) { return s[0]; }).join('').toUpperCase();

                html += '<div class="tech-row ' + (isSelected ? 'selected' : '') + '" onclick="selectTechnician(\'' + tech.id + '\')">';
                html += '  <div class="tech-info-line">';
                html += '    <div class="tech-avatar-group">';
                html += '      <div class="tech-badge-avatar">' + initials + '</div>';
                html += '      <div>';
                html += '        <div class="tech-name">' + tech.name + ' <span class="status-chip status-' + tech.status + '">' + tech.status + '</span></div>';
                html += '        <div class="tech-role">' + tech.role + ' • ' + tech.badge + '</div>';
                html += '      </div>';
                html += '    </div>';
                html += '    <div class="tech-metric-val" style="color:' + barColor + ';">' + displayValStr + '</div>';
                html += '  </div>';
                html += '  <div class="tech-bar-track">';
                html += '    <div class="tech-bar-fill" style="width:' + pct + '%; background:' + barColor + ';"></div>';
                html += '  </div>';
                html += '  <div class="tech-meta-row">';
                html += '    <span>SLA: <strong>' + tech.sla + '%</strong></span>';
                html += '    <span>Closed: <strong>' + tech.closedTasks + '</strong></span>';
                html += '    <span>MTTR: <strong>' + tech.mttr + 'h</strong></span>';
                html += '    <span>First Fix: <strong>' + tech.firstFix + '%</strong></span>';
                html += '  </div>';
                html += '</div>';
            });

            listEl.innerHTML = html;
        }

        function getTechMetricValue(tech, metric) {
            switch(metric) {
                case 'tasks': return tech.closedTasks;
                case 'sla': return tech.sla;
                case 'mttr': return tech.mttr;
                case 'score':
                default:
                    return tech.score;
            }
        }

        function selectTechnician(techId) {
            selectedTechId = techId;
            renderTechEfficiencyList();
            if (window.AndroidAnalyticsBridge && window.AndroidAnalyticsBridge.onTechnicianClick) {
                window.AndroidAnalyticsBridge.onTechnicianClick(techId);
            }
        }

        // Bridge update callbacks
        window.updateD3Data = function(payload) {
            if (!payload) return;
            currentData = payload;
            renderTrendsChart();
            renderSlaDonut();
            renderTechEfficiencyList();
            logToAndroid('D3 data refreshed successfully');
        };

        window.setD3Metric = function(metricKey) {
            currentData.metricType = metricKey;
            switch(metricKey) {
                case 'tasks':
                    currentData.metricLabel = 'Tasks Completed';
                    currentData.metricUnit = 'tasks';
                    break;
                case 'sla':
                    currentData.metricLabel = 'SLA On-Time Rate';
                    currentData.metricUnit = '%';
                    break;
                case 'mttr':
                    currentData.metricLabel = 'Mean Time to Resolve';
                    currentData.metricUnit = 'hrs';
                    break;
                default:
                    currentData.metricLabel = 'Efficiency Score';
                    currentData.metricUnit = '%';
            }
            renderTechEfficiencyList();
        };

        // Initialize on DOM load
        document.addEventListener('DOMContentLoaded', function() {
            // Check if Android bridge provides initial data
            if (window.AndroidAnalyticsBridge && window.AndroidAnalyticsBridge.getAnalyticsJson) {
                try {
                    var jsonStr = window.AndroidAnalyticsBridge.getAnalyticsJson();
                    if (jsonStr && jsonStr !== '{}') {
                        var parsed = JSON.parse(jsonStr);
                        if (parsed) currentData = parsed;
                    }
                } catch(e) {
                    console.error('Error reading initial analytics JSON:', e);
                }
            }

            renderTrendsChart();
            renderSlaDonut();
            renderTechEfficiencyList();
        });
    </script>
</body>
</html>
        """.trimIndent()
    }
}
