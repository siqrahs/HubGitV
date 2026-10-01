package com.hubgitv.client.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hubgitv.client.AppViewModel
import com.hubgitv.client.GhContent
import com.hubgitv.client.Screen

@Composable
fun FilesScreen(vm: AppViewModel, padding: PaddingValues) {
    val path = vm.filesPath
    Column(Modifier.fillMaxSize().padding(padding)) {
        if (path.isNotEmpty()) {
            TextButton(onClick = { vm.back() }) { Text("‹ $path") }
        }
        when {
            vm.filesLoading -> LoadingBox("Membaca direktori…")
            vm.filesError != null -> ErrorBox(vm.filesError!!) { vm.loadFiles(path) }
            vm.files.isEmpty() -> EmptyBox("Direktori kosong.")
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 12.dp)) {
                items(vm.files, key = { it.path }) { entry ->
                    FileRow(entry) {
                        if (entry.type == "dir") vm.loadFiles(entry.path) else vm.openFile(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun FileRow(entry: GhContent, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (entry.type == "dir") "[dir]" else "[${entry.size} B]",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(10.dp))
        Text(entry.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

@Composable
fun FileScreen(vm: AppViewModel, padding: PaddingValues) {
    val text = vm.fileText
    Column(Modifier.fillMaxSize().padding(padding)) {
        Text(
            vm.fileName,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(16.dp)
        )
        when {
            text == null -> LoadingBox("Mengunduh file…")
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
                item {
                    CodeBlock(text.take(20000))
                    if (text.length > 20000) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "... file dipotong pada 20000 karakter",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IssuesScreen(vm: AppViewModel, padding: PaddingValues) {
    val state = vm.issues
    Column(Modifier.fillMaxSize().padding(padding)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("open", "closed", "all").forEach { f ->
                FilterChip(
                    selected = vm.issueStateFilter == f,
                    onClick = { vm.setIssueFilter(f) },
                    label = { Text(f) }
                )
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = { vm.openNewIssue() }) { Text("Baru") }
        }
        when {
            state.loading -> LoadingBox("Mengambil issue…")
            state.error != null -> ErrorBox(state.error!!) { vm.loadIssues() }
            state.items.isEmpty() -> EmptyBox("Tidak ada issue ${vm.issueStateFilter}.")
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.items, key = { it.number }) { issue ->
                    IssueRow(issue) { vm.openIssue(issue.number) }
                }
            }
        }
    }
}

@Composable
fun IssueDetailScreen(vm: AppViewModel, padding: PaddingValues) {
    val state = vm.issues
    val issue = state.detail
    if (issue == null) {
        LoadingBox("Mengambil issue…")
        return
    }
    var comment by rememberSaveable(issue.number) { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("#${issue.number} ${issue.title}", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Chip(
                        issue.state,
                        if (issue.state == "open") androidx.compose.ui.graphics.Color(0xFF1A7F37) else androidx.compose.ui.graphics.Color(0xFF9A6700),
                        if (issue.state == "open") androidx.compose.ui.graphics.Color(0xFFD2F8D9) else androidx.compose.ui.graphics.Color(0xFFFFF3C4)
                    )
                    Spacer(Modifier.width(8.dp))
                    issue.user?.login?.let { Text("@$it", style = MaterialTheme.typography.labelMedium) }
                }
                if (!issue.body.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(issue.body!!, style = MaterialTheme.typography.bodyMedium)
                }
            }
            item { HorizontalDivider() }
            item {
                SectionLabel("Labels")
                Spacer(Modifier.height(8.dp))
                if (state.labels.isEmpty()) {
                    Text("Repo tidak punya label.", style = MaterialTheme.typography.bodySmall)
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.labels.forEach { label ->
                                val active = state.applied.contains(label.name)
                                LabelDot(label) { vm.toggleLabel(label.name) }
                                if (active) {
                                    // indicate active state
                                }
                            }
                        }
                    }
                }
                if (state.applied.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Aktif: ${state.applied.joinToString(", ")}", style = MaterialTheme.typography.labelSmall)
                }
            }
            item {
                SectionLabel("Tambah komentar")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    placeholder = { Text("Tulis komentar…") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { vm.addComment(comment); comment = "" },
                    enabled = comment.isNotBlank() && !state.submitting
                ) { Text(if (state.submitting) "Mengirim…" else "Kirim") }
            }
        }
        state.done?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun NewIssueScreen(vm: AppViewModel, padding: PaddingValues) {
    val state = vm.issues
    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
    ) {
        Text("Buat issue baru", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = state.title,
            onValueChange = { vm.editIssueField(it, state.body) },
            label = { Text("Judul") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.body,
            onValueChange = { vm.editIssueField(state.title, it) },
            label = { Text("Deskripsi (Markdown)") },
            minLines = 6,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { vm.submitIssue { } },
            enabled = !state.submitting,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (state.submitting) "Mengirim…" else "Kirim issue") }
        state.done?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun SearchScreen(vm: AppViewModel, padding: PaddingValues) {
    var query by rememberSaveable { mutableStateOf("is:issue is:open") }
    val state = vm.issues
    Column(Modifier.fillMaxSize().padding(padding)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Query") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { vm.searchIssues(query) }) { Text("Cari") }
        }
        when {
            state.loading -> LoadingBox("Mencari…")
            state.error != null -> ErrorBox(state.error!!) { vm.searchIssues(query) }
            state.items.isEmpty() -> EmptyBox("Belum ada hasil.")
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.items, key = { it.number }) { issue ->
                    IssueRow(issue) {
                        if (issue.htmlUrl.isNotBlank()) vm.openInBrowser(issue.htmlUrl)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(vm: AppViewModel, padding: PaddingValues) {
    when {
        vm.notifLoading -> LoadingBox("Mengambil notifikasi…")
        vm.notifError != null -> ErrorBox(vm.notifError!!) { vm.openNotifications() }
        vm.notifications.isEmpty() -> EmptyBox("Tidak ada notifikasi.")
        else -> LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(vm.notifications, key = { it.id }) { n ->
                androidx.compose.material3.Card(
                    onClick = { n.repository?.let { vm.openRepo(it.fullName) } },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Chip(
                                n.subject.type,
                                MaterialTheme.colorScheme.onPrimaryContainer,
                                MaterialTheme.colorScheme.primaryContainer
                            )
                            Spacer(Modifier.width(8.dp))
                            if (n.unread) Chip("baru", MaterialTheme.colorScheme.onTertiaryContainer, MaterialTheme.colorScheme.tertiaryContainer)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(n.subject.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${n.repository?.fullName.orEmpty()} • ${n.reason}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReleasesScreen(vm: AppViewModel, padding: PaddingValues) {
    if (vm.releases.isEmpty()) LoadingBox("Mengambil releases…")
    else LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(vm.releases, key = { it.tagName }) { ReleaseRow(it) }
    }
}

@Composable
fun ProfileScreen(vm: AppViewModel, padding: PaddingValues) {
    val p = vm.profile
    if (p == null) {
        LoadingBox("Memuat profil…")
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(padding)) {
        item { UserHeader(p) }
        item { HorizontalDivider() }
        item {
            Column(Modifier.padding(16.dp)) {
                SectionLabel("Repo publik")
                Spacer(Modifier.height(8.dp))
            }
        }
        items(vm.profileRepos, key = { it.fullName }) { r ->
            Box(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                RepoRow(r) { vm.openRepo(r.fullName) }
            }
        }
    }
}
