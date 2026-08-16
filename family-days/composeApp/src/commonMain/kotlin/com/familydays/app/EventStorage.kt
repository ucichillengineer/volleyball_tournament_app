package com.familydays.app

expect fun loadSavedEvents(onLoaded: (String?) -> Unit)
expect fun saveEvents(csv: String)

fun List<ImportantDay>.toStructuredCsv(): String = buildString {
    appendLine("name,last_initial,event_type,month,day,year")
    this@toStructuredCsv.forEach { event ->
        appendLine(
            listOf(
                event.name,
                event.initial,
                event.type.label.lowercase(),
                event.month,
                event.day,
                event.year.orEmpty()
            ).joinToString(",")
        )
    }
}

private fun Int?.orEmpty(): String = this?.toString().orEmpty()
