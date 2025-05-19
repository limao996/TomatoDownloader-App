package org.limao996.tomato_downloader

import java.io.File


actual fun isDesktop() = true
actual fun getDataDir(): File {
    return File("./data").apply { mkdirs() }
}

actual fun log(vararg msg: Any?) {
    println(
        msg.joinToString("\t") { it.toString() })
}

actual fun shareFile(file: File): Unit = TODO("Not yet implemented")

actual fun createFile(
    prefix: String, suffix: String
) = File.createTempFile(prefix, suffix)