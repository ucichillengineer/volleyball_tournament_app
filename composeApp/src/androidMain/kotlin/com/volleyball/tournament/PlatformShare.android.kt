package com.volleyball.tournament

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

private var shareContext: Context? = null

fun initAndroidShare(context: Context) {
    shareContext = context.applicationContext
}

actual fun shareText(text: String, title: String) {
    val ctx = shareContext ?: return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_TITLE, title)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    ctx.startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

actual fun openWhatsApp(text: String) {
    val ctx = shareContext ?: return
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(intent)
    } catch (_: Exception) {
        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("teams", text))
        Toast.makeText(ctx, "WhatsApp not found — team sheet copied", Toast.LENGTH_LONG).show()
        val web = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://wa.me/?text=${Uri.encode(text)}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { ctx.startActivity(web) }
    }
}
