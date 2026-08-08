package com.volleyball.tournament

import kotlinx.browser.window
import org.w3c.dom.url.URLSearchParams

actual fun shareText(text: String, title: String) {
    window.navigator.clipboard.writeText(text)
    window.alert("$title copied to clipboard — paste into WhatsApp, email, or Drive.")
}

actual fun openWhatsApp(text: String) {
    val params = URLSearchParams()
    params.append("text", text)
    window.open("https://wa.me/?${params.toString()}", "_blank")
}
