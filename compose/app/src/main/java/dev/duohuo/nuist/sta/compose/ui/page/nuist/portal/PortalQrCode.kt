// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 NUIST++ contributors
package dev.duohuo.nuist.sta.compose.ui.page.nuist.portal

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter

/**
 * 通行密钥二维码的编解码。
 *
 * 导出文本约 150–200 字符，二维码用 M 级纠错即可容纳，尺寸取 768px 方便相机远距离识别；
 * 解码直接吃 CameraX 的 YUV_420_888 Y 平面，省掉一次转码。
 */
internal object PortalQrCode {

    private const val DEFAULT_SIZE = 768

    fun encode(text: String, sizePx: Int = DEFAULT_SIZE): Bitmap? = runCatching {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        )
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) {
            val row = y * sizePx
            for (x in 0 until sizePx) {
                pixels[row + x] = if (matrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }
        Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
        }
    }.getOrNull()

    fun decode(image: ImageProxy): String? {
        val plane = image.planes.firstOrNull() ?: return null
        val width = image.width
        val height = image.height
        val rowStride = plane.rowStride
        if (width <= 0 || height <= 0 || rowStride < width) return null
        val needed = rowStride * height
        val buffer = plane.buffer
        if (buffer.remaining() < needed) return null
        val data = ByteArray(needed)
        buffer.get(data)
        // 相机传感器的 Y 平面通常带 90° 旋转，QR 解不出来，先按 imageInfo 旋正。
        val rotation = image.imageInfo.rotationDegrees
        val frame = rotateLuminance(data, width, height, rowStride, rotation)
        val source = PlanarYUVLuminanceSource(frame.data, frame.stride, frame.height, 0, 0, frame.width, frame.height, false)
        val reader = MultiFormatReader()
        reader.setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true,
            ),
        )
        return try {
            reader.decode(BinaryBitmap(HybridBinarizer(source)))?.text
        } catch (_: Exception) {
            null
        } finally {
            reader.reset()
        }
    }

    private data class Frame(val data: ByteArray, val width: Int, val height: Int, val stride: Int)

    private fun rotateLuminance(
        data: ByteArray,
        width: Int,
        height: Int,
        stride: Int,
        degrees: Int,
    ): Frame {
        val normalized = ((degrees % 360) + 360) % 360
        if (normalized == 0) return Frame(data, width, height, stride)
        return when (normalized) {
            90 -> Frame(rotate90(data, width, height, stride, clockwise = true), height, width, height)
            180 -> Frame(rotate180(data, width, height, stride), width, height, width)
            else -> Frame(rotate90(data, width, height, stride, clockwise = false), height, width, height)
        }
    }

    private fun rotate90(data: ByteArray, width: Int, height: Int, stride: Int, clockwise: Boolean): ByteArray {
        val out = ByteArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val value = data[y * stride + x]
                if (clockwise) {
                    out[x * height + (height - 1 - y)] = value
                } else {
                    out[(width - 1 - x) * height + y] = value
                }
            }
        }
        return out
    }

    private fun rotate180(data: ByteArray, width: Int, height: Int, stride: Int): ByteArray {
        val out = ByteArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                out[(height - 1 - y) * width + (width - 1 - x)] = data[y * stride + x]
            }
        }
        return out
    }
}