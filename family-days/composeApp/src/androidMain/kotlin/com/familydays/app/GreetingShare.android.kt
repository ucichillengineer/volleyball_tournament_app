package com.familydays.app

import android.content.Intent
import android.content.ActivityNotFoundException

actual fun shareGreetingOnWhatsApp(greeting: String) {
    val activity = MainActivity.currentActivity ?: return
    val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, greeting)
        setPackage("com.whatsapp")
    }
    try {
        activity.startActivity(whatsappIntent)
    } catch (_: ActivityNotFoundException) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, greeting)
        }
        activity.startActivity(Intent.createChooser(shareIntent, "Share greeting"))
    }
}
