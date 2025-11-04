package com.example.wastemanagment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import com.example.wastemanagment.WasteClassificationViewModel
import com.example.wastemanagment.ui.AIChatbotActivity
import com.example.wastemanagment.ui.LanguageSelectionActivity
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: WasteClassificationViewModel

    private lateinit var buttonCamera: Button
    private lateinit var buttonPick: Button
    private lateinit var buttonProfile: Button
    private lateinit var buttonSettings: Button
    private lateinit var buttonChatbot: Button
    private lateinit var buttonSubmit: Button
    private lateinit var imagePreview: ImageView
    private lateinit var textResult: TextView
    private lateinit var textClassificationResult: TextView

    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    private var photoUri: Uri? = null
    private var currentBitmap: android.graphics.Bitmap? = null
    private var currentImageUri: Uri? = null

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        buttonPick = findViewById(R.id.buttonPick)
        buttonCamera = findViewById(R.id.buttonCamera)
        buttonProfile = findViewById(R.id.buttonProfile)
        buttonSettings = findViewById(R.id.buttonSettings)
        buttonChatbot = findViewById(R.id.buttonChatbot)
        buttonSubmit = findViewById(R.id.buttonSubmit)
        imagePreview = findViewById(R.id.imagePreview)
        textResult = findViewById(R.id.textResult)
        textClassificationResult = findViewById(R.id.textClassificationResult)

        viewModel = ViewModelProvider(this)[WasteClassificationViewModel::class.java]

        setupObservers()
        registerLaunchers()
        requestPermissions()

        buttonPick.setOnClickListener {
            pickImageFromGallery()
        }

        buttonCamera.setOnClickListener {
            takePhotoFromCamera()
        }
        
        buttonProfile.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            startActivity(intent)
        }
        
        buttonSettings.setOnClickListener {
            val intent = Intent(this, LanguageSelectionActivity::class.java)
            startActivity(intent)
        }
        
        buttonChatbot.setOnClickListener {
            val intent = Intent(this, AIChatbotActivity::class.java)
            startActivity(intent)
        }

        buttonSubmit.setOnClickListener {
            currentBitmap?.let { bitmap ->
                submitImageForClassification(bitmap)
            } ?: run {
                Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            }
        }

        testModelIntegration()
    }

    private fun setupObservers() {
        viewModel.predictionResult.observe(this) { prediction ->
            // Map classification to Organic or Inorganic
            val wasteCategory = mapToOrganicInorganic(prediction.label)
            val confidencePercent = (prediction.confidence * 100f)
            
            // Show classification result prominently
            textClassificationResult.visibility = android.view.View.VISIBLE
            textClassificationResult.text = "$wasteCategory\n${"%.1f".format(confidencePercent)}% Confidence"
            
            // Show detailed result below
            textResult.text = getString(
                R.string.prediction_result,
                prediction.label,
                "%.2f".format(confidencePercent)
            )
        }
        
        viewModel.disposalAdvice.observe(this) { advice ->
            if (advice != null) {
                val predictionText = textResult.text.toString()
                val fullText =
                    "$predictionText\n\n${advice.icon} Disposal Advice:\n${advice.advice}"
                textResult.text = fullText
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            buttonPick.isEnabled = !isLoading
            buttonCamera.isEnabled = !isLoading
            buttonSubmit.isEnabled = !isLoading
            
            if (isLoading) {
                textResult.text = getString(R.string.classifying)
                textClassificationResult.visibility = android.view.View.GONE
            } else {
                // Classification complete, reset UI to allow new image selection
                if (currentBitmap != null) {
                    hideSubmitButton()
                    currentBitmap = null
                    currentImageUri = null
                }
            }
        }

        viewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }
    
    private fun mapToOrganicInorganic(label: String): String {
        return when (label.lowercase()) {
            "organic", "biodegradable" -> "Organic"
            "inorganic", "non-biodegradable", "glass", "plastic", "metal" -> "Inorganic"
            else -> {
                // Try to determine based on common patterns
                if (label.contains("organic", ignoreCase = true) || 
                    label.contains("biodegradable", ignoreCase = true) ||
                    label.contains("compost", ignoreCase = true)) {
                    "Organic"
                } else {
                    "Inorganic"
                }
            }
        }
    }

    private fun registerLaunchers() {
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { handleImageUri(it) }
        }

        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && photoUri != null) {
                handleImageUri(photoUri!!)
            } else if (!success) {
                Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
            }
        }

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (!allGranted) {
                Toast.makeText(this, getString(R.string.permissions_required), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun pickImageFromGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun takePhotoFromCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, getString(R.string.camera_permission_required), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Create images directory in cache if it doesn't exist
            val imagesDir = File(cacheDir, "images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            val photoFile = File.createTempFile("photo_", ".jpg", imagesDir)
            photoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )
            photoUri?.let { uri ->
                takePictureLauncher.launch(uri)
            }
        } catch (e: IOException) {
            Toast.makeText(this, getString(R.string.error_creating_temp_file), Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error setting up camera: ${e.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
            e.printStackTrace()
        }
    }

    private fun handleImageUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (bitmap != null) {
                // Store the bitmap and URI for later classification
                currentBitmap = bitmap
                currentImageUri = uri
                
                // Display the image
                imagePreview.setImageBitmap(bitmap)
                
                // Show submit button and hide camera/gallery buttons
                showSubmitButton()
                
                // Reset result text
                textResult.text = getString(R.string.result_placeholder)
                textClassificationResult.visibility = android.view.View.GONE
            } else {
                Toast.makeText(this, getString(R.string.error_loading_image), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_loading_image), Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
    
    private fun showSubmitButton() {
        buttonSubmit.visibility = android.view.View.VISIBLE
        buttonCamera.visibility = android.view.View.GONE
        buttonPick.visibility = android.view.View.GONE
    }
    
    private fun hideSubmitButton() {
        buttonSubmit.visibility = android.view.View.GONE
        buttonCamera.visibility = android.view.View.VISIBLE
        buttonPick.visibility = android.view.View.VISIBLE
    }
    
    private fun submitImageForClassification(bitmap: android.graphics.Bitmap) {
        // Classify the image and store the data
        viewModel.classifyImage(bitmap, currentImageUri)
    }
    
    private fun testModelIntegration() {
        try {
            val classifier = WasteClassifier(this)
            val modelInfo = classifier.getModelInfo()
            val isLoaded = classifier.isModelLoaded()
            
            // Only log to console, no toast message
            println("Model Status: $modelInfo")
        } catch (e: Exception) {
            println("Model test failed: ${e.message}")
        }
    }
}