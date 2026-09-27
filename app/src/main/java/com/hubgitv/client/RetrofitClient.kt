import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://github.com"

    fun getInstance(authManager: AuthManager): GitHubApiService {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
                .addHeader("Accept", "application/vnd.github+json")
            
            // Pasang token jika user sudah login
            authManager.getToken()?.let { token ->
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            
            chain.proceed(requestBuilder.build())
        }.build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GitHubApiService::class.java)
    }
}
