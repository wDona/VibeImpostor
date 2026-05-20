package org.example.project

import org.jetbrains.exposed.sql.transactions.transaction
import org.example.project.db.SessionEntity
import org.example.project.db.UserEntity
import org.example.project.db.Users
import org.example.project.db.Sessions
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

object AuthService {
    fun hashPassword(raw: String): String {
        val salt = ByteArray(16).apply {
            SecureRandom().nextBytes(this)
        }
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val hash = digest.digest(raw.toByteArray())

        val saltB64 = Base64.getEncoder().encodeToString(salt)
        val hashB64 = Base64.getEncoder().encodeToString(hash)
        return "$saltB64:$hashB64"
    }

    fun verifyPassword(raw: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 2) return false

        val salt = try {
            Base64.getDecoder().decode(parts[0])
        } catch (e: Exception) {
            return false
        }

        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val hash = digest.digest(raw.toByteArray())

        val hashB64 = Base64.getEncoder().encodeToString(hash)
        return hashB64 == parts[1]
    }

    fun createSession(userId: Int): String = transaction {
        val token = UUID.randomUUID().toString()
        val expiresAt = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)

        val user = UserEntity.findById(userId) ?: throw Exception("User not found")

        SessionEntity.new {
            this.token = token
            this.userId = user.id
            this.expiresAt = expiresAt
        }

        token
    }

    fun userIdForToken(token: String): Int? = transaction {
        val session = SessionEntity.find { Sessions.token eq token }.firstOrNull()
            ?: return@transaction null

        if (session.expiresAt < System.currentTimeMillis()) {
            session.delete()
            return@transaction null
        }

        session.userId.value
    }
}
