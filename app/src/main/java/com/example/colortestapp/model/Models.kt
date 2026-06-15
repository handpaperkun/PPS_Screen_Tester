package com.example.colortestapp.model

data class RectangleData(
    val color: IntArray,
    val colex: IntArray,
    val bits: Int,
    val position: FloatArray,
    val size: FloatArray
)

data class SignalData(
    val background: RectangleData,
    val window: RectangleData
)

data class MotionBlurConfig(
    val backgroundGray: Int = 10,
    val fontColorMode: FontColorMode = FontColorMode.WHITE,
    val scrollSpeed: Int = 960
)

enum class FontColorMode {
    WHITE, RED, GREEN, BLUE
}

data class AplConfig(
    val aplValue: Int = 50
)

data class UniformityConfig(
    val grayLevel: Int = 255
)

data class SignalConfig(
    val ipAddress: String = "",
    val port: Int = 20002,
    val hdrEnabled: Boolean = false
)

data class UltraHdrConfig(
    val stepIndex: Int = 14  // 默认 SDR 白 (100%)
)

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR
}