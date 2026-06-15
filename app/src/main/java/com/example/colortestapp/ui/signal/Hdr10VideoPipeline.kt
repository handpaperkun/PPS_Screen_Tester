package com.example.colortestapp.ui.signal

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import androidx.annotation.RequiresApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * 真 HDR10 视频管线：把 8-bit 背景+窗口图案"拉伸"成 10-bit BT.2020 PQ 视频帧，
 * 经 HEVC Main10 连续编码 → 解码 → 渲染到 SurfaceView。
 *
 * 关键设计：
 * - P010 字节缓冲输入（COLOR_FormatYUVP010），对纯色/窗口图案完全确定。
 * - 8-bit 码值直接映射为 10-bit PQ 信号（视频信号域，不再二次 OETF）。
 * - KEY_HDR_STATIC_INFO 25 字节（含开头 0x01）。
 * - 连续喂帧（像真视频持续编解码），HDR 稳定保持，避免单帧不吐的厂商兼容坑。
 * - **按 SurfaceView 实际分辨率编码**（近 1:1，无放大糊边）。
 * - **色度分区**：背景用背景自身色度（黑=中性512），窗口用窗口色度——避免背景染色。
 * - **缓存帧**：只在背景/窗口参数变化时重建 P010，逐帧仅做一次整块拷贝（省 CPU）。
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class Hdr10VideoPipeline {

    companion object {
        private const val TAG = "Hdr10Pipeline"
        private const val MIME = MediaFormat.MIMETYPE_VIDEO_HEVC
        private const val IO_TIMEOUT_US = 2_000L
        private const val FRAME_INTERVAL_MS = 40L

        private const val Y_BLACK = 64.0
        private const val Y_RANGE = 876.0
        private const val C_NEUTRAL = 512.0
        private const val C_RANGE = 896.0
    }

    private var encoder: MediaCodec? = null
    private var decoder: MediaCodec? = null
    private var displaySurface: Surface? = null

    private var codecW = 1280
    private var codecH = 720
    private var stride = 0
    private var sliceHeight = 0

    private var thread: HandlerThread? = null
    private var handler: Handler? = null
    @Volatile private var generation = 0

    @Volatile var isRunning = false; private set
    @Volatile var lastError: String? = null; private set
    @Volatile var statusText: String = "未启动"; private set
    @Volatile var frames: Int = 0; private set

    // 图案参数：打包成不可变快照，单一 @Volatile 引用，避免 codec 线程读到撕裂的混合值。
    private data class Params(
        val winR: Int, val winG: Int, val winB: Int,
        val bgR: Int, val bgG: Int, val bgB: Int,
        val winX: Float, val winY: Float, val winW: Float, val winH: Float
    )
    @Volatile private var params = Params(255, 255, 255, 0, 0, 0, 0f, 0f, 1f, 1f)

    // 帧缓存
    @Volatile private var cacheDirty = true
    private var cachedFrame: ByteArray? = null

    /** 启动管线（surface 就绪后调用，传入像素尺寸）。 */
    fun start(surface: Surface, width: Int, height: Int) {
        stop()
        displaySurface = surface
        codecW = align16(width)
        codecH = align16(height)
        frames = 0
        cachedFrame = null
        cacheDirty = true
        statusText = "启动中…"
        thread = HandlerThread("Hdr10Codec").also { it.start() }
        handler = Handler(thread!!.looper)
        post { initCodecs() }
    }

    /** 更新显示：窗口色 + 背景色 + 窗口几何（分数 0..1）。 */
    fun render(winR: Int, winG: Int, winB: Int, bgR: Int = 0, bgG: Int = 0, bgB: Int = 0,
               posX: Float = 0f, posY: Float = 0f, sizeW: Float = 1f, sizeH: Float = 1f) {
        params = Params(winR, winG, winB, bgR, bgG, bgB, posX, posY, sizeW, sizeH)
        cacheDirty = true
    }

    fun stop() {
        val t = thread; val h = handler
        thread = null; handler = null; isRunning = false
        generation++
        if (t != null && h != null) {
            // 在 handler 线程上原子执行 release + quit，避免 TOCTOU
            h.post {
                releaseCodecs()
                t.quitSafely()
            }
            try { t.join(200) } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
        }
    }

    private fun post(r: () -> Unit) { handler?.post(r) }

    private fun align16(x: Int): Int = ((x / 16) * 16).coerceAtLeast(16)

    // ---------------------------------------------------------------- codec

    private fun initCodecs() {
        val myGen = ++generation
        try {
            val surface = displaySurface ?: return

            decoder = MediaCodec.createDecoderByType(MIME).apply {
                val fmt = MediaFormat.createVideoFormat(MIME, codecW, codecH).apply {
                    setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT2020)
                    setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_ST2084)
                    setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_LIMITED)
                    setByteBuffer(MediaFormat.KEY_HDR_STATIC_INFO, buildHdrStaticInfo())
                }
                configure(fmt, surface, null, 0)
                start()
            }

            val encName = findHdr10Encoder()
            if (encName == null) {
                lastError = "本机无 HDR10(HEVC Main10) 编码器"
                statusText = "✗ 本机无 HDR10 编码器（只能解码）"
                Log.e(TAG, lastError!!); releaseCodecs(); return
            }
            encoder = MediaCodec.createByCodecName(encName).apply {
                configure(buildEncoderFormat(), null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                val inFmt = inputFormat
                stride = inFmt.getInteger(MediaFormat.KEY_STRIDE, codecW)
                sliceHeight = inFmt.getInteger(MediaFormat.KEY_SLICE_HEIGHT, codecH)
                start()
            }

            isRunning = true; lastError = null; cacheDirty = true
            statusText = "编码器就绪…等待出帧"
            Log.i(TAG, "Pipeline started ${codecW}x$codecH stride=$stride sliceH=$sliceHeight enc=$encName")
            post { renderLoop(myGen) }
        } catch (e: Exception) {
            lastError = e.message?.take(120) ?: "init failed"
            statusText = "✗ 初始化失败: $lastError"
            Log.e(TAG, "init failed", e); releaseCodecs()
        }
    }

    private fun releaseCodecs() {
        try { encoder?.stop() } catch (_: Exception) {}
        try { encoder?.release() } catch (_: Exception) {}
        try { decoder?.stop() } catch (_: Exception) {}
        try { decoder?.release() } catch (_: Exception) {}
        encoder = null; decoder = null
        isRunning = false
    }

    private fun buildEncoderFormat(): MediaFormat =
        MediaFormat.createVideoFormat(MIME, codecW, codecH).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, 12_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, 25)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 0)
            setInteger(MediaFormat.KEY_MAX_B_FRAMES, 0)
            setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10)
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUVP010)
            setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT2020)
            setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_ST2084)
            setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_LIMITED)
            setByteBuffer(MediaFormat.KEY_HDR_STATIC_INFO, buildHdrStaticInfo())
        }

    private fun findHdr10Encoder(): String? {
        val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        list.codecInfos.firstOrNull { info ->
            info.isEncoder && info.supportedTypes.any { it.equals(MIME, true) } &&
                runCatching {
                    info.getCapabilitiesForType(MIME)
                        .isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_HdrEditing)
                }.getOrDefault(false)
        }?.let { return it.name }
        list.codecInfos.firstOrNull { info ->
            info.isEncoder && info.supportedTypes.any { it.equals(MIME, true) } &&
                runCatching {
                    info.getCapabilitiesForType(MIME).colorFormats.contains(
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUVP010
                    )
                }.getOrDefault(false)
        }?.let { return it.name }
        return null
    }

    // ---------------------------------------------------------------- 连续渲染

    private fun renderLoop(gen: Int) {
        if (gen != generation || !isRunning) return
        try { pumpOnce() } catch (e: Throwable) {
            Log.e(TAG, "pump error", e)
            statusText = "✗ pump错误: ${e.javaClass.simpleName} ${e.message ?: ""}"
        }
        handler?.postDelayed({ renderLoop(gen) }, FRAME_INTERVAL_MS)
    }

    private fun pumpOnce() {
        val enc = encoder ?: return
        val dec = decoder ?: return

        // 1) 喂一帧（缓存：仅参数变化时重建 P010）
        val inIdx = enc.dequeueInputBuffer(IO_TIMEOUT_US)
        if (inIdx >= 0) {
            val inBuf = enc.getInputBuffer(inIdx)
            if (inBuf != null) {
                val cap = inBuf.capacity()
                val rowsTotal = (sliceHeight + sliceHeight / 2).coerceAtLeast(1)
                val rowBytes = stride.coerceAtLeast(2)  // 用 codec 报告的精确 stride
                val total = rowBytes * rowsTotal
                var frame = cachedFrame
                if (cacheDirty || frame == null || frame.size != total) {
                    frame = buildFrame(rowBytes, total)
                    cachedFrame = frame
                    cacheDirty = false
                }
                inBuf.clear()
                inBuf.put(frame, 0, total)
                enc.queueInputBuffer(inIdx, 0, total, System.nanoTime() / 1000L, 0)
            } else {
                enc.queueInputBuffer(inIdx, 0, 0, System.nanoTime() / 1000L, 0)
            }
        }

        // 2) 编码器输出 → 解码器
        val info = MediaCodec.BufferInfo()
        while (true) {
            val outIdx = enc.dequeueOutputBuffer(info, 0)
            if (outIdx < 0) break
            val outBuf = enc.getOutputBuffer(outIdx)
            if (outBuf != null && info.size > 0) {
                val dInIdx = dec.dequeueInputBuffer(IO_TIMEOUT_US)
                if (dInIdx >= 0) {
                    val dInBuf = dec.getInputBuffer(dInIdx)!!
                    dInBuf.clear()
                    outBuf.position(info.offset); outBuf.limit(info.offset + info.size)
                    dInBuf.put(outBuf)
                    dec.queueInputBuffer(dInIdx, 0, info.size, info.presentationTimeUs, info.flags)
                }
            }
            enc.releaseOutputBuffer(outIdx, false)
        }

        // 3) 解码器输出 → 渲染
        val dInfo = MediaCodec.BufferInfo()
        while (true) {
            val dOut = dec.dequeueOutputBuffer(dInfo, 0)
            if (dOut < 0) break
            dec.releaseOutputBuffer(dOut, true)
            frames++
            if (frames == 1 || frames % 30 == 0) {
                val p = params
                statusText = "✓ 显示中 ${frames}帧 win(${p.winR},${p.winG},${p.winB}) bg(${p.bgR},${p.bgG},${p.bgB})"
            }
        }
    }

    // ---------------------------------------------------------------- P010 构帧

    /** 8-bit RGB → BT.2020 limited-range 10-bit YCbCr（直接码值映射，不做 OETF）。 */
    private fun rgbToYuv10(r: Int, g: Int, b: Int): Triple<Int, Int, Int> {
        val rf = r / 255.0; val gf = g / 255.0; val bf = b / 255.0
        val y = 0.2627 * rf + 0.6780 * gf + 0.0593 * bf
        val pb = (bf - y) / 1.8814
        val pr = (rf - y) / 1.4746
        val y10 = (y * Y_RANGE + Y_BLACK).roundToInt().coerceIn(64, 940)
        val cb10 = (pb * C_RANGE + C_NEUTRAL).roundToInt().coerceIn(64, 960)
        val cr10 = (pr * C_RANGE + C_NEUTRAL).roundToInt().coerceIn(64, 960)
        return Triple(y10, cb10, cr10)
    }

    private fun word10(v: Int) = (v shl 6) and 0xFFFF

    /** 生成 lenBytes 长、用 10-bit 亮度 y 重复填充的 Y 行模板（每 2 字节）。 */
    private fun makeYRow(lenBytes: Int, y10: Int): ByteArray {
        val w = word10(y10)
        val lo = (w and 0xFF).toByte(); val hi = ((w ushr 8) and 0xFF).toByte()
        val a = ByteArray(lenBytes)
        var i = 0; while (i + 1 < lenBytes) { a[i] = lo; a[i + 1] = hi; i += 2 }
        return a
    }

    /** 生成 lenBytes 长、用 (Cb,Cr) 重复填充的 UV 行模板（每 4 字节）。 */
    private fun makeUVRow(lenBytes: Int, cb10: Int, cr10: Int): ByteArray {
        val cw = word10(cb10); val rw = word10(cr10)
        val cbLo = (cw and 0xFF).toByte(); val cbHi = ((cw ushr 8) and 0xFF).toByte()
        val crLo = (rw and 0xFF).toByte(); val crHi = ((rw ushr 8) and 0xFF).toByte()
        val a = ByteArray(lenBytes)
        var i = 0; while (i + 3 < lenBytes) { a[i] = cbLo; a[i + 1] = cbHi; a[i + 2] = crLo; a[i + 3] = crHi; i += 4 }
        return a
    }

    /**
     * 构建一整帧 P010（背景 + 窗口，色度分区）。
     * rowBytes 由 buffer 容量反推，规避 KEY_STRIDE 单位歧义。
     */
    private fun buildFrame(rowBytes: Int, total: Int): ByteArray {
        val p = params   // 一次性快照，避免撕裂读
        val frame = ByteArray(total)
        val yPlaneBytes = rowBytes * sliceHeight

        val (bgY, bgCb, bgCr) = rgbToYuv10(p.bgR, p.bgG, p.bgB)
        val (wY, wCb, wCr) = rgbToYuv10(p.winR, p.winG, p.winB)

        // 背景：整 Y 平面 + 整 UV 平面（含 padding 行）
        val bgYRow = makeYRow(rowBytes, bgY)
        for (r in 0 until sliceHeight) System.arraycopy(bgYRow, 0, frame, r * rowBytes, rowBytes)
        val bgUVRow = makeUVRow(rowBytes, bgCb, bgCr)
        for (r in 0 until sliceHeight / 2) System.arraycopy(bgUVRow, 0, frame, yPlaneBytes + r * rowBytes, rowBytes)

        // 窗口区域（图像空间 codecW×codecH，对齐偶数像素以保 4:2:0 干净）
        val c1 = ((p.winX * codecW).toInt()).coerceIn(0, codecW) and 1.inv()
        var c2 = (((p.winX + p.winW) * codecW).roundToInt()).coerceIn(0, codecW)
        c2 = ((c2 + 1) and 1.inv()).coerceAtMost(codecW)
        val r1 = ((p.winY * codecH).toInt()).coerceIn(0, codecH) and 1.inv()
        var r2 = (((p.winY + p.winH) * codecH).roundToInt()).coerceIn(0, codecH)
        r2 = ((r2 + 1) and 1.inv()).coerceAtMost(codecH)

        if (c2 > c1 && r2 > r1) {
            val segBytes = (c2 - c1) * 2
            val wYSeg = makeYRow(segBytes, wY)
            for (r in r1 until r2) System.arraycopy(wYSeg, 0, frame, r * rowBytes + c1 * 2, segBytes)
            val wUVSeg = makeUVRow(segBytes, wCb, wCr)
            val cr1 = r1 / 2; val cr2 = r2 / 2
            for (r in cr1 until cr2) System.arraycopy(wUVSeg, 0, frame, yPlaneBytes + r * rowBytes + c1 * 2, segBytes)
        }
        return frame
    }

    /** KEY_HDR_STATIC_INFO：25 字节 CTA-861.3 Type 1（含开头 0x01）。 */
    private fun buildHdrStaticInfo(
        maxLuminance: Int = 1000, minLuminance: Int = 1, maxCll: Int = 1000, maxFall: Int = 400
    ): ByteBuffer {
        val buf = ByteBuffer.allocate(25).order(ByteOrder.LITTLE_ENDIAN)
        fun u16(v: Int) { buf.putShort((v and 0xFFFF).toShort()) }
        buf.put(0x01.toByte())
        u16(8500); u16(39850); u16(6550); u16(2300); u16(35400); u16(14600); u16(15635); u16(16450)
        u16(maxLuminance); u16(minLuminance); u16(maxCll); u16(maxFall)
        buf.rewind()
        return buf
    }
}
