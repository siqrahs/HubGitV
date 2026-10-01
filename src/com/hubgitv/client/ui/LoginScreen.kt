package com.hubgitv.client.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.hubgitv.client.AppViewModel

/**
 * Layar login: PAT dulu. Field token jadi fokus utama, ada tempel dari
 * clipboard, toggle tampil/sembunyikan, dan link langsung ke halaman
 * pembuatan token GitHub.
 */
@Composable
fun LoginScreen(vm: AppViewModel) {
    var token by rememberSaveable { mutableStateOf("") }
    var revealed by rememberSaveable { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("HubGitV", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Klien GitHub. Tempel Personal Access Token untuk mulai.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Personal Access Token (PAT)") },
            placeholder = { Text("ghp_… / github_pat_…") },
            singleLine = true,
            enabled = !vm.loginBusy,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (token.isNotEmpty()) {
                        TextButton(onClick = { token = clipboard.getText()?.text.orEmpty() }) { Text("Tempel") }
                    }
                    TextButton(onClick = { revealed = !revealed }) {
                        Text(if (revealed) "Sembunyi" else "Lihat")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        val error = vm.loginError
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { vm.login(token) },
            enabled = token.isNotBlank() && !vm.loginBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (vm.loginBusy) "Memverifikasi token…" else "Masuk dengan PAT")
        }

        Spacer(Modifier.height(28.dp))
        HorizontalRule()
        Spacer(Modifier.height(16.dp))

        SectionLabel("Buat token")
        Spacer(Modifier.height(8.dp))
        Text(
            "GitHub → Settings → Developer settings → Personal access tokens → " +
                "Fine-grained / Generate new token.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = {
            vm.openInBrowser("https://github.com/settings/tokens")
        }) { Text("Buka halaman token") }

        Spacer(Modifier.height(12.dp))
        SectionLabel("Scope")
        Spacer(Modifier.height(8.dp))
        Text(
            "Minimal: repo (agar repo privat terlihat dan bisa ditulis) dan read:user " +
                "(profil). Issue butuh scope repo juga.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Token disimpan terenkripsi di perangkat lewat EncryptedSharedPreferences " +
                "(AES256-GCM) dan hanya dikirim ke api.github.com.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun DashboardScreen(vm: AppViewModel, contentPadding: androidx.compose.foundation.layout.PaddingValues) {
    val me = vm.me
    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
    ) {
        me?.let { UserHeader(it) }
        HorizontalRule()

        MenuCard("Repositori", "Daftar repo milikmu, sorted by update") {
            vm.go(com.hubgitv.client.Screen("Repositori", com.hubgitv.client.Screen.Kind.REPOS))
        }
        MenuCard("Search issues", "Cari issue di seluruh repo yang kamu punya akses") {
            vm.openSearch()
        }
        MenuCard("Notifications", "Notifikasi GitHub terbaru") { vm.openNotifications() }
        if (me != null) {
            MenuCard("Profil publik @${me.login}", "Bio, followers, repo publik") { vm.openProfile(me.login) }
        }
        HorizontalRule()

        Column(Modifier.padding(16.dp)) {
            OutlinedButton(
                onClick = { vm.logout() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Keluar / ganti token") }
        }
    }
}

@Composable
private fun HorizontalRule() {
    androidx.compose.material3.HorizontalDivider()
}

@Composable
private fun MenuCard(title: String, subtitle: String, onClick: () -> Unit) {
    androidx.compose.material3.Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("›", style = MaterialTheme.typography.headlineSmall)
        }
    }
}
