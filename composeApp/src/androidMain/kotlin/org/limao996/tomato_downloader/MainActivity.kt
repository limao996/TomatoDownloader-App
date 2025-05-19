package org.limao996.tomato_downloader

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
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
            bookId = Regex("\\d{19}").find(sharedText)?.groupValues?.getOrNull(0) ?: bookId
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.action != null && intent.action
                .equals(Intent.ACTION_SEND) && intent.type != null && "text/plain".equals(intent.type)
        ) {
            handleSendText(intent); // 处理文本数据
        }
        enableEdgeToEdge()
        activity = this
        FileKit.init(this)
        setContent {
            App()
        }
    }
}