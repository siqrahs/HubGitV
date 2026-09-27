package com.hubgitv.client

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val authManager = AuthManager(application)
    private val db = AppDatabase.getDatabase(application)
    private val apiService = RetrofitClient.getInstance(authManager)

    var userData = mutableStateOf<UserEntity?>(null)
    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    fun loadUserProfile() {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null

            // 1. Ambil data dari cache lokal dulu agar UI langsung muncul tanpa nunggu internet
            val localUser = db.userDao().getCachedUser()
            if (localUser != null) {
                userData.value = localUser
                // Jika data lokal masih baru (kurang dari 5 menit), tidak perlu hit API lagi untuk hemat rate limit
                if (System.currentTimeMillis() - localUser.lastUpdated < 5 * 60 * 1000) {
                    isLoading.value = false
                    return@launch
                }
            }

            // 2. Jika cache kosong atau sudah usang, ambil data terbaru dari GitHub API
            try {
                val remoteUser = apiService.getAuthenticatedUser()
                
                // Konversi dari model API ke model Database Lokal (Entity)
                val newUserEntity = UserEntity(
                    login = remoteUser.login,
                    avatarUrl = remoteUser.avatar_url,
                    name = remoteUser.name,
                    bio = remoteUser.bio,
                    publicRepos = remoteUser.public_repos,
                    followers = remoteUser.followers,
                    following = remoteUser.following
                )

                // 3. Simpan data terbaru ke database lokal untuk cache berikutnya
                db.userDao().insertUser(newUserEntity)
                
                // Perbarui tampilan UI
                userData.value = newUserEntity
            } catch (e: Exception) {
                if (userData.value == null) {
                    errorMessage.value = "Gagal memuat data dari server."
                }
            } finally {
                isLoading.value = false
            }
        }
    }

    fun logout(onLogoutSuccess: () -> Unit) {
        viewModelScope.launch {
            authManager.clearAuth()
            db.userDao().clearUser()
            onLogoutSuccess()
        }
    }
}
