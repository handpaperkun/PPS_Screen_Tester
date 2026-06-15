package com.example.colortestapp.ui.signal

import android.content.Context
import android.view.SurfaceView

class Hdr10SurfaceView(context: Context) : SurfaceView(context) {
    val pipeline = Hdr10VideoPipeline()
}
