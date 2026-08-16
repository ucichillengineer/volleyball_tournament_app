package com.familydays.app

actual fun chooseCsvFile(onSelected: (String) -> Unit) {
    // Android file picking is wired to the Activity in a later platform-specific pass.
    // The web app uses the browser's local file picker now.
}
