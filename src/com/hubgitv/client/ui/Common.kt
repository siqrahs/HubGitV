package com.hubgitv.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hubgitv.client.GhContent
import com.hubgitv.client.GhIssue
import com.hubgitv.client.GhLabel
import com.hubgitv.client.GhRelease
import com.hubgitv.client.GhRepo
import com.hubgitv.client.GhUser

@Composable
fun Chip(text: String, fg: Color, bg: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun LoadingBox(label: String = "Memuat…") {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ErrorBox(message: String, retry: (() -> Unit)? = null) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Gagal memuat", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (retry != null) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = retry) { Text("Coba lagi") }
            }
        }
    }
}

@Composable
fun EmptyBox(message: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RepoRow(repo: GhRepo, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    repo.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Chip(
                    if (repo.private) "Private" else "Public",
                    if (repo.private) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                    if (repo.private) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                )
            }
            if (!repo.description.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    repo.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("* ${repo.stargazersCount}", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(12.dp))
                Text("fork ${repo.forksCount}", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(12.dp))
                Text("issue ${repo.openIssuesCount}", style = MaterialTheme.typography.labelMedium)
                if (!repo.language.isNullOrBlank()) {
                    Spacer(Modifier.width(12.dp))
                    Text(repo.language, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (repo.topics.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repo.topics.take(6).forEach { t ->
                        Chip(t, MaterialTheme.colorScheme.onSecondaryContainer, MaterialTheme.colorScheme.secondaryContainer)
                    }
                }
            }
        }
    }
}

@Composable
fun IssueRow(issue: GhIssue, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (issue.isPullRequest) {
                    Chip("PR", MaterialTheme.colorScheme.onTertiaryContainer, MaterialTheme.colorScheme.tertiaryContainer)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    "#${issue.number} ${issue.title}",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Chip(
                    issue.state,
                    if (issue.state == "open") Color(0xFF1A7F37) else Color(0xFF9A6700),
                    if (issue.state == "open") Color(0xFFD2F8D9) else Color(0xFFFFF3C4)
                )
                if (issue.comments > 0) {
                    Spacer(Modifier.width(8.dp))
                    Text("${issue.comments} komentar", style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.weight(1f))
                issue.user?.login?.let {
                    Text("@$it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (issue.labels.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    issue.labels.forEach { LabelDot(it) }
                }
            }
        }
    }
}

@Composable
fun LabelDot(label: GhLabel, onClick: (() -> Unit)? = null) {
    val bg = Color(label.rgb())
    val fg = if (label.rgb().let { (it shr 18 and 0xff) * 0.299 + (it shr 8 and 0xff) * 0.587 + (it and 0xff) * 0.114 } > 150) Color.Black else Color.White
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    ) {
        Text(
            label.name,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun ReleaseRow(release: GhRelease) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    release.tagName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (release.draft) Chip("draft", Color(0xFF9A6700), Color(0xFFFFF3C4))
                if (release.prerelease) Chip("pre", Color(0xFF0550AE), Color(0xFFDDF4FF))
            }
            if (!release.body.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(release.body, style = MaterialTheme.typography.bodySmall, maxLines = 6, overflow = TextOverflow.Ellipsis)
            }
            if (!release.publishedAt.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(release.publishedAt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun UserHeader(user: GhUser) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(user.login, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (!user.name.isNullOrBlank()) {
            Text(user.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!user.bio.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(user.bio, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(10.dp))
        Row {
            Text("repo ${user.publicRepos}", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(14.dp))
            Text("followers ${user.followers}", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(14.dp))
            Text("following ${user.following}", style = MaterialTheme.typography.labelMedium)
        }
        val loc = listOfNotNull(user.location?.takeIf { it.isNotBlank() }, user.company?.takeIf { it.isNotBlank() })
        if (loc.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(loc.joinToString(" • "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CodeBlock(text: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun AppScaffold(
    title: String,
    snackbar: SnackbarHostState,
    onBack: (() -> Unit)?,
    actions: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (onBack != null) {
                        TextButton(onClick = onBack) { Text("‹", style = MaterialTheme.typography.headlineMedium) }
                    }
                },
                actions = { actions() }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        content(padding)
    }
}
