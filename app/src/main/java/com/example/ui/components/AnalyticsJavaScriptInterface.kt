package com.example.ui.components

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import java.lang.ref.WeakReference

/**
 * JavaScript interface bridge between Android WebView running D3.js data visualizations
 * and the Jetpack Compose field operations dashboard.
 */
class AnalyticsJavaScriptInterface(
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
    private val onTechnicianSelected: (String) -> Unit = {},
    private val onTimeframeSelected: (String) -> Unit = {},
    private val onMetricSelected: (String) -> Unit = {}
) {
    private var webViewRef: WeakReference<WebView>? = null

    @Volatile
    private var cachedDataJson: String = "{}"

    fun attachWebView(webView: WebView) {
        webViewRef = WeakReference(webView)
    }

    fun detachWebView() {
        webViewRef?.clear()
        webViewRef = null
    }

    fun updateCachedJson(json: String) {
        cachedDataJson = json
    }

    /**
     * Called by D3.js in WebView on DOM ready to retrieve initial dataset.
     */
    @JavascriptInterface
    fun getAnalyticsJson(): String {
        return cachedDataJson
    }

    /**
     * Invoked when a technician's efficiency bar is tapped in the D3 chart.
     */
    @JavascriptInterface
    fun onTechnicianClick(techId: String) {
        mainHandler.post {
            onTechnicianSelected(techId)
        }
    }

    /**
     * Invoked when timeframe is toggled inside the chart interface.
     */
    @JavascriptInterface
    fun onTimeframeClick(timeframeKey: String) {
        mainHandler.post {
            onTimeframeSelected(timeframeKey)
        }
    }

    /**
     * Invoked when metric dimension is toggled inside the chart interface.
     */
    @JavascriptInterface
    fun onMetricClick(metricKey: String) {
        mainHandler.post {
            onMetricSelected(metricKey)
        }
    }

    @JavascriptInterface
    fun onLog(message: String) {
        android.util.Log.d("D3AnalyticsBridge", message)
    }

    /**
     * Dispatches an updated JSON payload to the running D3.js chart on the main UI thread.
     */
    fun pushDataToD3(jsonPayload: String) {
        cachedDataJson = jsonPayload
        val escaped = jsonPayload
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "")
        evaluateJs("window.updateD3Data && window.updateD3Data(JSON.parse('$escaped'));")
    }

    fun setMetricInD3(metricKey: String) {
        evaluateJs("window.setD3Metric && window.setD3Metric('$metricKey');")
    }

    private fun evaluateJs(script: String) {
        mainHandler.post {
            webViewRef?.get()?.evaluateJavascript(script, null)
        }
    }
}
