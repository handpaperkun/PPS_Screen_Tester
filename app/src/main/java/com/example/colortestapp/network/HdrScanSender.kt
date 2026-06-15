package com.example.colortestapp.network

import android.content.Context
import android.util.Log
import com.example.colortestapp.ui.hdrscan.HdrScanViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.net.Socket

class HdrScanSender(private val context: Context) {
    private var socket: Socket? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun start(points: List<HdrScanViewModel.ScanPoint>) {
        scope.launch {
            try {
                socket = Socket("127.0.0.1", 20003)
                val writer = OutputStreamWriter(socket!!.getOutputStream())
                // JSON 格式发送
                val json = buildString {
                    append("{\"type\":\"hdr_scan\",\"points\":[")
                    points.forEachIndexed { i, pt ->
                        if (i > 0) append(",")
                        append("{\"index\":${pt.index},\"brightness\":${pt.brightness},\"hdrRatio\":${pt.hdrRatio}}")
                    }
                    append("]}")
                }
                writer.write(json)
                writer.flush()
                writer.close()
                socket?.close()
                Log.d("HdrScanSender", "Data sent: ${points.size} points")
            } catch (e: Exception) {
                Log.e("HdrScanSender", "Send failed", e)
            }
        }
    }

    fun stop() {
        scope.cancel()
        try { socket?.close() } catch (_: Exception) {}
    }
}
