package org.example.project.db

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class UserEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserEntity>(Users)

    var username by Users.username
    var passwordHash by Users.passwordHash
    var createdAt by Users.createdAt
}

class SessionEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<SessionEntity>(Sessions)

    var token by Sessions.token
    var userId by Sessions.userId
    var expiresAt by Sessions.expiresAt
}

class WordPackEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<WordPackEntity>(WordPacks)

    var name by WordPacks.name
    var language by WordPacks.language
    var ownerUserId by WordPacks.ownerUserId
    var isBuiltIn by WordPacks.isBuiltIn
}

class CategoryEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CategoryEntity>(Categories)

    var packId by Categories.packId
    var name by Categories.name
}

class WordEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<WordEntity>(Words)

    var categoryId by Words.categoryId
    var text by Words.text
    var hints by Words.hints
}
