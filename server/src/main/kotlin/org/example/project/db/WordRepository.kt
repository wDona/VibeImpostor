package org.example.project.db

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

data class CategoryDto(
    val id: Int,
    val name: String,
    val language: String
)

object WordRepository {
    fun listCategories(forUserId: Int?): List<CategoryDto> = transaction {
        val basePackIds = WordPacks.select(WordPacks.id).where { WordPacks.isBuiltIn eq true }
        val baseCategories = CategoryEntity.find {
            Categories.packId inSubQuery basePackIds
        }

        val userCategories = if (forUserId != null) {
            val userPackIds = WordPacks.select(WordPacks.id).where {
                (WordPacks.ownerUserId eq forUserId) and (WordPacks.isBuiltIn eq false)
            }
            CategoryEntity.find { Categories.packId inSubQuery userPackIds }
        } else {
            emptyList()
        }

        (baseCategories + userCategories).map { cat ->
            val pack = WordPackEntity.findById(cat.packId)
            CategoryDto(cat.id.value, cat.name, pack?.language ?: "es")
        }
    }

    fun randomWordFrom(categoryIds: List<Int>, language: String): Triple<String, String, List<String>>? = transaction {
        val categoriesToUse = if (categoryIds.isEmpty()) {
            val basePackIds = WordPacks.select(WordPacks.id).where {
                (WordPacks.isBuiltIn eq true) and (WordPacks.language eq language)
            }
            CategoryEntity.find {
                Categories.packId inSubQuery basePackIds
            }
        } else {
            CategoryEntity.find { Categories.id inList categoryIds }
        }

        val categoriesList = categoriesToUse.toList()
        if (categoriesList.isEmpty()) return@transaction null

        val allWords = categoriesList.flatMap { cat ->
            WordEntity.find { Words.categoryId eq cat.id }.toList()
        }
        if (allWords.isEmpty()) return@transaction null

        val randomWord = allWords.random()
        val category = CategoryEntity.findById(randomWord.categoryId)
            ?: return@transaction null

        val hints = randomWord.hints
            ?.split("||")
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        Triple(randomWord.text, category.name, hints)
    }

    fun importPack(ownerUserId: Int, jsonString: String): Int = transaction {
        val json = Json { ignoreUnknownKeys = true }
        val packJson = json.decodeFromString<WordPackJson>(jsonString)

        val user = UserEntity.findById(ownerUserId) ?: throw Exception("User not found")

        val pack = WordPackEntity.new {
            name = packJson.name
            language = packJson.language
            isBuiltIn = false
            this.ownerUserId = user.id
        }

        for (catJson in packJson.categories) {
            val cat = CategoryEntity.new {
                packId = pack.id
                name = catJson.name
            }

            for (wordElement in catJson.words) {
                val parsed = wordElement.toWord()
                if (parsed.text.isBlank()) continue
                WordEntity.new {
                    categoryId = cat.id
                    text = parsed.text
                    hints = if (parsed.hints.isEmpty()) null else parsed.hints.joinToString("||")
                }
            }
        }

        pack.id.value
    }

    fun deletePack(ownerUserId: Int, packId: Int): Boolean = transaction {
        val pack = WordPackEntity.findById(packId) ?: return@transaction false

        if (pack.isBuiltIn || pack.ownerUserId?.value != ownerUserId) {
            return@transaction false
        }

        pack.delete()
        true
    }

    fun listUserPacks(userId: Int): List<CategoryDto> = transaction {
        val userPackIds = WordPacks.select(WordPacks.id).where {
            (WordPacks.ownerUserId eq userId) and (WordPacks.isBuiltIn eq false)
        }
        val userPacks = WordPackEntity.find { WordPacks.id inSubQuery userPackIds }

        userPacks.map { pack ->
            CategoryDto(pack.id.value, pack.name, pack.language)
        }
    }
}
