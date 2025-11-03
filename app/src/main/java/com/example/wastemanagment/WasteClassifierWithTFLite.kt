package com.example.wastemanagment

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel


class WasteClassifierWithTFLite(private val context: Context) {

    private val modelFileName = "model.tflite"
    private val labelsFileName = "labels.txt"

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private var inputImageWidth = 224
    private var inputImageHeight = 224

    init {
        try {
            loadModel()
            loadLabels()
        } catch (e: Exception) {
            throw RuntimeException("Failed to initialize WasteClassifier: ${e.message}", e)
        }
    }

    private fun loadModel() {
        try {
            val assetFileDescriptor = context.assets.openFd(modelFileName)
            val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = fileInputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val modelBuffer =
                fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(modelBuffer, options)

            val inputShape = interpreter!!.getInputTensor(0).shape()
            inputImageHeight = inputShape[1]
            inputImageWidth = inputShape[2]

        } catch (e: Exception) {
            throw RuntimeException(
                "Failed to load model. Make sure '$modelFileName' exists in assets/ folder",
                e
            )
        }
    }

    private fun loadLabels() {
        try {
            val inputStream = context.assets.open(labelsFileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            labels = reader.readLines().map { it.trim() }
            reader.close()
        } catch (e: Exception) {
            labels = listOf(
                "cardboard",
                "glass",
                "metal",
                "paper",
                "plastic",
                "trash"
            )
        }
    }

    data class Prediction(val label: String, val confidence: Float)

    fun classify(bitmap: Bitmap): Prediction {
        val interpreter = this.interpreter
            ?: throw IllegalStateException("Model not loaded")

        try {
            val resizedBitmap =
                Bitmap.createScaledBitmap(bitmap, inputImageWidth, inputImageHeight, true)
            val inputBuffer = convertBitmapToByteBuffer(resizedBitmap)

            val outputShape = interpreter.getOutputTensor(0).shape()
            val numClasses = outputShape[outputShape.size - 1]
            val outputBuffer = Array(1) { FloatArray(numClasses) }

            interpreter.run(inputBuffer, outputBuffer)

            val scores = outputBuffer[0]

            val expScores = scores.map { kotlin.math.exp(it.toDouble()).toFloat() }
            val sumExpScores = expScores.sum()
            val softmaxScores = expScores.map { it / sumExpScores }

            val maxIndex = softmaxScores.indices.maxByOrNull { softmaxScores[it] } ?: 0
            val confidence = softmaxScores[maxIndex]

            val predictedLabel = if (maxIndex < labels.size) {
                labels[maxIndex].replaceFirstChar { it.uppercase() }
            } else {
                "Unknown Class $maxIndex"
            }

            return Prediction(predictedLabel, confidence)

        } catch (e: Exception) {
            throw RuntimeException("Classification failed: ${e.message}", e)
        }
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputImageWidth * inputImageHeight * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputImageWidth * inputImageHeight)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until inputImageHeight) {
            for (j in 0 until inputImageWidth) {
                val value = intValues[pixel++]

                byteBuffer.putFloat(((value shr 16) and 0xFF) / 255.0f) // Red
                byteBuffer.putFloat(((value shr 8) and 0xFF) / 255.0f)  // Green
                byteBuffer.putFloat((value and 0xFF) / 255.0f)          // Blue
            }
        }

        return byteBuffer
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}