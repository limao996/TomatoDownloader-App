@file:OptIn(ExperimentalMaterial3Api::class)

package org.limao996.tomato_downloader

import TomatoBook
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import buildTomatoBook
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.dialogs.openFileSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.io.File

var bookId by mutableStateOf("")

@Composable
@Preview
fun App() {
    MaterialTheme {
        Scaffold(topBar = {
            if (!isDesktop()) TopAppBar(title = {
                Text(text = "番茄小说下载器")
            })
        }) {
            Surface(
                Modifier.fillMaxSize().padding(it),
            ) {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    var isEpub by remember { mutableStateOf(false) }
                    var state by remember { mutableIntStateOf(0) }
                    var msg by remember { mutableStateOf("准备就绪") }
                    var file by remember { mutableStateOf<File?>(null) }
                    OutlinedTextField(
                        bookId,
                        enabled = state == 0,
                        label = { Text("书籍id") },
                        onValueChange = { bookId = it })
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("启用Epub模式")
                        Checkbox(
                            isEpub, enabled = state == 0, onCheckedChange = { isEpub = it })
                    }
                    Button(enabled = state != 1, onClick = {
                        if (state == 2) {
                            file?.delete()
                            file = null
                            state = 0
                            return@Button
                        }
                        state = 1
                        GlobalScope.launch(Dispatchers.IO) {
                            try {
                                val book = TomatoBook(bookId)
                                val _file = createFile(book.name, (if (isEpub) ".epub" else ".txt"))

                                launch {
                                    buildTomatoBook(
                                        book = book,
                                        file = _file,
                                        isEpub = isEpub,
                                    ) {
                                        msg = it
                                    }
                                }.join()
                                file = _file
                                state = 2
                            } catch (e: Exception) {
                                file?.delete()
                                file = null
                                state = 0
                                msg = "下载失败"
                            }
                        }
                    }) {
                        Text(
                            when (state) {
                                0 -> "开始下载"
                                1 -> "正在下载"
                                else -> "清空文件"
                            }
                        )
                    }
                    OutlinedButton(enabled = file != null, onClick = {
                        GlobalScope.launch(Dispatchers.IO) {
                            val pFile = FileKit.openFileSaver(
                                suggestedName = file!!.nameWithoutExtension, extension = file!!.extension
                            )
                            pFile?.let { PlatformFile(file!!).copyTo(it) }
                        }
                    }) {
                        Text("导出文件")
                    }
                    if (!isDesktop()) {
                        OutlinedButton(enabled = file != null, onClick = {
                            shareFile(file!!)
                        }) {
                            Text("分享文件")
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(msg)

                }
            }
        }
    }
}