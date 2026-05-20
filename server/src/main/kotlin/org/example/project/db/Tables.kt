package org.example.project.db

import org.jetbrains.exposed.dao.id.IntIdTable

object Users : IntIdTable("users") {
    val username = varchar("username", 32).uniqueIndex()
    val passwordHash = varchar("password_hash", 100)
    val createdAt = long("created_at")
}

object Sessions : IntIdTable("sessions") {
    val token = varchar("token", 64).uniqueIndex()
    val userId = reference("user_id", Users)
    val expiresAt = long("expires_at")
}

object WordPacks : IntIdTable("word_packs") {
    val name = varchar("name", 64)
    val language = varchar("language", 8)
    val ownerUserId = reference("owner_user_id", Users).nullable()
    val isBuiltIn = bool("is_built_in").default(false)
}

object Categories : IntIdTable("categories") {
    val packId = reference("pack_id", WordPacks)
    val name = varchar("name", 64)
}

object Words : IntIdTable("words") {
    val categoryId = reference("category_id", Categories)
    val text = varchar("text", 64)
}
