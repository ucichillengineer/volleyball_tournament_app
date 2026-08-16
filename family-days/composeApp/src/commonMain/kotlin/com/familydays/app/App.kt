package com.familydays.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val importedRecords = """
    Alex: Jan 15, 1990
    Priya: April 8, 1988
    Jordan: July 21, 2012
    Morgan: October 3, 2016
    Taylor: Dec 12, 1982
""".trimIndent()

@Composable
fun FamilyDaysApp() {
    val events = remember { mutableStateListOf<ImportantDay>().apply { addAll(LegacyCsvParser.parse(importedRecords)) } }
    var screen by remember { mutableStateOf(Screen.UPCOMING) }

    MaterialTheme {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text("Family Days", style = MaterialTheme.typography.headlineMedium)
            Text("Birthdays & anniversaries", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { screen = Screen.UPCOMING }) { Text("Upcoming") }
                Button(onClick = { screen = Screen.ALL }) { Text("All") }
                Button(onClick = { screen = Screen.HOLIDAYS }) { Text("Holidays") }
                Button(onClick = { screen = Screen.ADD }) { Text("Add") }
                Button(onClick = { screen = Screen.IMPORT }) { Text("Import CSV") }
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxSize()) {
                when (screen) {
                    Screen.UPCOMING -> {
                        Column(Modifier.fillMaxSize()) {
                            TodayGreetings(events)
                            Spacer(Modifier.height(12.dp))
                            EventList(events.sortedBy { it.daysUntil(todayMonth(), todayDay()) }, showCountdown = true)
                        }
                    }
                    Screen.ALL -> EventList(events.sortedWith(compareBy({ it.month }, { it.day }, { it.name })))
                    Screen.HOLIDAYS -> HolidayCalendar()
                    Screen.ADD -> AddEvent { events.add(it); screen = Screen.UPCOMING }
                    Screen.IMPORT -> ImportCsv { imported ->
                        events.clear()
                        events.addAll(imported)
                        screen = Screen.UPCOMING
                    }
                }
            }
        }
    }
}

private enum class Screen { UPCOMING, ALL, HOLIDAYS, ADD, IMPORT }

@Composable
private fun EventList(events: List<ImportantDay>, showCountdown: Boolean = false) {
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(events, key = { it.id }) { event ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(event.displayName, style = MaterialTheme.typography.titleMedium)
                    Text("${event.type.label} · ${monthName(event.month)} ${event.day}${event.year?.let { ", $it" }.orEmpty()}")
                    Text(event.greeting(todayYear()), style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { shareGreetingOnWhatsApp(event.greeting(todayYear())) }) { Text("Share on WhatsApp") }
                    if (showCountdown) Text("In ${event.daysUntil(todayMonth(), todayDay())} days")
                    if (event.needsReview) Text("Imported date needs confirmation", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun TodayGreetings(events: List<ImportantDay>) {
    val todayEvents = events.filter { it.month == todayMonth() && it.day == todayDay() }
    val observances = todayObservances(todayYear(), todayMonth(), todayDay())
    if (todayEvents.isEmpty() && observances.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Today’s celebrations", style = MaterialTheme.typography.titleLarge)
        (todayEvents.map { Observance(it.displayName, it.greeting(todayYear())) } + observances).forEach { celebration ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(celebration.title, style = MaterialTheme.typography.titleMedium)
                    Text(celebration.greeting)
                    Button(onClick = { shareGreetingOnWhatsApp(celebration.greeting) }) { Text("Share on WhatsApp") }
                }
            }
        }
    }
}

@Composable
private fun HolidayCalendar() {
    val holidays = observancesForYear(todayYear())
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Column {
                Text("${todayYear()} holiday calendar", style = MaterialTheme.typography.titleLarge)
                Text("U.S. federal observances and major Hindu holidays. Scroll to see the full calendar.")
            }
        }
        items(holidays, key = { "${it.month}-${it.day}-${it.observance.title}" }) { holiday ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("${monthName(holiday.month)} ${holiday.day} · ${holiday.observance.title}", style = MaterialTheme.typography.titleMedium)
                    Text(holiday.observance.greeting)
                    Button(onClick = { shareGreetingOnWhatsApp(holiday.observance.greeting) }) { Text("Share on WhatsApp") }
                }
            }
        }
    }
}

@Composable
private fun ImportCsv(onImported: (List<ImportantDay>) -> Unit) {
    var message by remember { mutableStateOf("Choose a CSV from this device. It stays in this browser session.") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Import important days", style = MaterialTheme.typography.titleLarge)
        Text("Use sample-important-days.csv as the template: name, last_initial, event_type, month, day, year.")
        Button(onClick = {
            chooseCsvFile { csv ->
                val imported = LegacyCsvParser.parse(csv)
                message = if (imported.isEmpty()) "No valid events were found. Check the CSV columns and dates." else "Imported ${imported.size} event(s)."
                if (imported.isNotEmpty()) onImported(imported)
            }
        }) { Text("Choose CSV file") }
        Text(message)
    }
}

@Composable
private fun AddEvent(onAdd: (ImportantDay) -> Unit) {
    var name by remember { mutableStateOf("") }
    var initial by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    var day by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var isAnniversary by remember { mutableStateOf(false) }
    var greetingTemplate by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Add an important day", style = MaterialTheme.typography.titleLarge)
        Text("Use only a last-name initial to protect family privacy.")
        OutlinedTextField(name, { name = it }, label = { Text("First name") }, singleLine = true)
        OutlinedTextField(initial, { initial = it.take(1).uppercase() }, label = { Text("Last initial (optional)") }, singleLine = true)
        OutlinedTextField(month, { month = it.filter(Char::isDigit).take(2) }, label = { Text("Month (1–12)") }, singleLine = true)
        OutlinedTextField(day, { day = it.filter(Char::isDigit).take(2) }, label = { Text("Day (1–31)") }, singleLine = true)
        OutlinedTextField(year, { year = it.filter(Char::isDigit).take(4) }, label = { Text("Year (enables age/anniversary years)") }, singleLine = true)
        OutlinedTextField(
            greetingTemplate,
            { greetingTemplate = it },
            label = { Text("Custom greeting (optional)") },
            supportingText = { Text("Use {name}, {years}, and {event}.") }
        )
        Button(onClick = { isAnniversary = !isAnniversary }) {
            Text(if (isAnniversary) "Type: Anniversary" else "Type: Birthday")
        }
        Button(onClick = {
            val parsedMonth = month.toIntOrNull()
            val parsedDay = day.toIntOrNull()
            error = when {
                name.isBlank() -> "First name is required."
                parsedMonth !in 1..12 || parsedDay !in 1..31 -> "Enter a valid month and day."
                else -> null
            }
            if (error == null) {
                onAdd(
                    ImportantDay(
                        id = "${name.lowercase()}-$parsedMonth-$parsedDay-${isAnniversary}",
                        name = name.trim(),
                        initial = initial,
                        month = parsedMonth!!,
                        day = parsedDay!!,
                        year = year.toIntOrNull(),
                        type = if (isAnniversary) EventType.ANNIVERSARY else EventType.BIRTHDAY,
                        greetingTemplate = greetingTemplate.trim().takeIf { it.isNotBlank() }
                    )
                )
            }
        }) { Text("Save event") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

private fun monthName(month: Int) = listOf("", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")[month]
