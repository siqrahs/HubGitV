package com.hubgitv.client.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hubgitv.client.AppViewModel
import com.hubgitv.client.Screen

@Composable
fun RepoListScreen(vm: AppViewModel, padding: PaddingValues) {
    val repos = vm.filteredRepos
    val error = vm.reposError

    Column(Modifier.fillMaxSize().padding(padding)) {
        OutlinedTextField(
            value = vm.repoQuery,
            onValueChange = { vm.repoQuery = it },
            label = { Text("Filter repo") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
        )
        when {
            vm.reposLoading && repos.isEmpty() -> LoadingBox("Mengambil repo…")
            error != null && repos.isEmpty() -> ErrorBox(error) { vm.loadRepos() }
            repos.isEmpty() -> EmptyBox("Tidak ada repo yang cocok.")
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(repos, key = { it.fullName }) { r ->
                    RepoRow(r) { vm.openRepo(r.fullName) }
                }
            }
        }
    }
}

@Composable
fun RepoDetailScreen(vm: AppViewModel, padding: PaddingValues) {
    val r = vm.repo
    val error = vm.repoError

    when {
        vm.repoLoading && r == null -> LoadingBox("Memuat repo…")
        error != null && r == null -> ErrorBox(error)
        r == null -> EmptyBox("Repo tidak tersedia.")
        else -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Column(Modifier.padding(16.dp)) {
                    Text(r.fullName, style = MaterialTheme.typography.titleLarge)
                    if (!r.description.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(r.description, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(
                            if (r.private) "Private" else "Public",
                            if (r.private) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                            if (r.private) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                        )
                        if (r.fork) Chip("Fork", MaterialTheme.colorScheme.onSecondaryContainer, MaterialTheme.colorScheme.secondaryContainer)
                        if (r.archived) Chip("Archived", Color0x("9A6700"), Color0x("FFF3C4"))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("★ ${r.stargazersCount}    fork ${r.forksCount}    issue ${r.openIssuesCount}", style = MaterialTheme.typography.bodyMedium)
                    if (!r.language.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Bahasa: ${r.language}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Branch default: ${r.defaultBranch}", style = MaterialTheme.typography.bodySmall)
                    r.pushedAt?.let {
                        Spacer(Modifier.height(2.dp))
                        Text("Push terakhir: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(16.dp))
                    ActionRow(vm, "★ Bintang (${if (vm.starred == true) "aktif" else "tidak"})") { vm.toggleStar() }
                    ActionRow(vm, "Kode / Files") { vm.openFiles() }
                    ActionRow(vm, "Issues") { vm.openIssues() }
                    ActionRow(vm, "Commits") {
                        vm.go(Screen("Commits", Screen.Kind.COMMITS, r.owner?.login.orEmpty(), r.name))
                    }
                    ActionRow(vm, "Releases") { vm.openReleases() }
                    if (r.topics.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        SectionLabel("Topics")
                        Spacer(Modifier.height(6.dp))
                        Text(r.topics.joinToString(" • "), style = MaterialTheme.typography.bodySmall)
                    }
                    if (vm.branches.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        SectionLabel("Branches (${vm.branches.size})")
                        Spacer(Modifier.height(6.dp))
                        Text(
                            vm.branches.take(20).joinToString(", ") { it.name },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    val md = vm.readme
                    if (!md.isNullOrBlank()) {
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))
                        SectionLabel("README")
                        Spacer(Modifier.height(8.dp))
                        Text(md.take(2000), style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ActionRow(vm: AppViewModel, label: String, onClick: () -> Unit) {
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text("›", style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun Color0x(hex: String) = androidx.compose.ui.graphics.Color(0xFF000000.toInt() or hex.toLong(16))

@Composable
fun CommitsScreen(vm: AppViewModel, padding: PaddingValues) {
    if (vm.commits.isEmpty()) {
        LoadingBox("Mengambil commit…")
    } else {
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(12.dp)) {
            items(vm.commits, key = { it.sha }) { c ->
                androidx.compose.material3.Card(
                    onClick = { vm.say(c.htmlUrl) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row {
                            Text(c.shortSha, style = MaterialTheme.typography.labelMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            c.date?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(c.message, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                        c.authorName?.let {
                            Spacer(Modifier.height(2.dp))
                            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
