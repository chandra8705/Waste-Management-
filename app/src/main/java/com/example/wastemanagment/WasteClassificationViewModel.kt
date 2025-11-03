package com.example.wastemanagment

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.wastemanagment.repository.UserRepository
import com.example.wastemanagment.repository.WasteRecordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WasteClassificationViewModel(application: Application) : AndroidViewModel(application) {

    private val wasteClassifier = WasteClassifier(application.applicationContext)
    private val userRepository = UserRepository(application.applicationContext)
    private val wasteRecordRepository = WasteRecordRepository(application.applicationContext)

    private val _predictionResult = MutableLiveData<WasteClassifier.Prediction>()
    val predictionResult = _predictionResult

    private val _disposalAdvice = MutableLiveData<WasteAdvisor.DisposalAdvice>()
    val disposalAdvice = _disposalAdvice
    
    private val _wasteHistory = MutableLiveData<List<com.example.wastemanagment.model.WasteRecord>>()
    val wasteHistory = _wasteHistory

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage = _errorMessage
    
    private var currentImageUri: Uri? = null

    fun classifyImage(bitmap: Bitmap, imageUri: Uri? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                currentImageUri = imageUri
                
                val prediction = withContext(Dispatchers.Default) {
                    wasteClassifier.classify(bitmap)
                }
                _predictionResult.value = prediction

                val advice = WasteAdvisor.getDisposalAdvice(prediction.label)
                _disposalAdvice.value = advice

                saveClassificationRecord(prediction, advice)

                loadWasteHistory()
            } catch (e: Exception) {
                _errorMessage.value = "Classification failed: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun saveClassificationRecord(prediction: WasteClassifier.Prediction, advice: WasteAdvisor.DisposalAdvice) {
        val currentUser = userRepository.getCurrentUser()
        wasteRecordRepository.createRecord(
            userId = currentUser.id,
            wasteType = prediction.label,
            confidence = prediction.confidence,
            imageUri = currentImageUri?.toString(),
            disposalAdvice = advice.advice
        )
    }
    
    fun loadWasteHistory() {
        viewModelScope.launch {
            try {
                val currentUser = userRepository.getCurrentUser()
                val records = wasteRecordRepository.getRecordsForUser(currentUser.id)
                _wasteHistory.value = records
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load history: ${e.localizedMessage}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        wasteClassifier.close()
    }
}