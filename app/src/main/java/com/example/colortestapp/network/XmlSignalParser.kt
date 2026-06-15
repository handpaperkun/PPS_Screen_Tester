package com.example.colortestapp.network

import android.util.Log
import com.example.colortestapp.model.RectangleData
import com.example.colortestapp.model.SignalData
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

object XmlSignalParser {

    /**
     * 解析 CalMAN / dogegen 兼容的 Resolve XML 协议。
     * 支持两种格式：
     *   Variant A (CalMAN):  <color>, <background>, <geometry> 同级标签
     *   Variant B (ColourSpace/LightSpace): <rectangle> × 1..2
     */
    fun parse(xmlString: String): SignalData? {
        return try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(xmlString.reader())

            val variant = if (xmlString.contains("<rectangle")) "B" else "A"
            Log.d("XmlSignalParser", "Detected variant: $variant")

            when (variant) {
                "B" -> parseVariantB(parser)
                else -> parseVariantA(parser)
            }
        } catch (e: Exception) {
            Log.e("XmlSignalParser", "Parse error", e)
            null
        }
    }

    // ── Variant A: CalMAN 格式 ──────────────────────────────────────
    // <calibration>
    //   <color red="512" green="512" blue="512" bits="10"/>
    //   <background red="0" green="0" blue="0" bits="10"/>
    //   <geometry x="0.146" y="0.146" cx="0.707" cy="0.707"/>
    // </calibration>

    private fun parseVariantA(parser: XmlPullParser): SignalData? {
        var windowColor = intArrayOf(255, 255, 255)
        var windowColex = intArrayOf(255, 255, 255)
        var windowBits = 8

        var bgColor = intArrayOf(0, 0, 0)
        var bgColex = intArrayOf(0, 0, 0)
        var bgBits = 8

        var posX = 0f; var posY = 0f; var cx = 1f; var cy = 1f

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "color" -> {
                        val raw = intArrayOf(
                            parser.getAttributeValue(null, "red")?.toInt() ?: 0,
                            parser.getAttributeValue(null, "green")?.toInt() ?: 0,
                            parser.getAttributeValue(null, "blue")?.toInt() ?: 0
                        )
                        windowBits = parser.getAttributeValue(null, "bits")?.toInt() ?: 8
                        windowColex = raw
                        windowColor = if (windowBits != 8) {
                            intArrayOf(
                                (raw[0] * 255f / ((1 shl windowBits) - 1)).toInt(),
                                (raw[1] * 255f / ((1 shl windowBits) - 1)).toInt(),
                                (raw[2] * 255f / ((1 shl windowBits) - 1)).toInt()
                            )
                        } else raw
                    }
                    "background" -> {
                        val raw = intArrayOf(
                            parser.getAttributeValue(null, "red")?.toInt() ?: 0,
                            parser.getAttributeValue(null, "green")?.toInt() ?: 0,
                            parser.getAttributeValue(null, "blue")?.toInt() ?: 0
                        )
                        bgBits = parser.getAttributeValue(null, "bits")?.toInt() ?: 8
                        bgColex = raw
                        bgColor = if (bgBits != 8) {
                            intArrayOf(
                                (raw[0] * 255f / ((1 shl bgBits) - 1)).toInt(),
                                (raw[1] * 255f / ((1 shl bgBits) - 1)).toInt(),
                                (raw[2] * 255f / ((1 shl bgBits) - 1)).toInt()
                            )
                        } else raw
                    }
                    "geometry" -> {
                        posX = parser.getAttributeValue(null, "x")?.toFloat() ?: 0f
                        posY = parser.getAttributeValue(null, "y")?.toFloat() ?: 0f
                        cx   = parser.getAttributeValue(null, "cx")?.toFloat() ?: 1f
                        cy   = parser.getAttributeValue(null, "cy")?.toFloat() ?: 1f
                    }
                }
            }
            eventType = parser.next()
        }

        val backgroundRect = RectangleData(bgColor, bgColex, bgBits, floatArrayOf(0f, 0f), floatArrayOf(1f, 1f))
        val windowRect = RectangleData(windowColor, windowColex, windowBits, floatArrayOf(posX, posY), floatArrayOf(cx, cy))
        return SignalData(backgroundRect, windowRect)
    }

    // ── Variant B: ColourSpace / LightSpace 格式 ─────────────────
    // <rectangle> × 1 → 单色块，背景默认黑（兼容 ColourSpace 单矩形模式）
    // <rectangle> × 2 → 第一个=背景，第二个=窗口

    private fun parseVariantB(parser: XmlPullParser): SignalData? {
        var background: RectangleData? = null
        var window: RectangleData? = null
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "rectangle" -> {
                        val rect = parseRectangle(parser)
                        if (background == null) background = rect
                        else window = rect
                    }
                }
            }
            eventType = parser.next()
        }

        // 单矩形：作窗口，背景默认黑
        if (window == null && background != null) {
            val blackBg = RectangleData(
                intArrayOf(0, 0, 0), intArrayOf(0, 0, 0), 8,
                floatArrayOf(0f, 0f), floatArrayOf(1f, 1f))
            return SignalData(blackBg, background)
        }
        return if (background != null && window != null) SignalData(background, window) else null
    }

    private fun parseRectangle(parser: XmlPullParser): RectangleData {
        var color = intArrayOf(0, 0, 0)
        var colex = intArrayOf(0, 0, 0)
        var bits = 8
        var position = floatArrayOf(0f, 0f)
        var size = floatArrayOf(1f, 1f)
        var hasColex = false

        var eventType = parser.next()
        while (eventType != XmlPullParser.END_TAG || parser.name != "rectangle") {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "color" -> {
                        color = intArrayOf(
                            parser.getAttributeValue(null, "red")?.toInt() ?: 0,
                            parser.getAttributeValue(null, "green")?.toInt() ?: 0,
                            parser.getAttributeValue(null, "blue")?.toInt() ?: 0
                        )
                    }
                    "colex" -> {
                        colex = intArrayOf(
                            parser.getAttributeValue(null, "red")?.toInt() ?: 0,
                            parser.getAttributeValue(null, "green")?.toInt() ?: 0,
                            parser.getAttributeValue(null, "blue")?.toInt() ?: 0
                        )
                        val b = parser.getAttributeValue(null, "bits")?.toIntOrNull() ?: 8
                        bits = if (b <= 0) 8 else b  // bits="0" → 默认 8
                        hasColex = true
                    }
                    "geometry" -> {
                        position = floatArrayOf(
                            parser.getAttributeValue(null, "x")?.toFloat() ?: 0f,
                            parser.getAttributeValue(null, "y")?.toFloat() ?: 0f
                        )
                        size = floatArrayOf(
                            parser.getAttributeValue(null, "cx")?.toFloat() ?: 1f,
                            parser.getAttributeValue(null, "cy")?.toFloat() ?: 1f
                        )
                    }
                }
            }
            eventType = parser.next()
        }

        // 无 colex → 把 color 值按位深映射到 colex
        if (!hasColex) {
            colex = color
        }
        return RectangleData(color, colex, bits, position, size)
    }
}
