package com.example.colortestapp.ui.signal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.colortestapp.model.ConnectionState
import com.example.colortestapp.model.SignalConfig
import com.example.colortestapp.network.SignalTcpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SignalViewModel : ViewModel() {
    private val tcpClient = SignalTcpClient()

    private val _config = MutableStateFlow(SignalConfig())
    val config: StateFlow<SignalConfig> = _config

    val connectionState: StateFlow<ConnectionState> = tcpClient.connectionState
    val signalData = tcpClient.signalData

    fun updateConfig(newConfig: SignalConfig) {
        _config.value = newConfig
    }

    fun connect() {
        viewModelScope.launch {
            val cfg = _config.value
            if (cfg.ipAddress.isNotBlank()) {
                tcpClient.connect(cfg.ipAddress, cfg.port)
            }
        }
    }

    fun disconnect() {
        tcpClient.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        tcpClient.disconnect()
    }
}
