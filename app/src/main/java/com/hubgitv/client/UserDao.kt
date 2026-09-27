package com.hubgitv.client

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    // Ambil data profil lokal yang tersimpan
    @Query("SELECT * FROM cached_user LIMIT 1")
    suspend fun getCachedUser(): UserEntity?

    // Simpan atau perbarui data profil terbaru ke lokal
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    // Hapus data lokal (berguna saat user melakukan logout)
    @Query("DELETE FROM cached_user")
    suspend fun clearUser()
}
