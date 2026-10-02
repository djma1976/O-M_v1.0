package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.DiagnosticResult
import com.example.data.ai.GeminiAiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AiDiagnosticsViewModel(
    private val aiService: GeminiAiService = GeminiAiService()
) : ViewModel() {

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisResult = MutableStateFlow<DiagnosticResult?>(null)
    val analysisResult: StateFlow<DiagnosticResult?> = _analysisResult.asStateFlow()

    private val _selectedEquipment = MutableStateFlow("High-Voltage Substation Transformer (345kV)")
    val selectedEquipment: StateFlow<String> = _selectedEquipment.asStateFlow()

    private val _symptomInput = MutableStateFlow("Feeder 4B Buchholz relay alarm with high dissolved combustible gas (H2: 450ppm, C2H2: 35ppm). Tank temperature 78°C.")
    val symptomInput: StateFlow<String> = _symptomInput.asStateFlow()

    private val _siteConditions = MutableStateFlow("Substation Yard Alpha, Ambient Temp 34°C, 85% Humidity, Post-Lightning Storm")
    val siteConditions: StateFlow<String> = _siteConditions.asStateFlow()

    private val _hazardAlert = MutableStateFlow("Arc Flash Boundary 12ft (40 cal/cm²), Pressurized Insulating Oil (35,000 Gallons)")
    val hazardAlert: StateFlow<String> = _hazardAlert.asStateFlow()

    fun updateEquipment(value: String) { _selectedEquipment.value = value }
    fun updateSymptoms(value: String) { _symptomInput.value = value }
    fun updateConditions(value: String) { _siteConditions.value = value }
    fun updateHazards(value: String) { _hazardAlert.value = value }

    fun loadPreset(equipment: String, symptoms: String, conditions: String, hazards: String) {
        _selectedEquipment.value = equipment
        _symptomInput.value = symptoms
        _siteConditions.value = conditions
        _hazardAlert.value = hazards
    }

    fun runDiagnostics() {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _analysisResult.value = null
            val result = aiService.analyzeComplexFieldIssue(
                equipmentType = _selectedEquipment.value,
                symptomsOrErrorCode = _symptomInput.value,
                siteConditions = _siteConditions.value,
                safetyHazards = _hazardAlert.value
            )
            _analysisResult.value = result
            _isAnalyzing.value = false
        }
    }
}
