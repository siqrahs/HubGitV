package com.hubgitv.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun DashboardScreen(viewModel: DashboardViewModel, onNavigateToLogin: () -> Unit) {
    val user by viewModel.userData
    val isLoading by viewModel.isLoading
    val error by viewModel.errorMessage

    // Otomatis load data begitu halaman ini dibuka
    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        if (isLoading && user == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Dashboard Akun", 
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            user?.let { profile ->
                // Foto Profil (Avatar)
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Informasi Profil
                Text(text = profile.name ?: profile.login, style = MaterialTheme.typography.titleLarge)
                Text(text = "@${profile.login}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                
                profile.bio?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = it, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Baris Info Statistik Ringkas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${profile.publicRepos}", style = MaterialTheme.typography.titleMedium)
                        Text("Repos", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${profile.followers}", style = MaterialTheme.typography.titleMedium)
                        Text("Followers", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${profile.following}", style = MaterialTheme.typography.titleMedium)
                        Text("Following", style = MaterialTheme.typography.labelSmall)
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Tombol Log Out
                Button(
                    onClick = { viewModel.logout(onLogoutSuccess = onNavigateToLogin) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Keluar dari Akun")
                }
            }
        }
    }
}
