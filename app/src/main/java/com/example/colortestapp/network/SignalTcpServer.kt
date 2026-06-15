package com.example.colortestapp.network

import android.util.Log
import com.example.colortestapp.model.ConnectionState
import com.example.colortestapp.model.SignalData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext

class SignalTcpServer(
    private val port: Int = 20002
) {
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _signalData = MutableStateFlow<SignalData?>(null)
    val signalData: StateFlow<SignalData?> = _signalData

    fun start() {
        scope.launch {
            val job = coroutineContext[Job]!!
            try {
                _connectionState.value = ConnectionState.CONNECTING
                serverSocket = ServerSocket(port)
                Log.d("SignalTcpServer", "Listening on port $port")

                while (job.isActive) {
                    try {
                        val client = serverSocket!!.accept()
                        Log.d("SignalTcpServer", "Client connected: ${client.inetAddress}")

                        clientSocket?.close()
                        clientSocket = client

                        _connectionState.value = ConnectionState.CONNECTED
                        handleClient(client)
                        _connectionState.value = ConnectionState.DISCONNECTED
                    } catch (e: Exception) {
                        if (!job.isActive) break
                        Log.e("SignalTcpServer", "Accept error", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("SignalTcpServer", "Server error", e)
                _connectionState.value = ConnectionState.ERROR
            }
        }
    }

    private suspend fun handleClient(socket: Socket) {
        val job = currentCoroutineContext()[Job]!!
        try {
            val stream = socket.getInputStream()

            while (job.isActive && socket.isConnected && !socket.isClosed) {
                try {
                    val lengthBytes = ByteArray(4)
                    var totalRead = 0
                    while (totalRead < 4) {
                        val read = stream.read(lengthBytes, totalRead, 4 - totalRead)
                        if (read == -1) throw Exception("连接已关闭")
                        totalRead += read
                    }
                    val xmlLength = ByteBuffer.wrap(lengthBytes).order(ByteOrder.BIG_ENDIAN).int

                    if (xmlLength <= 0 || xmlLength > 10 * 1024 * 1024) {
                        Log.e("SignalTcpServer", "Invalid XML length: $xmlLength")
                        continue
                    }

                    val xmlBytes = ByteArray(xmlLength)
                    totalRead = 0
                    while (totalRead < xmlLength) {
                        val read = stream.read(xmlBytes, totalRead, xmlLength - totalRead)
                        if (read == -1) throw Exception("连接已关闭")
                        totalRead += read
                    }

                    val xmlString = xmlBytes.toString(Charsets.UTF_8)
                    Log.d("SignalTcpServer", "Received: ${xmlString.take(200)}...")

                    val parsed = XmlSignalParser.parse(xmlString)
                    if (parsed != null) {
                        _signalData.value = parsed
                    }
                } catch (e: Exception) {
                    Log.e("SignalTcpServer", "Client read error", e)
                    break
                }
            }
        } catch (e: Exception) {
            Log.e("SignalTcpServer", "Handle client error", e)
            _connectionState.value = ConnectionState.ERROR
        } finally {
            try { socket.close() } catch (_: Exception) {}
            clientSocket = null
        }
    }

    fun stop() {
        scope.cancel()
        try { clientSocket?.close() } catch (_: Exception) {}
        try { serverSocket?.close() } catch (_: Exception) {}
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun getPort(): Int = port
}
