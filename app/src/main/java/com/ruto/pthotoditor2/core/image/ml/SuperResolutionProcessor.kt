package com.ruto.photoeditor2.core.image.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import com.ruto.pthotoditor2.core.image.commonutil.HardwareBitmapConvert.ensureSoftwareConfig

import com.ruto.pthotoditor2.core.image.ml.policy.UpscalePolicy
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.ceil

object SuperResolutionHelper {

    private lateinit var interpreter: Interpreter
    private const val MODEL_NAME = "real_esrgan_general_x4v3.tflite"
    private const val TAG = "SuperResolution"

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(MODEL_NAME)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    private fun createInterpreter(context: Context): Interpreter {
        val modelBuffer = loadModelFile(context)
        val compatList = CompatibilityList()

        val options = Interpreter.Options().apply {
            if (compatList.isDelegateSupportedOnThisDevice) {
                val delegate = GpuDelegate(compatList.bestOptionsForThisDevice)
                addDelegate(delegate)
                Log.d(TAG, "Using GPU delegate")
            } else {
                setNumThreads(4)
                Log.d(TAG, "Using CPU fallback")
            }
        }
        return Interpreter(modelBuffer, options)
    }

    fun initialize(context: Context) {
        if (!::interpreter.isInitialized) {
            interpreter = createInterpreter(context)
        }
    }

    fun upscale(context: Context, bitmap: Bitmap): Bitmap {
        try {
            initialize(context)

            val preset = UpscalePolicy.calculate(bitmap.width, bitmap.height)
            Log.d("SuperResolution", "📐 업스케일 사전 설정 - inputScaleRatio=${preset.inputScaleRatio}, outputSize=${preset.finalOutputWidth}x${preset.finalOutputHeight}")
            val resizedBitmap = if (preset.inputScaleRatio < 1.0f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * preset.inputScaleRatio).toInt(),
                    (bitmap.height * preset.inputScaleRatio).toInt(),
                    true
                )
            } else {
                bitmap
            }

            val inputTileSize = 128
            val outputTileSize = 512
            val xTiles = ceil(resizedBitmap.width / inputTileSize.toDouble()).toInt()
            val yTiles = ceil(resizedBitmap.height / inputTileSize.toDouble()).toInt()

            val paddedWidth = xTiles * inputTileSize
            val paddedHeight = yTiles * inputTileSize
            val scaledInput = Bitmap.createScaledBitmap(resizedBitmap, paddedWidth, paddedHeight, true)
            val upscaledBitmap = Bitmap.createBitmap(paddedWidth * 4, paddedHeight * 4, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(upscaledBitmap)

            for (yt in 0 until yTiles) {
                for (xt in 0 until xTiles) {
                    val tile = Bitmap.createBitmap(scaledInput, xt * inputTileSize, yt * inputTileSize, inputTileSize, inputTileSize)
                    val tensorImage = TensorImage(DataType.FLOAT32)
                    tensorImage.load(tile.ensureSoftwareConfig())

                    val imageProcessor = ImageProcessor.Builder().add(NormalizeOp(0f, 255f)).build()
                    val processed = imageProcessor.process(tensorImage)

                    val outputBuffer = ByteBuffer.allocateDirect(4 * outputTileSize * outputTileSize * 3)
                    outputBuffer.order(java.nio.ByteOrder.nativeOrder())
                    interpreter.run(processed.buffer, outputBuffer)
                    outputBuffer.rewind()

                    val floatBuffer = outputBuffer.asFloatBuffer()
                    val floatArray = FloatArray(floatBuffer.capacity())
                    floatBuffer.get(floatArray)

                    val tileBitmap = Bitmap.createBitmap(outputTileSize, outputTileSize, Bitmap.Config.ARGB_8888)
                    for (y in 0 until outputTileSize) {
                        for (x in 0 until outputTileSize) {
                            val baseIndex = (y * outputTileSize + x) * 3
                            val r = (floatArray[baseIndex].coerceIn(0f, 1f) * 255f).toInt()
                            val g = (floatArray[baseIndex + 1].coerceIn(0f, 1f) * 255f).toInt()
                            val b = (floatArray[baseIndex + 2].coerceIn(0f, 1f) * 255f).toInt()
                            tileBitmap.setPixel(x, y, (0xFF shl 24) or (r shl 16) or (g shl 8) or b)
                        }
                    }

                    canvas.drawBitmap(tileBitmap, (xt * outputTileSize).toFloat(), (yt * outputTileSize).toFloat(), null)
                }
            }



            return if (preset.inputScaleRatio < 1.0f) {
                Bitmap.createScaledBitmap(
                    upscaledBitmap,
                    preset.finalOutputWidth,
                    preset.finalOutputHeight,
                    true
                )
            } else {
                upscaledBitmap
            }

        } catch (e: Exception) {
            Log.e("SuperResolution", "Upscale failed: ${e.message}", e)
            return bitmap
        }
    }
}
