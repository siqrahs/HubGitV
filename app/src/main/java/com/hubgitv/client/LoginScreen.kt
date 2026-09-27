package com.hubgitv.client

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(viewModel: LoginViewModel, onNavigateToDashboard: () -> Unit) {
    var tokenInput by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading
    val error by viewModel.errorMessage
    val isSuccess by viewModel.loginSuccess

    // Pindah halaman jika sukses login
    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            onNavigateToDashboard()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("HubGitV - Pro Client", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Masuk menggunakan Personal Access Token (PAT)", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = tokenInput,
                onValueChange = { tokenInput = it },
                label = { Text("GitHub PAT (classic / fine-grained)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { viewModel.loginWithPAT(tokenInput) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Verifikasi & Masuk")
                }
            }

            error?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
