package org.example.project.db

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.and
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
    val words: List<JsonElement>
)

data class ParsedWord(val text: String, val hints: List<String>)

fun JsonElement.toWord(): ParsedWord = when (this) {
    is JsonPrimitive -> ParsedWord(content, emptyList())
    is JsonObject -> ParsedWord(
        get("text")?.jsonPrimitive?.content ?: "",
        get("hints")?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
    )
    else -> ParsedWord("", emptyList())
}

private val ES_MINI_FILES = listOf(
    "words/es/animales.json",
    "words/es/comida.json",
    "words/es/bebida_ropa.json",
    "words/es/casa_mueble.json",
    "words/es/transporte_tecnologia.json",
    "words/es/escuela_profesion.json",
    "words/es/cuerpo_salud.json",
    "words/es/naturaleza_clima.json",
    "words/es/color_forma.json",
    "words/es/deportes_instrumento.json",
    "words/es/juego_pelicula_arte.json",
    "words/es/emocion_musica_literatura.json",
    "words/es/pais_ciudad.json",
    "words/es/familia_verbo.json",
    "words/es/fruta_verdura.json",
    "words/es/numero_estacion.json",
    "words/es/mitologia_objeto.json",
    "words/es/construccion.json"
)

private val EN_MINI_FILES = listOf(
    "words/en/animals.json",
    "words/en/food.json",
    "words/en/drink_clothes.json",
    "words/en/house_furniture.json",
    "words/en/transport_technology.json",
    "words/en/school_job.json",
    "words/en/body_health.json",
    "words/en/nature_weather.json",
    "words/en/color_shape.json",
    "words/en/sports_instrument.json",
    "words/en/game_movie_art.json",
    "words/en/emotion_music_literature.json",
    "words/en/country_city.json",
    "words/en/family_verb.json",
    "words/en/fruit_vegetable.json",
    "words/en/number_season.json",
    "words/en/mythology_object.json",
    "words/en/building.json"
)

fun seedBaseWordsIfEmpty() {
    seedBasePackFromFiles("Paquete base", "es", ES_MINI_FILES)
    seedBasePackFromFiles("Base pack", "en", EN_MINI_FILES)
}

private fun seedBasePackFromFiles(packName: String, lang: String, files: List<String>) {
    transaction {
        WordPackEntity.find {
            (WordPacks.isBuiltIn eq true) and (WordPacks.language eq lang)
        }.firstOrNull()?.let { oldPack ->
            CategoryEntity.find { Categories.packId eq oldPack.id }.forEach { cat ->
                WordEntity.find { Words.categoryId eq cat.id }.forEach { it.delete() }
                cat.delete()
            }
            oldPack.delete()
        }

        val pack = WordPackEntity.new {
            name = packName
            language = lang
            isBuiltIn = true
            ownerUserId = null
        }

        val json = Json { ignoreUnknownKeys = true }
        val classLoader = object {}.javaClass.classLoader

        for (filePath in files) {
            val content = classLoader.getResourceAsStream(filePath)
                ?.bufferedReader()?.readText()
                ?: run {
                    println("[WordSeeder] $filePath not found, skipping")
                    continue
                }

            val packJson = try {
                json.decodeFromString<WordPackJson>(content)
            } catch (e: Exception) {
                println("[WordSeeder] Failed to parse $filePath: ${e.message}")
                continue
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
        }
    }
}
