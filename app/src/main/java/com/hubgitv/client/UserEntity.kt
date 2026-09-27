package com.hubgitv.client

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_user")
data class UserEntity(
    @PrimaryKey val login: String,
    val avatarUrl: String,
    val name: String?,
    val bio: String?,
    val publicRepos: Int,
    val followers: Int,
    val following: Int,
    val lastUpdated: Long = System.currentTimeMillis() // Penanda waktu cache dibuat
)
