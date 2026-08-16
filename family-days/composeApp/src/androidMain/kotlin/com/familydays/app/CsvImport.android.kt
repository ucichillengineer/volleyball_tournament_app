package com.familydays.app

actual fun chooseCsvFile(onSelected: (String) -> Unit) {
    MainActivity.currentActivity?.selectCsv(onSelected)
}
