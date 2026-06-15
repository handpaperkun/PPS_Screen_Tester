package com.example.colortestapp.ui.uniformity

import androidx.lifecycle.ViewModel
import com.example.colortestapp.model.UniformityConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UniformityViewModel : ViewModel() {
    private val _config = MutableStateFlow(UniformityConfig())
    val config: StateFlow<UniformityConfig> = _config

    fun updateConfig(newConfig: UniformityConfig) {
        _config.value = newConfig
    }
}