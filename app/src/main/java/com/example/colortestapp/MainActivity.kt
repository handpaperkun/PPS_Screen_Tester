package com.example.colortestapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.colortestapp.navigation.Screen
import com.example.colortestapp.ui.apl.AplScreen
import com.example.colortestapp.ui.home.HomeScreen
import com.example.colortestapp.ui.motionblur.MotionBlurScreen
import com.example.colortestapp.ui.signal.SignalScreen
import com.example.colortestapp.ui.subpixel.SubpixelScreen
import com.example.colortestapp.ui.hdrscan.HdrScanScreen
import com.example.colortestapp.ui.rgbtest.RgbTestScreen
import com.example.colortestapp.ui.motion.MotionScreen
import com.example.colortestapp.ui.theme.ColorTESTappTheme
import com.example.colortestapp.ui.ultrahdr.UltraHdrScreen
import com.example.colortestapp.ui.uniformity.UniformityScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.isAppearanceLightStatusBars = false

        setContent {
            ColorTESTappTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route
                    ) {
                        composable(Screen.Home.route) {
                            HomeScreen(
                                onNavigate = { screen ->
                                    navController.navigate(screen.route)
                                }
                            )
                        }

                        composable(Screen.SignalReceiver.route) {
                            SignalScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(Screen.MotionBlur.route) {
                            MotionBlurScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(Screen.AplTest.route) {
                            AplScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(Screen.Uniformity.route) {
                            UniformityScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable(Screen.UltraHdrTest.route) {
                            UltraHdrScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable(Screen.Subpixel.route) {
                            SubpixelScreen(onNavigateBack = { navController.popBackStack() })
                        }
                        composable(Screen.HdrScan.route) {
                            HdrScanScreen(onNavigateBack = { navController.popBackStack() })
                        }
                        composable(Screen.RgbTest.route) {
                            RgbTestScreen(onBack = { navController.popBackStack() })
                        }
                        composable(Screen.Motion.route) {
                            MotionScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}