package com.familydays.app

import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader

actual fun chooseCsvFile(onSelected: (String) -> Unit) {
    val input = document.createElement("input") as HTMLInputElement
    input.type = "file"
    input.accept = ".csv,text/csv"
    input.onchange = {
        val file = input.files?.item(0)
        if (file != null) {
            val reader = FileReader()
            reader.onload = {
                onSelected(reader.result as String)
                null
            }
            reader.readAsText(file)
        }
        null
    }
    input.click()
}
