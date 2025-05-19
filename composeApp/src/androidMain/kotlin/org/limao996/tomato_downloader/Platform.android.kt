package org.limao996.tomato_downloader

import android.content.Intent
import android.content.Intent.*
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File


actual fun isDesktop() = false
actual fun getDataDir(): File {
    return application.filesDir!!.apply {
        mkdirs()
    }
}

actual fun log(vararg msg: Any?) {
    Log.d(
        "TAG", msg.joinToString("\t") { it.toString() })
}

actual fun shareFile(file: File) {
    val fileUri = FileProvider.getUriForFile(
        application, "TomatoDownloader.provider", file
    )
    val intent = Intent(ACTION_SEND)
    intent.setType("*/*")
    intent.putExtra(EXTRA_STREAM, fileUri)
    intent.addFlags(FLAG_GRANT_READ_URI_PERMISSION)
    activity.startActivity(createChooser(intent, "分享文件"))
}

actual fun createFile(
    prefix: String, suffix: String
): File {
    val dataDir = getDataDir()
    dataDir.listFiles()?.forEach {
        it.delete()
    }
    return File(dataDir, prefix + suffix).apply { createNewFile() }
}