package com.example.colortestapp.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Color Test")
    object SignalReceiver : Screen("signal", "Signal Receiver")
    object MotionBlur : Screen("motion_blur", "Motion Blur Test")
    object AplTest : Screen("apl_test", "APL Test")
    object Uniformity : Screen("uniformity", "Uniformity Test")
    object UltraHdrTest : Screen("ultra_hdr", "Ultra HDR Test")
    object Subpixel : Screen("subpixel", "Subpixel Test")
    object HdrScan : Screen("hdr_scan", "HDR Scan Test")
    object RgbTest : Screen("rgb_test", "RGB Test")
    object Motion : Screen("motion", "Motion Test")
}