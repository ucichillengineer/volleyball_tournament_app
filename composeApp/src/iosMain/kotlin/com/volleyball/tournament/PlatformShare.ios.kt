package com.volleyball.tournament

import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard

actual fun shareText(text: String, title: String) {
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    val controller = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
    root.presentViewController(controller, animated = true, completion = null)
}

actual fun openWhatsApp(text: String) {
    val encoded = text.encodeURLParameter()
    val url = NSURL.URLWithString("whatsapp://send?text=$encoded")
    val app = UIApplication.sharedApplication
    if (url != null && app.canOpenURL(url)) {
        app.openURL(url)
    } else {
        UIPasteboard.generalPasteboard.string = text
        val web = NSURL.URLWithString("https://wa.me/?text=$encoded")
        if (web != null) app.openURL(web)
    }
}

private fun String.encodeURLParameter(): String =
    this.map { ch ->
        when {
            ch.isLetterOrDigit() || ch in "-._~" -> ch.toString()
            else -> "%${ch.code.toString(16).uppercase().padStart(2, '0')}"
        }
    }.joinToString("")
