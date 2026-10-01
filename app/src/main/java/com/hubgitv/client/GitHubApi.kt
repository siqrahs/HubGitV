package com.hubgitv.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ApiException(val code: Int, message: String) : Exception(message)

/**
 * Klien REST GitHub minimalist di atas HttpURLConnection supaya APK tidak perlu
 * OkHttp/Retrofit. Semua request jalan di Dispatchers.IO.
 */
class GitHubApi(private val store: TokenStore) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    suspend fun viewer(): GhViewer = call("GET", "user").let {
        json.decodeFromString<GhViewer>(it)
    }

    suspend fun user(login: String): GhUser =
        json.decodeFromString(call("GET", "users/${enc(login)}"))

    suspend fun myRepos(page: Int = 1, perPage: Int = 50): List<GhRepo> =
        json.decodeFromString(call("GET", "user/repos?per_page=$perPage&sort=updated&page=$page&affiliation=owner,collaborator,organization_member"))

    suspend fun userRepos(login: String): List<GhRepo> =
        json.decodeFromString(call("GET", "users/${enc(login)}/repos?per_page=100&sort=updated"))

    suspend fun repo(owner: String, name: String): GhRepo =
        json.decodeFromString(call("GET", "repos/${enc(owner)}/${enc(name)}"))

    suspend fun branches(owner: String, name: String): List<GhBranch> =
        json.decodeFromString(call("GET", "repos/${enc(owner)}/${enc(name)}/branches?per_page=100"))

    suspend fun commits(owner: String, name: String, branch: String? = null, perPage: Int = 30): List<GhCommit> {
        val q = buildString {
            append("repos/${enc(owner)}/${enc(name)}/commits?per_page=$perPage")
            if (!branch.isNullOrBlank()) append("&sha=${enc(branch)}")
        }
        return json.decodeFromString(call("GET", q))
    }

    suspend fun contents(owner: String, name: String, path: String, ref: String? = null): List<GhContent> {
        val q = buildString {
            append("repos/${enc(owner)}/${enc(name)}/contents/${encPath(path)}")
            if (!ref.isNullOrBlank()) append("?ref=${enc(ref)}")
        }
        return json.decodeFromString(call("GET", q))
    }

    suspend fun readme(owner: String, name: String, ref: String? = null): GhReadme {
        val q = buildString {
            append("repos/${enc(owner)}/${enc(name)}/readme")
            if (!ref.isNullOrBlank()) append("?ref=${enc(ref)}")
        }
        return json.decodeFromString(call("GET", q))
    }

    suspend fun issues(owner: String, name: String, state: String = "open"): List<GhIssue> =
        json.decodeFromString(call("GET", "repos/${enc(owner)}/${enc(name)}/issues?state=$state&per_page=100"))

    suspend fun createIssue(owner: String, name: String, title: String, body: String): GhIssue =
        json.decodeFromString(
            call("POST", "repos/${enc(owner)}/${enc(name)}/issues", """{"title":${q(title)},"body":${q(body)}}""")
        )

    suspend fun comment(owner: String, name: String, number: Int, body: String): GhIssue =
        json.decodeFromString(
            call("POST", "repos/${enc(owner)}/${enc(name)}/issues/$number/comments", """{"body":${q(body)}}""")
        )

    suspend fun labels(owner: String, name: String): List<GhLabel> =
        json.decodeFromString(call("GET", "repos/${enc(owner)}/${enc(name)}/labels?per_page=100"))

    suspend fun setLabels(owner: String, name: String, issueNumber: Int, labels: List<String>): List<GhLabel> {
        val arr = labels.joinToString(",") { q(it) }
        return json.decodeFromString(
            call("PUT", "repos/${enc(owner)}/${enc(name)}/issues/$issueNumber/labels", "[$arr]")
        )
    }

    suspend fun releases(owner: String, name: String): List<GhRelease> =
        json.decodeFromString(call("GET", "repos/${enc(owner)}/${enc(name)}/releases?per_page=30"))

    suspend fun notifications(): List<GhNotification> =
        json.decodeFromString(call("GET", "notifications?per_page=50"))

    suspend fun checkStar(owner: String, name: String): Boolean {
        val code = rawCode("GET", "user/starred/${enc(owner)}/${enc(name)}")
        return code == 204
    }

    suspend fun setStar(owner: String, name: String, starred: Boolean) {
        if (starred) call("PUT", "user/starred/${enc(owner)}/${enc(name)}", expectEmpty = true)
        else call("DELETE", "user/starred/${enc(owner)}/${enc(name)}", expectEmpty = true)
    }

    /** Issues saja; PR di-filter di sisi client. */
    suspend fun searchIssues(query: String): List<GhIssue> {
        val body = call("GET", "search/issues?q=${enc(query)}&per_page=50&sort=updated")
        val obj = json.parseToJsonElement(body).let { it as kotlinx.serialization.json.JsonObject }
        val items = obj["items"]?.let { json.decodeFromJsonElement<List<GhIssue>>(it) } ?: emptyList()
        return items.filter { !it.isPullRequest }
    }

    // ---- HTTP plumbing ----

    private suspend fun call(
        method: String,
        path: String,
        body: String? = null,
        expectEmpty: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val conn = open(method, path, body)
        conn.connect()
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
                throw ApiException(code, friendly(code, err))
            }
            if (expectEmpty) return@withContext ""
            conn.inputStream.bufferedReader().use(BufferedReader::readText)
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun rawCode(method: String, path: String): Int = withContext(Dispatchers.IO) {
        val conn = open(method, path, null)
        conn.connect()
        try {
            conn.responseCode
        } finally {
            conn.disconnect()
        }
    }

    private fun open(method: String, path: String, body: String?): HttpURLConnection {
        val conn = URL("$BASE/$path").openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 15_000
        conn.readTimeout = 20_000
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        conn.setRequestProperty("User-Agent", "HubGitV-Android")
        store.token()?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
        if (body != null) {
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }
        return conn
    }

    private fun friendly(code: Int, raw: String): String {
        val message = runCatching {
            val obj = json.parseToJsonElement(raw) as kotlinx.serialization.json.JsonObject
            (obj["message"] as? kotlinx.serialization.json.JsonPrimitive)?.content
        }.getOrNull()
        return when (code) {
            401 -> "Token ditolak. Periksa PAT kamu."
            403 -> message?.takeIf { it.isNotBlank() } ?: "Akses ditolak / rate limit habis."
            404 -> "Tidak ditemukan (404). Cek owner, repo, atau branch."
            422 -> message?.takeIf { it.isNotBlank() } ?: "Permintaan tidak valid (422)."
            else -> message?.takeIf { it.isNotBlank() } ?: "HTTP $code"
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun encPath(p: String) = p.split('/').filter { it.isNotEmpty() }.joinToString("/") { enc(it) }

    private fun q(s: String) = json.encodeToString(kotlinx.serialization.builtins.serializer(), s)

    private companion object {
        const val BASE = "https://api.github.com"
    }
}
