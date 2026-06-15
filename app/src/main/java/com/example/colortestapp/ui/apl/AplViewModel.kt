package com.example.colortestapp.ui.apl

import androidx.lifecycle.ViewModel
import com.example.colortestapp.model.AplConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AplViewModel : ViewModel() {
    private val _config = MutableStateFlow(AplConfig())
    val config: StateFlow<AplConfig> = _config

    fun updateConfig(newConfig: AplConfig) {
        _config.value = newConfig
    }
}