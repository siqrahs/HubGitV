package com.hubgitv.client

import retrofit2.http.GET // DITAMBAH
import retrofit2.http.Path // DITAMBAH


interface GitHubApiService {
    
 
    // Endpoint ini otomatis mengambil profil dari pemilik Token PAT yang sedang aktif
    @GET("user")
    suspend fun getAuthenticatedUser(): GitHubUser

    // ==========================================
    // UNTUK FASE BERIKUTNYA
    // ==========================================
    // Ambil detail profil user lain berdasarkan username
    @GET("users/{username}")
    suspend fun getUserProfile(
        @Path("username") username: String
    ): GitHubUser

    // Ambil daftar repositori milik user
    @GET("users/{username}/repos")
    suspend fun getUserRepos(
        @Path("username") username: String
    ): List<GitHubRepo>
}
