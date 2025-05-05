package org.limao996.tomato_downloader

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "番茄小说下载器",
    ) {
        App()
    }
}