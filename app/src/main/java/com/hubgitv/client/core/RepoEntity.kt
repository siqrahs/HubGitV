package com.hubgitv.client.core

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_repos")
data class RepoEntity(
    @PrimaryKey val name: String,
    val description: String?,
    val stargazersCount: Int,
    val forksCount: Int,
    val language: String?,
    val isPrivate: Boolean,
    val lastUpdated: Long = System.currentTimeMillis()
)
