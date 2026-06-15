package com.example.colortestapp.ui.motionblur

import androidx.lifecycle.ViewModel
import com.example.colortestapp.model.MotionBlurConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MotionBlurViewModel : ViewModel() {
    private val _config = MutableStateFlow(MotionBlurConfig())
    val config: StateFlow<MotionBlurConfig> = _config

    fun updateConfig(newConfig: MotionBlurConfig) {
        _config.value = newConfig
    }
}