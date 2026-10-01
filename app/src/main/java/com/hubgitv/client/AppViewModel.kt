package com.hubgitv.client

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

data class Screen(val title: String, val kind: Kind, val arg: String = "", val arg2: String = "") {
    enum class Kind { DASHBOARD, REPOS, REPO, FILES, FILE, ISSUES, ISSUE, NEW_ISSUE, SEARCH, NOTIFICATIONS, RELEASES, COMMITS, PROFILE }
}

data class IssueState(
    val loading: Boolean = false,
    val items: List<GhIssue> = emptyList(),
    val error: String? = null,
    val title: String = "",
    val body: String = "",
    val detail: GhIssue? = null,
    val labels: List<GhLabel> = emptyList(),
    val applied: List<String> = emptyList(),
    val submitting: Boolean = false,
    val done: String? = null
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val store = TokenStore(app)
    private val api = GitHubApi(store)

    var loggedIn by mutableStateOf(store.token() != null)
        private set
    var me by mutableStateOf<GhUser?>(null)
        private set
    var loginBusy by mutableStateOf(false)
        private set
    var loginError by mutableStateOf<String?>(null)
        private set

    var stack by mutableStateOf(listOf(Screen("HubGitV", Screen.Kind.DASHBOARD)))
        private set
    val current: Screen get() = stack.last()

    var repos by mutableStateOf<List<GhRepo>>(emptyList())
        private set
    var reposLoading by mutableStateOf(false)
        private set
    var reposError by mutableStateOf<String?>(null)
        private set
    var repoQuery by mutableStateOf("")

    var repo by mutableStateOf<GhRepo?>(null)
        private set
    var repoLoading by mutableStateOf(false)
        private set
    var repoError by mutableStateOf<String?>(null)
        private set
    var starred by mutableStateOf<Boolean?>(null)
        private set
    var branches by mutableStateOf<List<GhBranch>>(emptyList())
        private set
    var commits by mutableStateOf<List<GhCommit>>(emptyList())
        private set
    var readme by mutableStateOf<String?>(null)
        private set

    var files by mutableStateOf<List<GhContent>>(emptyList())
        private set
    var filesPath by mutableStateOf("")
        private set
    var filesLoading by mutableStateOf(false)
        private set
    var filesError by mutableStateOf<String?>(null)
        private set
    var fileText by mutableStateOf<String?>(null)
        private set
    var fileName by mutableStateOf("")
        private set

    var issues by mutableStateOf(IssueState())
        private set
    var issueStateFilter by mutableStateOf("open")

    var notifications by mutableStateOf<List<GhNotification>>(emptyList())
        private set
    var notifLoading by mutableStateOf(false)
        private set
    var notifError by mutableStateOf<String?>(null)
        private set

    var releases by mutableStateOf<List<GhRelease>>(emptyList())
        private set

    var profile by mutableStateOf<GhUser?>(null)
        private set
    var profileRepos by mutableStateOf<List<GhRepo>>(emptyList())
        private set
    var profileLoading by mutableStateOf(false)
        private set

    var toast by mutableStateOf<String?>(null)

    // ---- auth ----

    fun login(token: String) {
        if (token.isBlank()) {
            loginError = "Token kosong."
            return
        }
        loginBusy = true
        loginError = null
        viewModelScope.launch {
            store.save(token.trim())
            runCatching { api.viewer() }
                .onSuccess { v ->
                    loggedIn = true
                    me = api.user(v.login)
                    loginBusy = false
                    loadRepos()
                }
                .onFailure {
                    store.clear()
                    loginBusy = false
                    loginError = (it as? ApiException)?.message ?: "Login gagal: ${it.message}"
                }
        }
    }

    fun logout() {
        store.clear()
        loggedIn = false
        me = null
        repos = emptyList()
        stack = listOf(Screen("HubGitV", Screen.Kind.DASHBOARD))
    }

    // ---- navigation ----

    fun go(s: Screen) {
        stack = stack + s
    }

    fun back(): Boolean {
        if (stack.size <= 1) return false
        stack = stack.dropLast(1)
        return true
    }

    fun say(msg: String) {
        toast = msg
    }

    /** Buka URL eksternal (issue/PR/commit) lewat browser. */
    fun openInBrowser(url: String) {
        val ctx = getApplication<Application>()
        runCatching {
            ctx.startActivity(
                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure { say("Tidak ada browser untuk membuka tautan") }
    }

    fun clearToast() {
        toast = null
    }

    // ---- repos ----

    fun loadRepos() {
        reposLoading = true
        reposError = null
        viewModelScope.launch {
            runCatching { api.myRepos() }
                .onSuccess { repos = it; reposLoading = false }
                .onFailure { reposError = (it as? ApiException)?.message ?: it.message; reposLoading = false }
        }
    }

    val filteredRepos: List<GhRepo>
        get() = if (repoQuery.isBlank()) repos else repos.filter {
            it.name.contains(repoQuery, true) || (it.description?.contains(repoQuery, true) == true)
        }

    fun openRepo(fullName: String) {
        val parts = fullName.split("/")
        if (parts.size != 2) {
            say("Nama repo tidak valid")
            return
        }
        go(Screen(parts[1], Screen.Kind.REPO, parts[0], parts[1]))
        repo = null
        repoError = null
        starred = null
        readme = null
        commits = emptyList()
        repoLoading = true
        viewModelScope.launch {
            runCatching { api.repo(parts[0], parts[1]) }
                .onSuccess { r ->
                    repo = r
                    repoLoading = false
                    loadStar(r.owner?.login.orEmpty(), r.name)
                    viewModelScope.launch {
                        runCatching { api.branches(parts[0], parts[1]) }.onSuccess { branches = it }
                        runCatching { api.commits(parts[0], parts[1], perPage = 20) }.onSuccess { commits = it }
                        runCatching { api.readme(parts[0], parts[1]) }.onSuccess { readme = it.text() }
                    }
                }
                .onFailure { repoError = (it as? ApiException)?.message ?: it.message; repoLoading = false }
        }
    }

    private fun loadStar(owner: String, name: String) {
        if (owner.isBlank()) return
        viewModelScope.launch {
            starred = runCatching { api.checkStar(owner, name) }.getOrDefault(false)
        }
    }

    fun toggleStar() {
        val r = repo ?: return
        val owner = r.owner?.login ?: return
        val next = !(starred ?: false)
        viewModelScope.launch {
            runCatching { api.setStar(owner, r.name, next) }
                .onSuccess {
                    starred = next
                    repos = repos.map { if (it.fullName == r.fullName) it.copy(stargazersCount = it.stargazersCount + if (next) 1 else -1) else it }
                    say(if (next) "Ditandai bintang" else "Bintang dilepas")
                }
                .onFailure { say("Gagal: ${it.message}") }
        }
    }

    fun loadCommits() {
        val r = repo ?: return
        viewModelScope.launch {
            commits = runCatching { api.commits(r.owner?.login.orEmpty(), r.name) }.getOrDefault(emptyList())
        }
    }

    fun openFiles() {
        val r = repo ?: return
        go(Screen("Files", Screen.Kind.FILES, r.owner?.login.orEmpty(), r.name))
        loadFiles("")
    }

    fun loadFiles(path: String, ref: String? = null) {
        val r = repo ?: return
        filesPath = path
        filesLoading = true
        filesError = null
        viewModelScope.launch {
            runCatching { api.contents(r.owner?.login.orEmpty(), r.name, path, ref) }
                .onSuccess { files = it.sortedWith(compareBy({ it.type != "dir" }, { it.name.lowercase() })); filesLoading = false }
                .onFailure { filesError = (it as? ApiException)?.message ?: it.message; filesLoading = false }
        }
    }

    fun openFile(entry: GhContent) {
        val r = repo ?: return
        val owner = r.owner?.login.orEmpty()
        go(Screen(entry.name, Screen.Kind.FILE, owner, r.name))
        fileName = entry.path
        fileText = null
        viewModelScope.launch {
            val one = runCatching { api.contents(owner, r.name, entry.path).firstOrNull() }.getOrNull()
            fileText = one?.decodedText() ?: "Isi file tidak bisa dibaca (terlalu besar atau biner)."
        }
    }

    // ---- issues ----

    fun openIssues() {
        val r = repo ?: return
        go(Screen("Issues", Screen.Kind.ISSUES, r.owner?.login.orEmpty(), r.name))
        loadIssues()
    }

    fun loadIssues() {
        val r = repo ?: return
        issues = issues.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { api.issues(r.owner?.login.orEmpty(), r.name, issueStateFilter) }
                .onSuccess { issues = issues.copy(loading = false, items = it) }
                .onFailure { issues = issues.copy(loading = false, error = (it as? ApiException)?.message ?: it.message) }
        }
    }

    fun setIssueFilter(state: String) {
        issueStateFilter = state
        loadIssues()
    }

    fun openNewIssue() {
        val r = repo ?: return
        issues = IssueState()
        go(Screen("New Issue", Screen.Kind.NEW_ISSUE, r.owner?.login.orEmpty(), r.name))
    }

    fun openIssue(number: Int) {
        val r = repo ?: return
        val owner = r.owner?.login.orEmpty()
        val found = issues.items.firstOrNull { it.number == number }
        go(Screen("#$number", Screen.Kind.ISSUE, owner, r.name))
        issues = issues.copy(detail = found, loading = true, labels = emptyList())
        viewModelScope.launch {
            runCatching { api.issues(owner, r.name, "all") }
                .onSuccess { all ->
                    val issue = all.firstOrNull { it.number == number }
                    issues = issues.copy(
                        loading = false,
                        detail = issue,
                        applied = issue?.labels?.map { it.name }.orEmpty()
                    )
                    viewModelScope.launch {
                        issues = issues.copy(labels = runCatching { api.labels(owner, r.name) }.getOrDefault(emptyList()))
                    }
                }
                .onFailure { issues = issues.copy(loading = false, error = it.message) }
        }
    }

    fun addComment(body: String) {
        val r = repo ?: return
        val detail = issues.detail ?: return
        issues = issues.copy(submitting = true, done = null)
        viewModelScope.launch {
            runCatching { api.comment(r.owner?.login.orEmpty(), r.name, detail.number, body) }
                .onSuccess { issues = issues.copy(submitting = false, done = "Komentar terkirim."); loadIssues() }
                .onFailure { issues = issues.copy(submitting = false, done = "Gagal: ${it.message}") }
        }
    }

    fun toggleLabel(name: String) {
        val r = repo ?: return
        val detail = issues.detail ?: return
        val active = issues.applied
        val target = if (active.contains(name)) active - name else active + name
        issues = issues.copy(submitting = true)
        viewModelScope.launch {
            runCatching { api.setLabels(r.owner?.login.orEmpty(), r.name, detail.number, target) }
                .onSuccess {
                    issues = issues.copy(submitting = false, applied = target, done = "Label diperbarui.")
                }
                .onFailure { issues = issues.copy(submitting = false, done = "Gagal: ${it.message}") }
        }
    }

    fun submitIssue(onDone: (Int) -> Unit) {
        val r = repo ?: return
        val state = issues
        if (state.title.isBlank()) {
            issues = state.copy(done = "Judul wajib diisi.")
            return
        }
        issues = state.copy(submitting = true, done = null)
        viewModelScope.launch {
            runCatching { api.createIssue(r.owner?.login.orEmpty(), r.name, state.title, state.body) }
                .onSuccess { created ->
                    issues = issues.copy(submitting = false, done = "Issue #${created.number} dibuat.")
                    back()
                    onDone(created.number)
                    loadIssues()
                }
                .onFailure { issues = issues.copy(submitting = false, done = (it as? ApiException)?.message ?: it.message) }
        }
    }

    fun editIssueField(title: String, body: String) {
        issues = issues.copy(title = title, body = body)
    }

    fun dismissIssueMessage() {
        issues = issues.copy(done = null)
    }

    // ---- others ----

    fun openNotifications() {
        go(Screen("Notifications", Screen.Kind.NOTIFICATIONS))
        notifLoading = true
        notifError = null
        viewModelScope.launch {
            runCatching { api.notifications() }
                .onSuccess { notifications = it; notifLoading = false }
                .onFailure { notifError = (it as? ApiException)?.message ?: it.message; notifLoading = false }
        }
    }

    fun openReleases() {
        val r = repo ?: return
        go(Screen("Releases", Screen.Kind.RELEASES, r.owner?.login.orEmpty(), r.name))
        viewModelScope.launch {
            releases = runCatching { api.releases(r.owner?.login.orEmpty(), r.name) }.getOrDefault(emptyList())
        }
    }

    fun openSearch() {
        go(Screen("Search issues", Screen.Kind.SEARCH))
    }

    fun searchIssues(query: String) {
        if (query.isBlank()) return
        issues = issues.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { api.searchIssues(query) }
                .onSuccess { issues = issues.copy(loading = false, items = it) }
                .onFailure { issues = issues.copy(loading = false, error = (it as? ApiException)?.message ?: it.message) }
        }
    }

    fun openProfile(login: String) {
        go(Screen(login, Screen.Kind.PROFILE, login, ""))
        profile = null
        profileRepos = emptyList()
        profileLoading = true
        viewModelScope.launch {
            runCatching { api.user(login) }
                .onSuccess { profile = it; profileLoading = false }
                .onFailure { profileLoading = false; say(it.message ?: "Gagal memuat profil") }
            profileRepos = runCatching { api.userRepos(login) }.getOrDefault(emptyList())
        }
    }
}
