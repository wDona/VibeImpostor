package org.example.project.db

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

data class CategoryDto(
    val id: Int,
    val name: String
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

        (baseCategories + userCategories).map { CategoryDto(it.id.value, it.name) }
    }

    fun randomWordFrom(categoryIds: List<Int>, language: String): Pair<String, String>? = transaction {
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

        Pair(randomWord.text, category.name)
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

            for (wordText in catJson.words) {
                WordEntity.new {
                    categoryId = cat.id
                    text = wordText
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
}
