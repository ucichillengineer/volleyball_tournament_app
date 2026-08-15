package com.familydays.app

object LegacyCsvParser {
    private val monthNumbers = mapOf(
        "jan" to 1, "january" to 1, "feb" to 2, "february" to 2, "mar" to 3, "march" to 3,
        "apr" to 4, "april" to 4, "may" to 5, "jun" to 6, "june" to 6, "jul" to 7,
        "july" to 7, "aug" to 8, "august" to 8, "sep" to 9, "sept" to 9, "september" to 9,
        "oct" to 10, "october" to 10, "nov" to 11, "november" to 11, "dec" to 12, "december" to 12
    )

    /**
     * Extracts the dated records from the supplied legacy CSV. Records without a complete date are
     * intentionally omitted, so they cannot generate an accidental reminder.
     */
    fun parse(csv: String): List<ImportantDay> {
        return csv
            .replace("\"", "")
            .split('\n', '\r')
            .flatMap { line -> parseLine(line.trim()) }
            .distinctBy { "${it.name}-${it.month}-${it.day}-${it.type}" }
    }

    private fun parseLine(line: String): List<ImportantDay> {
        if (line.isBlank()) return emptyList()

        val name = line.substringBefore(':').trim()
        if (name.isBlank() || !line.contains(':')) return emptyList()
        val type = if (Regex("""\b(MD|DOM|Anniv|Anniversary|(^|:)M)\b""", RegexOption.IGNORE_CASE).containsMatchIn(line)) {
            EventType.ANNIVERSARY
        } else {
            EventType.BIRTHDAY
        }
        val date = extractDate(line) ?: return emptyList()
        val names = name.split(Regex("\\s+"))
        val givenName = names.first().replaceFirstChar { it.uppercase() }
        val initial = names.drop(1).firstOrNull()?.firstOrNull()?.uppercaseChar()?.toString().orEmpty()

        return listOf(
            ImportantDay(
                id = "${givenName.lowercase()}-${date.month}-${date.day}-$type",
                name = givenName,
                initial = initial,
                month = date.month,
                day = date.day,
                year = date.year,
                type = type,
                needsReview = date.year == null
            )
        )
    }

    private data class ParsedDate(val month: Int, val day: Int, val year: Int?)

    private fun extractDate(text: String): ParsedDate? {
        val named = Regex(
            """(?i)\b(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\.?['\s,-]*(\d{1,2})(?:[\s,'/-]*(\d{2,4}))?"""
        ).find(text)
        if (named != null) {
            val month = monthNumbers[named.groupValues[1].lowercase().removeSuffix(".")] ?: return null
            return ParsedDate(month, named.groupValues[2].toInt(), normaliseYear(named.groupValues[3]))
        }

        val numeric = Regex("""\b(\d{1,2})/(\d{1,2})/(\d{2,4})\b""").find(text) ?: return null
        return ParsedDate(numeric.groupValues[1].toInt(), numeric.groupValues[2].toInt(), normaliseYear(numeric.groupValues[3]))
    }

    private fun normaliseYear(value: String): Int? {
        if (value.isBlank()) return null
        val year = value.toInt()
        return if (year < 100) 1900 + year else year
    }
}
