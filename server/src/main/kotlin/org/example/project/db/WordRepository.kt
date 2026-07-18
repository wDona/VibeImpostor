package org.example.project.db

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
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

    // Contenido completo de un pack propio, en el mismo formato que acepta importPack,
    // para poder rellenar el editor de la UI.
    fun getPackContent(ownerUserId: Int, packId: Int): WordPackJson? = transaction {
        val pack = ownedPack(ownerUserId, packId) ?: return@transaction null

        val categories = CategoryEntity.find { Categories.packId eq pack.id }.map { cat ->
            val words = WordEntity.find { Words.categoryId eq cat.id }.map { word ->
                val hints = word.hints?.split("||")?.filter { it.isNotBlank() }.orEmpty()
                if (hints.isEmpty()) {
                    JsonPrimitive(word.text)
                } else {
                    buildJsonObject {
                        put("text", JsonPrimitive(word.text))
                        put("hints", JsonArray(hints.map { JsonPrimitive(it) }))
                    }
                }
            }
            CategoryJson(cat.name, words)
        }

        WordPackJson(pack.name, pack.language, categories)
    }

    // Sustituye el contenido de un pack propio por el del JSON. Las categorías se
    // emparejan por nombre en vez de borrarse y recrearse: las salas guardan
    // selectedCategoryIds, y unos ids nuevos las dejarían apuntando a categorías muertas.
    fun replacePackContent(ownerUserId: Int, packId: Int, jsonString: String): Boolean = transaction {
        val pack = ownedPack(ownerUserId, packId) ?: return@transaction false

        val json = Json { ignoreUnknownKeys = true }
        val packJson = json.decodeFromString<WordPackJson>(jsonString)

        pack.name = packJson.name
        pack.language = packJson.language

        val existing = CategoryEntity.find { Categories.packId eq pack.id }.associateBy { it.name }
        val keptNames = mutableSetOf<String>()

        for (catJson in packJson.categories) {
            if (catJson.name.isBlank()) continue
            keptNames += catJson.name

            val cat = existing[catJson.name] ?: CategoryEntity.new {
                this.packId = pack.id
                name = catJson.name
            }

            Words.deleteWhere { categoryId eq cat.id }
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

        for ((name, cat) in existing) {
            if (name in keptNames) continue
            Words.deleteWhere { categoryId eq cat.id }
            cat.delete()
        }

        true
    }

    private fun ownedPack(ownerUserId: Int, packId: Int): WordPackEntity? {
        val pack = WordPackEntity.findById(packId) ?: return null
        if (pack.isBuiltIn || pack.ownerUserId?.value != ownerUserId) return null
        return pack
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
