package org.limao996.tomato_downloader

import java.io.File

expect fun isDesktop(): Boolean
expect fun getDataDir(): File
fun getDataDir(name: String) = File(getDataDir(), name)

expect fun log(vararg msg: Any?)

expect fun shareFile(file: File)

expect fun createFile(
    prefix: String, suffix: String
): File