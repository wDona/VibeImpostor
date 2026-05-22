package org.example.project.game

object WordMatcher {
    fun matches(guess: String, correctWord: String, language: String): Boolean {
        val normalized1 = normalize(guess, language)
        val normalized2 = normalize(correctWord, language)

        if (normalized1 == normalized2) return true

        // Check singular/plural variants
        return normalized1 == removePlural(normalized2) ||
               removePlural(normalized1) == normalized2
    }

    private fun normalize(word: String, language: String): String {
        var text = word.trim()

        // Remove accents/tildes
        text = text.replace(Regex("[àáäâ]"), "a")
        text = text.replace(Regex("[èéëê]"), "e")
        text = text.replace(Regex("[ìíïî]"), "i")
        text = text.replace(Regex("[òóöô]"), "o")
        text = text.replace(Regex("[ùúüû]"), "u")
        text = text.replace(Regex("[ñ]"), "n")

        // In English, replace hyphens with spaces
        if (language == "en") {
            text = text.replace("-", " ")
        }

        // Lowercase
        return text.lowercase()
    }

    private fun removePlural(word: String): String {
        return if (word.endsWith("s") && word.length > 1) {
            word.dropLast(1)
        } else {
            word
        }
    }
}
