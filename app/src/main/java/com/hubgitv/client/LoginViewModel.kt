package com.hubgitv.client

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val authManager = AuthManager(application)
    
    var isLoading = mutableStateOf(false)
    var loginSuccess = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    init {
        // Cek jika token sudah ada, langsung lolos login
        if (!authManager.getToken().isNullOrBlank()) {
            loginSuccess.value = true
        }
    }

    fun loginWithPAT(token: String) {
        if (token.isBlank()) {
            errorMessage.value = "Token tidak boleh kosong!"
            return
        }

        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            try {
                // Simpan sementara untuk tes hit ke API internal GitHub milik user sendiri
                authManager.saveToken(token)
                
                // Panggil endpoint profil autentikasi diri sendiri
                val apiService = RetrofitClient.getInstance(authManager)
                val userProfile = apiService.getAuthenticatedUser() // Endpoint: @GET("user")
                
                loginSuccess.value = true
            } catch (e: Exception) {
                authManager.clearAuth() // Hapus kembali jika token salah
                errorMessage.value = "Token tidak valid atau masalah jaringan."
            } finally {
                isLoading.value = false
            }
        }
    }
}
