package org.example.project.net

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import org.example.project.protocol.ProtocolJson

@Serializable
data class AuthRequest(val username: String, val password: String)

@Serializable
data class AuthResponse(val token: String)

@Serializable
data class CategoryResponse(val id: Int, val name: String)

@Serializable
data class CategoriesResponse(val categories: List<CategoryResponse>)

@Serializable
data class ImportPackRequest(val json: String)

@Serializable
data class ImportPackResponse(val packId: Int)

@Serializable
data class NewPackCategory(val name: String, val words: List<String>)

@Serializable
data class NewPack(val name: String, val language: String, val categories: List<NewPackCategory>)

class ApiClient(private val baseUrl: String) {
    private val client = HttpClient()
    var authToken: String? = null

    suspend fun register(username: String, password: String): String? = try {
        val reqJson = ProtocolJson.json.encodeToString(AuthRequest(username, password))
        val resText = client.post("$baseUrl/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(reqJson)
        }.body<String>()
        val response = ProtocolJson.json.decodeFromString<AuthResponse>(resText)
        authToken = response.token
        response.token
    } catch (e: Exception) {
        null
    }

    suspend fun login(username: String, password: String): String? = try {
        val reqJson = ProtocolJson.json.encodeToString(AuthRequest(username, password))
        val resText = client.post("$baseUrl/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(reqJson)
        }.body<String>()
        val response = ProtocolJson.json.decodeFromString<AuthResponse>(resText)
        authToken = response.token
        response.token
    } catch (e: Exception) {
        null
    }

    suspend fun getCategories(): List<CategoryResponse> = try {
        val resText = client.get("$baseUrl/packs/categories") {
            if (authToken != null) header("Authorization", "Bearer $authToken")
        }.body<String>()
        val response = ProtocolJson.json.decodeFromString<CategoriesResponse>(resText)
        response.categories
    } catch (e: Exception) {
        emptyList()
    }

    suspend fun importPack(json: String): Int? = try {
        if (authToken == null) return null
        val reqJson = ProtocolJson.json.encodeToString(ImportPackRequest(json))
        val resText = client.post("$baseUrl/packs/import") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $authToken")
            setBody(reqJson)
        }.body<String>()
        val response = ProtocolJson.json.decodeFromString<ImportPackResponse>(resText)
        response.packId
    } catch (e: Exception) {
        null
    }

    suspend fun deletePack(packId: Int): Boolean = try {
        if (authToken == null) return false
        client.delete("$baseUrl/packs/$packId") {
            header("Authorization", "Bearer $authToken")
        }
        true
    } catch (e: Exception) {
        false
    }

    suspend fun createCategoryPack(name: String, language: String, words: List<String>): Int? {
        val pack = NewPack(name, language, listOf(NewPackCategory(name, words)))
        val json = ProtocolJson.json.encodeToString(NewPack.serializer(), pack)
        return importPack(json)
    }

    fun logout() {
        authToken = null
    }
}
