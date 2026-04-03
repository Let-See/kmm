package nl.codeface.letsee_kmm.android

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class CatApiService(private val client: OkHttpClient) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getBreeds(): List<Breed> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.thecatapi.com/v1/breeds")
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: "[]"
        json.decodeFromString<List<Breed>>(body)
    }
}
