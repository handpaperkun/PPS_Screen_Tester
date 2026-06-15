package com.example.colortestapp.network

import android.util.Log
import com.example.colortestapp.model.SignalData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SignalTcpClient {
    private var socket: Socket? = null
    private var receiveJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow(com.example.colortestapp.model.ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<com.example.colortestapp.model.ConnectionState> = _connectionState

    private val _signalData = MutableStateFlow<SignalData?>(null)
    val signalData: StateFlow<SignalData?> = _signalData

    suspend fun connect(ip: String, port: Int) {
        withContext(Dispatchers.IO) {
            try {
                _connectionState.value = com.example.colortestapp.model.ConnectionState.CONNECTING
                socket = Socket(ip, port)
                socket?.soTimeout = 0 // 无超时，阻塞读取
                _connectionState.value = com.example.colortestapp.model.ConnectionState.CONNECTED
                Log.d("SignalTcpClient", "Connected to $ip:$port")
                startReceiving()
            } catch (e: Exception) {
                _connectionState.value = com.example.colortestapp.model.ConnectionState.ERROR
                Log.e("SignalTcpClient", "Connection failed", e)
            }
        }
    }

    private fun startReceiving() {
        receiveJob = scope.launch {
            val stream = socket?.getInputStream()
            if (stream == null) {
                _connectionState.value = com.example.colortestapp.model.ConnectionState.ERROR
                return@launch
            }

            while (isActive && socket?.isConnected == true && socket?.isClosed == false) {
                try {
                    // 读取4字节大端长度头
                    val lengthBytes = ByteArray(4)
                    var totalRead = 0
                    while (totalRead < 4) {
                        val read = stream.read(lengthBytes, totalRead, 4 - totalRead)
                        if (read == -1) throw Exception("连接已关闭")
                        totalRead += read
                    }
                    val xmlLength = ByteBuffer.wrap(lengthBytes).order(ByteOrder.BIG_ENDIAN).int

                    if (xmlLength <= 0 || xmlLength > 10 * 1024 * 1024) {
                        Log.e("SignalTcpClient", "Invalid XML length: $xmlLength")
                        continue
                    }

                    // 读取XML内容
                    val xmlBytes = ByteArray(xmlLength)
                    totalRead = 0
                    while (totalRead < xmlLength) {
                        val read = stream.read(xmlBytes, totalRead, xmlLength - totalRead)
                        if (read == -1) throw Exception("连接已关闭")
                        totalRead += read
                    }

                    val xmlString = xmlBytes.toString(Charsets.UTF_8)
                    Log.d("SignalTcpClient", "Received XML: $xmlString")

                    val parsedData = XmlSignalParser.parse(xmlString)
                    parsedData?.let {
                        _signalData.value = it
                    }
                } catch (e: Exception) {
                    Log.e("SignalTcpClient", "Receive error", e)
                    _connectionState.value = com.example.colortestapp.model.ConnectionState.ERROR
                    disconnect()
                    return@launch
                }
            }
        }
    }

    fun disconnect() {
        receiveJob?.cancel()
        receiveJob = null
        scope.cancel()
        try {
            socket?.close()
        } catch (e: Exception) {
            Log.e("SignalTcpClient", "Disconnect error", e)
        }
        socket = null
        if (_connectionState.value != com.example.colortestapp.model.ConnectionState.ERROR) {
            _connectionState.value = com.example.colortestapp.model.ConnectionState.DISCONNECTED
        }
    }
}
