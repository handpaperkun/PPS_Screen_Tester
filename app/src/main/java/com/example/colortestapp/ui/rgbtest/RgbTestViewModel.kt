package com.example.colortestapp.ui.rgbtest

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorSpace
import android.graphics.Gainmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.Deflater

enum class RgbTestMode(val label: String) {
    WIDE_GAMUT("广色域"), RGB_GAINMAP("HDR Photo-RGB GainMap"), GRAYSCALE_GRADIENT("极低灰阶截断")
}

class RgbTestViewModel : ViewModel() {
    var currentMode = RgbTestMode.WIDE_GAMUT

    // 从已确认正确显示 P3 的 PNG 提取的 ICC (Adobe v4.2.0, 620 bytes)
    private val P3_ICC = intArrayOf(
        0x00,0x00,0x02,0x6C,0x41,0x44,0x42,0x45,0x04,0x20,0x00,0x00,0x6D,0x6E,0x74,0x72,
        0x52,0x47,0x42,0x20,0x58,0x59,0x5A,0x20,0x07,0xE1,0x00,0x03,0x00,0x04,0x00,0x17,
        0x00,0x2A,0x00,0x2F,0x61,0x63,0x73,0x70,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
        0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
        0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0xF6,0xD6,0x00,0x01,0x00,0x00,
        0x00,0x00,0xD3,0x2D,0x41,0x44,0x42,0x45,0xC7,0x72,0xB2,0xD2,0x6A,0x8F,0x24,0x50,
        0xCE,0x0D,0xA7,0xAC,0x2C,0x51,0x1F,0x4C,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
        0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
        0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x0C,0x64,0x65,
        0x73,0x63,0x00,0x00,0x01,0x14,0x00,0x00,0x00,0x2C,0x63,0x70,0x72,0x74,0x00,0x00,
        0x01,0x40,0x00,0x00,0x00,0x6E,0x77,0x74,0x70,0x74,0x00,0x00,0x01,0xB0,0x00,0x00,
        0x00,0x14,0x63,0x68,0x61,0x64,0x00,0x00,0x01,0xC4,0x00,0x00,0x00,0x2C,0x74,0x65,
        0x63,0x68,0x00,0x00,0x01,0xF0,0x00,0x00,0x00,0x0C,0x6C,0x75,0x6D,0x69,0x00,0x00,
        0x01,0xFC,0x00,0x00,0x00,0x14,0x72,0x54,0x52,0x43,0x00,0x00,0x02,0x10,0x00,0x00,
        0x00,0x20,0x67,0x54,0x52,0x43,0x00,0x00,0x02,0x10,0x00,0x00,0x00,0x20,0x62,0x54,
        0x52,0x43,0x00,0x00,0x02,0x10,0x00,0x00,0x00,0x20,0x72,0x58,0x59,0x5A,0x00,0x00,
        0x02,0x30,0x00,0x00,0x00,0x14,0x67,0x58,0x59,0x5A,0x00,0x00,0x02,0x44,0x00,0x00,
        0x00,0x14,0x62,0x58,0x59,0x5A,0x00,0x00,0x02,0x58,0x00,0x00,0x00,0x14,0x6D,0x6C,
        0x75,0x63,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x01,0x00,0x00,0x00,0x0C,0x65,0x6E,
        0x55,0x53,0x00,0x00,0x00,0x10,0x00,0x00,0x00,0x1C,0x00,0x69,0x00,0x6D,0x00,0x61,
        0x00,0x67,0x00,0x65,0x00,0x20,0x00,0x50,0x00,0x33,0x6D,0x6C,0x75,0x63,0x00,0x00,
        0x00,0x00,0x00,0x00,0x00,0x01,0x00,0x00,0x00,0x0C,0x65,0x6E,0x55,0x53,0x00,0x00,
        0x00,0x52,0x00,0x00,0x00,0x1C,0x00,0x43,0x00,0x6F,0x00,0x70,0x00,0x79,0x00,0x72,
        0x00,0x69,0x00,0x67,0x00,0x68,0x00,0x74,0x00,0x20,0x00,0x32,0x00,0x30,0x00,0x31,
        0x00,0x37,0x00,0x20,0x00,0x41,0x00,0x64,0x00,0x6F,0x00,0x62,0x00,0x65,0x00,0x20,
        0x00,0x53,0x00,0x79,0x00,0x73,0x00,0x74,0x00,0x65,0x00,0x6D,0x00,0x73,0x00,0x20,
        0x00,0x49,0x00,0x6E,0x00,0x63,0x00,0x6F,0x00,0x72,0x00,0x70,0x00,0x6F,0x00,0x72,
        0x00,0x61,0x00,0x74,0x00,0x65,0x00,0x64,0x00,0x00,0x58,0x59,0x5A,0x20,0x00,0x00,
        0x00,0x00,0x00,0x00,0xF6,0xD6,0x00,0x01,0x00,0x00,0x00,0x00,0xD3,0x2D,0x73,0x66,
        0x33,0x32,0x00,0x00,0x00,0x00,0x00,0x01,0x0C,0x42,0x00,0x00,0x05,0xDE,0xFF,0xFF,
        0xF3,0x25,0x00,0x00,0x07,0x93,0x00,0x00,0xFD,0x90,0xFF,0xFF,0xFB,0xA1,0xFF,0xFF,
        0xFD,0xA2,0x00,0x00,0x03,0xDC,0x00,0x00,0xC0,0x6E,0x73,0x69,0x67,0x20,0x00,0x00,
        0x00,0x00,0x76,0x69,0x64,0x6D,0x58,0x59,0x5A,0x20,0x00,0x00,0x00,0x00,0x00,0x00,
        0x00,0x00,0x00,0x64,0x00,0x00,0x00,0x00,0x00,0x00,0x70,0x61,0x72,0x61,0x00,0x00,
        0x00,0x00,0x00,0x03,0x00,0x00,0x00,0x02,0x66,0x66,0x00,0x00,0xF2,0xA7,0x00,0x00,
        0x0D,0x59,0x00,0x00,0x13,0xD0,0x00,0x00,0x0A,0x5B,0x58,0x59,0x5A,0x20,0x00,0x00,
        0x00,0x00,0x00,0x00,0x83,0xDF,0x00,0x00,0x3D,0xBF,0xFF,0xFF,0xFF,0xBB,0x58,0x59,
        0x5A,0x20,0x00,0x00,0x00,0x00,0x00,0x00,0x4A,0xBF,0x00,0x00,0xB1,0x37,0x00,0x00,
        0x0A,0xB9,0x58,0x59,0x5A,0x20,0x00,0x00,0x00,0x00,0x00,0x00,0x28,0x38,0x00,0x00,
        0x11,0x0B,0x00,0x00,0xC8,0xB9
    ).map { it.toByte() }.toByteArray()

    @SuppressLint("NewApi")
    fun generateP3Bitmap(w: Int, h: Int): Bitmap {
        val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888, true, ColorSpace.get(ColorSpace.Named.DISPLAY_P3))
        else Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            bmp.setColorSpace(ColorSpace.get(ColorSpace.Named.DISPLAY_P3))
        drawRgbBars(Canvas(bmp), w, h); return bmp
    }

    fun generateSrgbBars(w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); drawRgbBars(Canvas(bmp), w, h); return bmp
    }

    private fun drawRgbBars(cv: Canvas, w: Int, h: Int) {
        val bh = h / 3f; val p = android.graphics.Paint()
        for (row in 0..2) { p.color = when(row) { 0->android.graphics.Color.rgb(255,0,0); 1->android.graphics.Color.rgb(0,255,0); else->android.graphics.Color.rgb(0,0,255) }; cv.drawRect(0f, row*bh, w.toFloat(), (row+1)*bh, p) }
    }

    fun decodeP3Jpeg(ctx: Context, w: Int, h: Int): Bitmap? {
        return try {
            val bmp = generateP3Bitmap(w, h)
            val tmp = File(ctx.cacheDir, "p3.jpg"); java.io.FileOutputStream(tmp).use { bmp.compress(Bitmap.CompressFormat.JPEG, 100, it) }; bmp.recycle()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) ImageDecoder.decodeBitmap(ImageDecoder.createSource(tmp)) else BitmapFactory.decodeFile(tmp.absolutePath)
        } catch (_: Exception) { null }
    }

    fun decodeSrgbJpeg(ctx: Context, w: Int, h: Int): Bitmap? {
        return try {
            val bmp = generateSrgbBars(w, h)
            val tmp = File(ctx.cacheDir, "srgb.jpg"); java.io.FileOutputStream(tmp).use { bmp.compress(Bitmap.CompressFormat.JPEG, 100, it) }; bmp.recycle()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) ImageDecoder.decodeBitmap(ImageDecoder.createSource(tmp)) else BitmapFactory.decodeFile(tmp.absolutePath)
        } catch (_: Exception) { null }
    }

    /** 从 assets 加载预置图片并拉伸到目标尺寸 */
    fun loadAssetBitmap(ctx: Context, assetName: String, targetW: Int, targetH: Int): Bitmap? {
        return try {
            val src = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.assets, assetName))
            else BitmapFactory.decodeStream(ctx.assets.open(assetName))
            src?.let { Bitmap.createScaledBitmap(it, targetW, targetH, true).also { if (it !== src) src.recycle() } }
        } catch (_: Exception) { null }
    }
    fun generateRgbBaseBitmap(w: Int, h: Int) = generateSrgbBars(w, h)

    fun generateRgbGainmapBitmap(w: Int, h: Int): Bitmap {
        val base = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); drawRgbBars(Canvas(base), w, h)
        val hw = w/2; val gm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        for (y in 0 until h) for (x in 0 until w) gm.setPixel(x, y, if (x<hw) android.graphics.Color.rgb(255,255,255) else android.graphics.Color.rgb(0,85,170))
        base.setGainmap(Gainmap(gm).apply { setRatioMin(1f,1f,1f); setRatioMax(4f,4f,4f); setGamma(1f,1f,1f); displayRatioForFullHdr=4f; minDisplayRatioForHdrTransition=1f })
        return base
    }
    /** 截断测试: W/R/G/B 四列, 码值 0→30 */
    fun generateClippingBitmap(w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        val colW = w / 4
        for (y in 0 until h) {
            val t = y.toFloat() / h
            val v = (t * 30).toInt().coerceIn(0, 255)
            for (x in 0 until w) {
                val col = (x / colW).coerceIn(0, 3)
                val color = when (col) {
                    0 -> android.graphics.Color.rgb(v, v, v)
                    1 -> android.graphics.Color.rgb(v, 0, 0)
                    2 -> android.graphics.Color.rgb(0, v, 0)
                    else -> android.graphics.Color.rgb(0, 0, v)
                }
                pixels[y * w + x] = color
            }
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp
    }

    /** PNG + iCCP chunk (和正确 P3.png 完全相同的方式) */
    suspend fun saveP3Png(ctx: Context, w: Int, h: Int, prefix: String) {
        withContext(Dispatchers.IO) {
            try {
                val bmp = generateP3Bitmap(w, h)
                val bos = ByteArrayOutputStream(); bmp.compress(Bitmap.CompressFormat.PNG, 100, bos); bmp.recycle()
                val png = injectIccp(bos.toByteArray(), "image P3".toByteArray(), P3_ICC)
                val v = ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME,"${prefix}_${System.currentTimeMillis()}.png");put(MediaStore.Images.Media.MIME_TYPE,"image/png")
                    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){put(MediaStore.Images.Media.RELATIVE_PATH,Environment.DIRECTORY_PICTURES+"/PPSTest");put(MediaStore.Images.Media.IS_PENDING,1)}}
                ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,v)?.let{uri->
                    ctx.contentResolver.openOutputStream(uri)?.use{it.write(png)}
                    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){v.clear();v.put(MediaStore.Images.Media.IS_PENDING,0);ctx.contentResolver.update(uri,v,null,null)}}
                android.os.Handler(android.os.Looper.getMainLooper()).post{Toast.makeText(ctx,"已保存 $prefix",Toast.LENGTH_SHORT).show()}
            } catch(e:Exception){android.os.Handler(android.os.Looper.getMainLooper()).post{Toast.makeText(ctx,"失败: ${e.message}",Toast.LENGTH_SHORT).show()}}
        }
    }

    /** 注入 PNG iCCP chunk (deflate 压缩的 ICC profile) */
    private fun injectIccp(png: ByteArray, profileName: ByteArray, icc: ByteArray): ByteArray {
        // deflate ICC
        val def = Deflater(9)
        def.setInput(icc); def.finish()
        val comp = ByteArray(icc.size + 64)
        val compLen = def.deflate(comp)
        def.end()

        // iCCP chunk data: profileName + \0 + compression(0) + compressed ICC
        val chunkData = ByteArray(profileName.size + 1 + 1 + compLen)
        System.arraycopy(profileName, 0, chunkData, 0, profileName.size)
        chunkData[profileName.size] = 0  // null terminator
        chunkData[profileName.size + 1] = 0  // compression method (deflate)
        System.arraycopy(comp, 0, chunkData, profileName.size + 2, compLen)

        // iCCP chunk: length(4) + type(4) + data + CRC(4)
        val type = "iCCP".toByteArray(Charsets.US_ASCII)
        val crcInput = ByteArray(4 + chunkData.size)
        System.arraycopy(type, 0, crcInput, 0, 4)
        System.arraycopy(chunkData, 0, crcInput, 4, chunkData.size)
        val crc = crc32(crcInput)

        // 插入到 IEND 之前
        val iend = findIend(png)
        if (iend < 0) return png
        val result = ByteArray(png.size + 4 + 4 + chunkData.size + 4)
        var p = 0
        System.arraycopy(png, 0, result, p, iend); p += iend
        // length
        result[p++] = ((chunkData.size shr 24) and 0xFF).toByte(); result[p++] = ((chunkData.size shr 16) and 0xFF).toByte(); result[p++] = ((chunkData.size shr 8) and 0xFF).toByte(); result[p++] = (chunkData.size and 0xFF).toByte()
        System.arraycopy(type, 0, result, p, 4); p += 4
        System.arraycopy(chunkData, 0, result, p, chunkData.size); p += chunkData.size
        // CRC
        result[p++] = ((crc shr 24) and 0xFF).toByte(); result[p++] = ((crc shr 16) and 0xFF).toByte(); result[p++] = ((crc shr 8) and 0xFF).toByte(); result[p++] = (crc and 0xFF).toByte()
        System.arraycopy(png, iend, result, p, png.size - iend)
        return result
    }

    private fun findIend(png: ByteArray): Int {
        val iend = byteArrayOf(0,0,0,0, 0x49,0x45,0x4E,0x44)
        for (i in 0 until png.size - 8) { var m = true; for (j in 0..7) if (png[i+j] != iend[j]) { m = false; break }; if (m) return i }
        return -1
    }

    private fun crc32(data: ByteArray): Int { val c = java.util.zip.CRC32(); c.update(data); return c.value.toInt() }

    /** 保存带 Gainmap 的 Ultra HDR JPEG（确保元数据声明） */
    suspend fun saveJpegGain(ctx: Context, bitmap: Bitmap, prefix: String) {
        withContext(Dispatchers.IO) {
            try {
                // 确保 attach gainmap（可能在 remember 中丢失）
                var bmp = bitmap
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !bmp.hasGainmap()) {
                    // 重新生成 gainmap
                    bmp = generateRgbGainmapBitmap(bmp.width, bmp.height)
                }
                val bos = java.io.ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, bos)
                val jpeg = bos.toByteArray()

                // 追加 XMP 元数据 (hdrgm namespace) — 确保非 Pixel 设备也识别
                val xmp = """<x:xmpmeta xmlns:x="adobe:ns:meta/"><rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"><rdf:Description rdf:about="" xmlns:hdrgm="http://ns.adobe.com/hdr-gain-map/1.0/" hdrgm:Version="1.0" hdrgm:BaseRenditionIsHDR="False"/></rdf:RDF></x:xmpmeta>"""
                val jpegWithXmp = injectXmpApp1(jpeg, xmp)

                val v = ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME,"${prefix}_${System.currentTimeMillis()}.jpg");put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg")
                    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){put(MediaStore.Images.Media.RELATIVE_PATH,Environment.DIRECTORY_PICTURES+"/PPSTest");put(MediaStore.Images.Media.IS_PENDING,1)}}
                ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,v)?.let{uri->
                    ctx.contentResolver.openOutputStream(uri)?.use{it.write(jpegWithXmp)}
                    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){v.clear();v.put(MediaStore.Images.Media.IS_PENDING,0);ctx.contentResolver.update(uri,v,null,null)}}
                android.os.Handler(android.os.Looper.getMainLooper()).post{Toast.makeText(ctx,"已保存 $prefix (Ultra HDR)",Toast.LENGTH_SHORT).show()}
            } catch(e:Exception){android.os.Handler(android.os.Looper.getMainLooper()).post{Toast.makeText(ctx,"失败: ${e.message}",Toast.LENGTH_SHORT).show()}}
        }
    }

    /** 在 JPEG SOI 之后注入 XMP APP1 marker */
    private fun injectXmpApp1(jpeg: ByteArray, xmp: String): ByteArray {
        if (jpeg.size < 2 || jpeg[0] != 0xFF.toByte() || jpeg[1] != 0xD8.toByte()) return jpeg
        val marker = byteArrayOf(0xFF.toByte(), 0xE1.toByte()) // APP1
        val ns = "http://ns.adobe.com/xap/1.0/\u0000".toByteArray(Charsets.US_ASCII)
        val xmpBytes = xmp.toByteArray(Charsets.UTF_8)
        val data = ByteArray(ns.size + xmpBytes.size)
        System.arraycopy(ns, 0, data, 0, ns.size)
        System.arraycopy(xmpBytes, 0, data, ns.size, xmpBytes.size)
        val len = (data.size + 2).toShort()
        val result = ByteArray(jpeg.size + 2 + 2 + data.size + 2)
        var p = 0
        result[p++] = 0xFF.toByte(); result[p++] = 0xD8.toByte()
        System.arraycopy(marker, 0, result, p, 2); p += 2
        result[p++] = ((len.toInt() shr 8) and 0xFF).toByte(); result[p++] = (len.toInt() and 0xFF).toByte()
        System.arraycopy(data, 0, result, p, data.size); p += data.size
        System.arraycopy(jpeg, 2, result, p, jpeg.size - 2)
        return result.copyOf(p + jpeg.size - 2)
    }

    suspend fun saveJpeg(ctx: Context, bitmap: Bitmap, prefix: String) {
        withContext(Dispatchers.IO) {
            try {
                val v = ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME,"${prefix}_${System.currentTimeMillis()}.jpg");put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg")
                    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){put(MediaStore.Images.Media.RELATIVE_PATH,Environment.DIRECTORY_PICTURES+"/PPSTest");put(MediaStore.Images.Media.IS_PENDING,1)}}
                ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,v)?.let{uri->
                    ctx.contentResolver.openOutputStream(uri)?.use{bitmap.compress(Bitmap.CompressFormat.JPEG,100,it)}
                    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){v.clear();v.put(MediaStore.Images.Media.IS_PENDING,0);ctx.contentResolver.update(uri,v,null,null)}}
                android.os.Handler(android.os.Looper.getMainLooper()).post{Toast.makeText(ctx,"已保存 $prefix",Toast.LENGTH_SHORT).show()}
            } catch(e:Exception){android.os.Handler(android.os.Looper.getMainLooper()).post{Toast.makeText(ctx,"失败: ${e.message}",Toast.LENGTH_SHORT).show()}}
        }
    }

    suspend fun savePng(ctx: Context, w: Int, h: Int, gen: (Int,Int)->Bitmap, prefix: String) {
        withContext(Dispatchers.IO) {
            try {
                val bmp = gen(w, h)
                val v = ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME,"${prefix}_${System.currentTimeMillis()}.png");put(MediaStore.Images.Media.MIME_TYPE,"image/png")
                    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){put(MediaStore.Images.Media.RELATIVE_PATH,Environment.DIRECTORY_PICTURES+"/PPSTest");put(MediaStore.Images.Media.IS_PENDING,1)}}
                ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,v)?.let{uri->
                    ctx.contentResolver.openOutputStream(uri)?.use{bmp.compress(Bitmap.CompressFormat.PNG,100,it)}
                    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){v.clear();v.put(MediaStore.Images.Media.IS_PENDING,0);ctx.contentResolver.update(uri,v,null,null)}}
                bmp.recycle()
                android.os.Handler(android.os.Looper.getMainLooper()).post{Toast.makeText(ctx,"已保存 $prefix",Toast.LENGTH_SHORT).show()}
            } catch(e:Exception){android.os.Handler(android.os.Looper.getMainLooper()).post{Toast.makeText(ctx,"失败: ${e.message}",Toast.LENGTH_SHORT).show()}}
        }
    }
}
