package com.hubgitv.client.ui

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hubgitv.client.core.AppDatabase
import com.hubgitv.client.core.AuthManager
import com.hubgitv.client.core.RepoEntity
import com.hubgitv.client.core.RetrofitClient
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp


class RepoViewModel(application: Application) : AndroidViewModel(application) {
    private val authManager = AuthManager(application)
    private val db = AppDatabase.getDatabase(application)
    private val apiService = RetrofitClient.getInstance(authManager)

    var repoList = mutableStateOf<List<RepoEntity>>(emptyList())
    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    fun loadUserRepositories() {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null

            // 1. Ambal dari cache lokal dulu agar UI langsung tampil tanpa loading lama
            val localRepos = db.repoDao().getCachedRepos()
            if (localRepos.isNotEmpty()) {
                repoList.value = localRepos
                
                // Jika cache masih segar (kurang dari 10 menit), batalkan request ke internet demi hemat rate limit
                val newestRepo = localRepos.firstOrNull()
                if (newestRepo != null && System.currentTimeMillis() - newestRepo.lastUpdated < 10 * 60 * 1000) {
                    isLoading.value = false
                    return@launch
                }
            }

            // 2. Ambil data terbaru dari GitHub API
            try {
                val remoteRepos = apiService.getAuthenticatedUserRepos()
                
                // Konversi data API ke model database lokal Entity
                val newRepoEntities = remoteRepos.map { repo ->
                    RepoEntity(
                        name = repo.name,
                        description = repo.description,
                        stargazersCount = repo.stargazers_count,
                        forksCount = repo.forks_count,
                        language = repo.language,
                        isPrivate = repo.isPrivate
                    )
                }

                // 3. Update database lokal dan perbarui layar
                db.repoDao().clearRepos()
                db.repoDao().insertRepos(newRepoEntities)
                repoList.value = newRepoEntities
            } catch (e: Exception) {
                if (repoList.value.isEmpty()) {
                    errorMessage.value = "Gagal memuat repositori dari server."
                }
            } finally {
                isLoading.value = false
            }
        }
    }
}
