package org.limao996.tomato_downloader

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init


lateinit var activity: MainActivity

class MainActivity : ComponentActivity() {

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSendText(intent)
    }

    private fun handleSendText(intent: Intent) {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (sharedText != null) {
            bookId = Regex("book_id=(\\d+)").find(sharedText)?.groupValues?.get(1) ?: bookId
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (getIntent().getAction() != null && getIntent().getAction()
                .equals(Intent.ACTION_SEND) && getIntent().getType() != null && "text/plain".equals(getIntent().getType())
        ) {
            handleSendText(getIntent()); // 处理文本数据
        }

        activity = this
        FileKit.init(this)
        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}