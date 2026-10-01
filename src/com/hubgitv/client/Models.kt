package com.hubgitv.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GhUser(
    val login: String = "",
    val name: String? = null,
    val avatarUrl: String = "",
    val bio: String? = null,
    val location: String? = null,
    val company: String? = null,
    val blog: String? = null,
    val publicRepos: Int = 0,
    val publicGists: Int = 0,
    val followers: Int = 0,
    val following: Int = 0,
    val createdAt: String? = null
)

@Serializable
data class GhRepo(
    val name: String = "",
    val fullName: String = "",
    val description: String? = null,
    val htmlUrl: String = "",
    val private: Boolean = false,
    val fork: Boolean = false,
    val language: String? = null,
    val stargazersCount: Int = 0,
    val forksCount: Int = 0,
    val openIssuesCount: Int = 0,
    val defaultBranch: String = "main",
    val updatedAt: String? = null,
    val pushedAt: String? = null,
    val archived: Boolean = false,
    val topics: List<String> = emptyList(),
    val owner: GhUser? = null
)

@Serializable
data class GhBranch(
    val name: String = "",
    @SerialName("protected") val isProtected: Boolean = false
)

@Serializable
data class GhContent(
    val name: String = "",
    val path: String = "",
    val type: String = "file",
    val size: Int = 0,
    val sha: String = "",
    val downloadUrl: String? = null,
    val encoding: String? = null,
    val content: String? = null
) {
    /** Isi file sudah di-base64 oleh GitHub API. */
    fun decodedText(): String? {
        val raw = content ?: return null
        val clean = raw.replace("\n", "").replace("\r", "")
        return runCatching {
            if (encoding == "base64") String(android.util.Base64.decode(clean, android.util.Base64.DEFAULT))
            else clean
        }.getOrNull()
    }
}

@Serializable
data class GhCommit(
    val sha: String = "",
    @SerialName("commit") val detail: GhCommitDetail? = null,
    val htmlUrl: String = ""
) {
    val message: String get() = detail?.message.orEmpty()
    val authorName: String? get() = detail?.author?.name
    val date: String? get() = detail?.author?.date
    val shortSha: String get() = sha.take(7)
}

@Serializable
data class GhCommitDetail(
    val message: String = "",
    val author: GhCommitAuthor? = null
)

@Serializable
data class GhCommitAuthor(val name: String? = null, val date: String? = null)

@Serializable
data class GhLabel(
    val name: String = "",
    val color: String = "cccccc"
) {
    fun rgb(): Int = runCatching { android.graphics.Color.parseColor("#$color") }
        .getOrDefault(0xFF8B949E.toInt())
}

@Serializable
data class GhIssue(
    val number: Int = 0,
    val title: String = "",
    val state: String = "open",
    val body: String? = null,
    val labels: List<GhLabel> = emptyList(),
    val htmlUrl: String = "",
    val comments: Int = 0,
    val user: GhUser? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    @SerialName("pull_request") val isPullRequest: Boolean = false
)

@Serializable
data class GhRelease(
    val tagName: String = "",
    val name: String? = null,
    val body: String? = null,
    val htmlUrl: String = "",
    val publishedAt: String? = null,
    val prerelease: Boolean = false,
    val draft: Boolean = false
)

@Serializable
data class GhNotification(
    val id: String = "",
    val unread: Boolean = true,
    val reason: String = "",
    val updatedAt: String? = null,
    val subject: NotificationSubject = NotificationSubject(),
    val repository: GhRepo? = null
)

@Serializable
data class NotificationSubject(
    val title: String = "",
    val type: String = "",
    val url: String = ""
)

@Serializable
data class GhReadme(val content: String = "", val encoding: String? = null, val path: String = "") {
    fun text(): String? = runCatching {
        if (encoding == "base64") String(android.util.Base64.decode(content.replace("\n", ""), android.util.Base64.DEFAULT))
        else content
    }.getOrNull()
}

@Serializable
data class GhViewer(
    val login: String = "",
    val avatarUrl: String = ""
)
