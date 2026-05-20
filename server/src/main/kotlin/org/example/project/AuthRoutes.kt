package org.example.project

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction
import org.example.project.db.UserEntity
import org.example.project.db.Users

@Serializable
data class AuthRequest(
    val username: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String
)

fun Route.authRoutes() {
    post("/auth/register") {
        val request = call.receive<AuthRequest>()

        val result = transaction {
            val existing = UserEntity.find { Users.username eq request.username }.firstOrNull()
            if (existing != null) {
                return@transaction Pair(false, "Username already exists")
            }

            val user = UserEntity.new {
                username = request.username
                passwordHash = AuthService.hashPassword(request.password)
                createdAt = System.currentTimeMillis()
            }

            val token = AuthService.createSession(user.id.value)
            Pair(true, token)
        }

        if (result.first) {
            call.respond(AuthResponse(result.second))
        } else {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.second))
        }
    }

    post("/auth/login") {
        val request = call.receive<AuthRequest>()

        val result = transaction {
            val user = UserEntity.find { Users.username eq request.username }.firstOrNull()
            if (user == null || !AuthService.verifyPassword(request.password, user.passwordHash)) {
                return@transaction Pair(false, "Invalid credentials")
            }

            val token = AuthService.createSession(user.id.value)
            Pair(true, token)
        }

        if (result.first) {
            call.respond(AuthResponse(result.second))
        } else {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to result.second))
        }
    }
}
