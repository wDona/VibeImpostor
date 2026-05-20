package org.example.project.db

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class WordPackJson(
    val name: String,
    val language: String,
    val categories: List<CategoryJson>
)

@Serializable
data class CategoryJson(
    val name: String,
    val words: List<String>
)

fun seedBaseWordsIfEmpty() {
    transaction {
        val existingPack = WordPackEntity.find {
            WordPacks.isBuiltIn eq true
        }.firstOrNull()

        if (existingPack != null) return@transaction

        val json = Json { ignoreUnknownKeys = true }
        val packJson = json.decodeFromString<WordPackJson>(
            object {}.javaClass.classLoader
                .getResourceAsStream("words/base_es.json")
                ?.bufferedReader()
                ?.readText()
                ?: throw RuntimeException("base_es.json not found")
        )

        val pack = WordPackEntity.new {
            name = packJson.name
            language = packJson.language
            isBuiltIn = true
            ownerUserId = null
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
    }
}
