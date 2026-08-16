package com.familydays.app

private const val STORAGE_NAME = "family_days"
private const val EVENTS_KEY = "events_csv"

actual fun loadSavedEvents(onLoaded: (String?) -> Unit) {
    val saved = MainActivity.appContext
        ?.getSharedPreferences(STORAGE_NAME, 0)
        ?.getString(EVENTS_KEY, null)
    onLoaded(saved)
}

actual fun saveEvents(csv: String) {
    MainActivity.appContext
        ?.getSharedPreferences(STORAGE_NAME, 0)
        ?.edit()
        ?.putString(EVENTS_KEY, csv)
        ?.apply()
}
