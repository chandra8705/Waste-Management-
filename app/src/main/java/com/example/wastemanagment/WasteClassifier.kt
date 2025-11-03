package com.example.wastemanagment

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class WasteClassifier(private val context: Context) {

    private val modelFileName = "model.tflite"
    private val labelsFileName = "labels.txt"

    private var tflite: Interpreter? = null
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
            val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            tflite = Interpreter(modelBuffer)
        } catch (e: Exception) {
            // Model not found or failed to load
            println("Model not found: $modelFileName. Using mock classification.")
            println("Error: ${e.message}")
            println("Full error: ${e.stackTraceToString()}")
            tflite = null
        }
    }

    private fun loadLabels() {
        try {
            val inputStream = context.assets.open(labelsFileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            labels = reader.readLines().map { it.trim() }
            reader.close()
        } catch (e: Exception) {
            // Fallback to the 5 required categories if labels.txt doesn't exist
            labels = listOf(
                "Glass",
                "Biodegradable",
                "Non-Biodegradable",
                "Organic",
                "Inorganic"
            )
        }
    }

    data class Prediction(val label: String, val confidence: Float)

    fun classify(bitmap: Bitmap): Prediction {
        return try {
            // Try to use the actual TensorFlow Lite model first
            if (tflite != null) {
                performActualClassification(bitmap)
            } else {
                // Fallback to mock classification if model is not available
                val pixelData = getAverageColor(bitmap)
                val prediction = determineWasteType(pixelData)
                prediction
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback prediction
            Prediction("Unknown", 0.5f)
        }
    }

    private fun performActualClassification(bitmap: Bitmap): Prediction {
        val interpreter = tflite ?: return getMockPrediction()
        
        try {
            // Resize bitmap to model input size
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputImageWidth, inputImageHeight, true)
            
            // Convert bitmap to ByteBuffer
            val inputBuffer = convertBitmapToByteBuffer(resizedBitmap)
            
            // Prepare output array
            val outputArray = Array(1) { FloatArray(labels.size) }
            
            // Run inference
            interpreter.run(inputBuffer, outputArray)
            
            // Process results
            val probabilities = outputArray[0]
            val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
            val confidence = probabilities[maxIndex]
            val label = if (maxIndex < labels.size) labels[maxIndex] else "Unknown"
            
            return Prediction(label, confidence)
        } catch (e: Exception) {
            e.printStackTrace()
            return getMockPrediction()
        }
    }
    
    private fun getMockPrediction(): Prediction {
        // Consistent fallback prediction when model is not available
        // This ensures the same input always gives the same output
        return Prediction("Unknown", 0.5f)
    }
    
    private fun getAverageColor(bitmap: Bitmap): IntArray {
        // Calculate average RGB values from the bitmap
        var redSum = 0
        var greenSum = 0
        var blueSum = 0
        val width = bitmap.width
        val height = bitmap.height
        val pixelCount = width * height
        
        // Sample pixels (using a stride to improve performance)
        val stride = 10
        var sampleCount = 0
        
        for (y in 0 until height step stride) {
            for (x in 0 until width step stride) {
                val pixel = bitmap.getPixel(x, y)
                redSum += (pixel shr 16) and 0xFF
                greenSum += (pixel shr 8) and 0xFF
                blueSum += pixel and 0xFF
                sampleCount++
            }
        }
        
        // Calculate averages
        val avgRed = redSum / sampleCount
        val avgGreen = greenSum / sampleCount
        val avgBlue = blueSum / sampleCount
        
        return intArrayOf(avgRed, avgGreen, avgBlue)
    }
    
    private fun determineWasteType(pixelData: IntArray): Prediction {
        val (red, green, blue) = pixelData
        
        // Improved color-based heuristics for waste classification with better plastic bottle detection
        // In a real app, this would be replaced by actual model inference
        
        // Check for plastic bottle characteristics
        val isLikelyPlastic = (blue > 100 && green > 100) || 
                             (red > 180 && green > 180 && blue > 180) ||
                             (blue > red * 0.8 && blue > green * 0.8)
        
        if (isLikelyPlastic) {
            return Prediction("Non-Biodegradable", 0.88f)
        }
        
        return when {
            // Clear/transparent items (likely glass)
            blue > 200 && red > 200 && green > 200 -> {
                Prediction("Glass", 0.85f)
            }
            // Green items (likely organic/biodegradable)
            green > red && green > blue -> {
                Prediction("Biodegradable", 0.78f)
            }
            // Blue/dark items (likely plastic or metal)
            blue > red && blue > green -> {
                if (blue - red > 50) {
                    Prediction("Non-Biodegradable", 0.82f)
                } else {
                    Prediction("Inorganic", 0.75f)
                }
            }
            // Brown/yellow items (likely paper or organic)
            red > blue && green > blue -> {
                Prediction("Organic", 0.79f)
            }
            // Default case - consistent prediction based on dominant color
            else -> {
                // Use the dominant color to make a consistent prediction
                val dominantColor = when {
                    red > green && red > blue -> "Organic"
                    green > red && green > blue -> "Biodegradable"
                    blue > red && blue > green -> "Non-Biodegradable"
                    else -> "Inorganic"
                }
                Prediction(dominantColor, 0.70f)
            }
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

                // Normalize pixel values to [0, 1]
                byteBuffer.putFloat(((value shr 16) and 0xFF) / 255.0f) // Red
                byteBuffer.putFloat(((value shr 8) and 0xFF) / 255.0f)  // Green
                byteBuffer.putFloat((value and 0xFF) / 255.0f)          // Blue
            }
        }

        return byteBuffer
    }

    fun isModelLoaded(): Boolean {
        return tflite != null
    }
    
    fun getModelInfo(): String {
        return if (tflite != null) {
            "TensorFlow Lite model loaded successfully (${inputImageWidth}x${inputImageHeight})"
        } else {
            "Using mock classification (model not loaded)"
        }
    }

    fun close() {
        // Close TensorFlow Lite interpreter if it exists
        tflite?.close()
        tflite = null
    }
}
