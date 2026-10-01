package com.hubgitv.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.hubgitv.client.ui.AppScaffold
import com.hubgitv.client.ui.CommitsScreen
import com.hubgitv.client.ui.DashboardScreen
import com.hubgitv.client.ui.FileScreen
import com.hubgitv.client.ui.FilesScreen
import com.hubgitv.client.ui.IssueDetailScreen
import com.hubgitv.client.ui.IssuesScreen
import com.hubgitv.client.ui.LoginScreen
import com.hubgitv.client.ui.NewIssueScreen
import com.hubgitv.client.ui.NotificationsScreen
import com.hubgitv.client.ui.ProfileScreen
import com.hubgitv.client.ui.ReleasesScreen
import com.hubgitv.client.ui.RepoDetailScreen
import com.hubgitv.client.ui.RepoListScreen
import com.hubgitv.client.ui.SearchScreen
import androidx.compose.material3.SnackbarHostState

private val Dark = darkColorScheme(
    primary = Color(0xFF58A6FF),
    secondary = Color(0xFF7EE787),
    tertiary = Color(0xFFD29922),
    background = Color(0xFF0D1117),
    surface = Color(0xFF0D1117)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = Dark) {
                Surface(Modifier.fillMaxSize()) {
                    Root()
                }
            }
        }
    }
}

@Composable
fun Root(vm: AppViewModel = viewModel()) {
    if (!vm.loggedIn) {
        LoginScreen(vm)
        return
    }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val toast = vm.toast
    LaunchedEffect(toast) {
        if (toast != null) {
            scope.launch { snackbar.showSnackbar(toast) }
            vm.clearToast()
        }
    }

    val screen = vm.current
    AppScaffold(
        title = screen.title,
        snackbar = snackbar,
        onBack = if (vm.stack.size > 1) ({ vm.back() }) else null,
        actions = {
            if (screen.kind == Screen.Kind.REPOS) {
                androidx.compose.material3.TextButton(onClick = { vm.loadRepos() }) { Text2("Refresh") }
            }
            if (screen.kind == Screen.Kind.DASHBOARD) {
                androidx.compose.material3.TextButton(onClick = { vm.loadRepos() }) { Text2("Refresh") }
            }
        }
    ) { padding ->
        when (screen.kind) {
            Screen.Kind.DASHBOARD -> DashboardScreen(vm, padding)
            Screen.Kind.REPOS -> RepoListScreen(vm, padding)
            Screen.Kind.REPO -> RepoDetailScreen(vm, padding)
            Screen.Kind.FILES -> FilesScreen(vm, padding)
            Screen.Kind.FILE -> FileScreen(vm, padding)
            Screen.Kind.ISSUES -> IssuesScreen(vm, padding)
            Screen.Kind.ISSUE -> IssueDetailScreen(vm, padding)
            Screen.Kind.NEW_ISSUE -> NewIssueScreen(vm, padding)
            Screen.Kind.SEARCH -> SearchScreen(vm, padding)
            Screen.Kind.NOTIFICATIONS -> NotificationsScreen(vm, padding)
            Screen.Kind.RELEASES -> ReleasesScreen(vm, padding)
            Screen.Kind.COMMITS -> CommitsScreen(vm, padding)
            Screen.Kind.PROFILE -> ProfileScreen(vm, padding)
        }
    }
}

@Composable
private fun Text2(s: String) {
    androidx.compose.material3.Text(s, style = MaterialTheme.typography.labelLarge)
}
