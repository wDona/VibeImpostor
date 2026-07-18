package org.example.project

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import kotlinx.serialization.Serializable
import org.example.project.db.WordRepository

@Serializable
data class CategoriesResponse(
    val categories: List<CategoryResponse>
)

@Serializable
data class CategoryResponse(
    val id: Int,
    val name: String,
    val language: String = "es"
)

@Serializable
data class ImportPackRequest(
    val json: String
)

@Serializable
data class ImportPackResponse(
    val packId: Int
)

@Serializable
data class PackDto(
    val id: Int,
    val name: String,
    val language: String
)

@Serializable
data class UserPacksResponse(
    val packs: List<PackDto>
)

fun Route.packRoutes() {
    get("/packs/my-packs") {
        val token = call.request.headers["Authorization"]
            ?.removePrefix("Bearer ")
            .orEmpty()

        val userId = if (token.isNotEmpty()) AuthService.userIdForToken(token) else null
        if (userId == null) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
            return@get
        }

        val packs = WordRepository.listUserPacks(userId)
        call.respond(UserPacksResponse(packs.map { PackDto(it.id, it.name, it.language) }))
    }

    get("/packs/categories") {
        val token = call.request.headers["Authorization"]
            ?.removePrefix("Bearer ")

        val userId = if (token != null) AuthService.userIdForToken(token) else null

        val categories = WordRepository.listCategories(userId)
        call.respond(CategoriesResponse(
            categories.map { CategoryResponse(it.id, it.name, it.language) }
        ))
    }

    post("/packs/import") {
        val token = call.request.headers["Authorization"]
            ?.removePrefix("Bearer ")
            .orEmpty()

        val userId = if (token.isNotEmpty()) AuthService.userIdForToken(token) else null
        if (userId == null) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
            return@post
        }

        val request = call.receive<ImportPackRequest>()

        try {
            val packId = WordRepository.importPack(userId, request.json)
            call.respond(ImportPackResponse(packId))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid pack format: ${e.message}"))
        }
    }

    get("/packs/{id}") {
        val token = call.request.headers["Authorization"]
            ?.removePrefix("Bearer ")
            .orEmpty()

        val userId = if (token.isNotEmpty()) AuthService.userIdForToken(token) else null
        if (userId == null) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
            return@get
        }

        val packId = call.parameters["id"]?.toIntOrNull()
        if (packId == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid pack id"))
            return@get
        }

        val content = WordRepository.getPackContent(userId, packId)
        if (content == null) {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot read pack"))
        } else {
            call.respond(content)
        }
    }

    put("/packs/{id}") {
        val token = call.request.headers["Authorization"]
            ?.removePrefix("Bearer ")
            .orEmpty()

        val userId = if (token.isNotEmpty()) AuthService.userIdForToken(token) else null
        if (userId == null) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
            return@put
        }

        val packId = call.parameters["id"]?.toIntOrNull()
        if (packId == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid pack id"))
            return@put
        }

        val request = call.receive<ImportPackRequest>()

        try {
            if (WordRepository.replacePackContent(userId, packId, request.json)) {
                call.respond(mapOf("success" to true))
            } else {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot edit pack"))
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid pack format: ${e.message}"))
        }
    }

    delete("/packs/{id}") {
        val token = call.request.headers["Authorization"]
            ?.removePrefix("Bearer ")
            .orEmpty()

        val userId = if (token.isNotEmpty()) AuthService.userIdForToken(token) else null
        if (userId == null) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
            return@delete
        }

        val packId = call.parameters["id"]?.toIntOrNull()
        if (packId == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid pack id"))
            return@delete
        }

        val deleted = WordRepository.deletePack(userId, packId)
        if (deleted) {
            call.respond(mapOf("success" to true))
        } else {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Cannot delete pack"))
        }
    }
}
