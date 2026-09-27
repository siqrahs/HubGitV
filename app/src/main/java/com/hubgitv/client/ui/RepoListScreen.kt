package com.hubgitv.client.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hubgitv.client.core.RepoEntity

@Composable
fun RepoListScreen(viewModel: RepoViewModel, onRepoClick: (String) -> Unit) {
    val repos by viewModel.repoList
    val isLoading by viewModel.isLoading
    val error by viewModel.errorMessage

    // Ambil data otomatis saat halaman diakses
    LaunchedEffect(Unit) {
        viewModel.loadUserRepositories()
    }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (isLoading && repos.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Daftar Repositori", 
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(repos) { repo ->
                    Card(
                        onClick = { onRepoClick(repo.name) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = repo.name, style = MaterialTheme.typography.titleMedium)
                                
                                // Badge Status Publik / Privat
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = if (repo.isPrivate) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = if (repo.isPrivate) "Private" else "Public",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = repo.description ?: "Tidak ada deskripsi.", 
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "⭐ ${repo.stargazersCount}  •  🍴 ${repo.forksCount}  •  ${repo.language ?: "N/A"}", 
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}
