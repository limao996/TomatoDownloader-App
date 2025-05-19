import UserAgentGenerator.randomUserAgent
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONPath
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.GZIPInputStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private class Crypto(key: String) {
    private val key: ByteArray
    private val cipherMode = "AES/CBC/PKCS5Padding"

    init {
        val keyBytes = key.hexToBytes()
        require(keyBytes.size == 16) { "Key length mismatch! key: ${keyBytes.toHex()}" }
        this.key = keyBytes
    }

    fun encrypt(data: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(cipherMode)
        val keySpec = SecretKeySpec(key, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(iv))
        return cipher.doFinal(data)
    }

    fun decrypt(data: ByteArray): ByteArray {
        val iv = data.copyOfRange(0, 16)
        val ct = data.copyOfRange(16, data.size)
        val cipher = Cipher.getInstance(cipherMode)
        val keySpec = SecretKeySpec(key, "AES")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(iv))
        return cipher.doFinal(ct)
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun newRegisterKeyContent(serverDeviceId: String, strVal: String): String {
        require(serverDeviceId.all { it.isDigit() } && strVal.all { it.isDigit() }) {
            "Parse failed\nserver_device_id: $serverDeviceId\nstr_val:$strVal"
        }

        val combinedBytes = serverDeviceId.toLong().toBytesLittleEndian(8) + strVal.toLong().toBytesLittleEndian(8)
        val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val encData = encrypt(combinedBytes, iv)
        return Base64.encode(iv + encData)
    }
}


private fun String.hexToBytes(): ByteArray {
    return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}

private fun ByteArray.toHex(): String {
    return joinToString("") { "%02x".format(it) }
}

private fun Long.toBytesLittleEndian(size: Int): ByteArray {
    return ByteArray(size) { i -> (this shr (8 * i)).toByte() }
}

private fun Map<String, String>.toQueryString(): String {
    if (isEmpty()) return ""
    return "?" + entries.joinToString("&") { "${it.key}=${it.value}" }
}


class OldTomatoChapterDownloader(val chapterId: String) {
    private val config = object {
        val installId = "4427064614339001"
        val serverDeviceId = "4427064614334905"
        val aid = "1967"
        val updateVersionCode = "62532"
    }

    private fun downloadBody(): Any? {
        val url = "https://api5-normal-sinfonlineb.fqnovel.com/reading/reader/batch_full/v"
        val params = mapOf(
            "item_ids" to chapterId,
            "req_type" to 1,
            "aid" to config.aid,
            "update_version_code" to config.updateVersionCode
        )

        val request = Request.Builder().url("$url?" + params.entries.joinToString("&") { "${it.key}=${it.value}" })
            .header("Cookie", "install_id=${config.installId}").header("User-Agent", randomUserAgent(false)).get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("请求失败: ${response.code}")
        }
        val body = response.body!!.string()
        return JSONPath.extract(body, "data.$chapterId")
    }

    private fun getNovelData(body: Any?): Any? {
        return JSONPath.eval(body, "$.novel_data")
    }

    private fun getWordCount(novelData: Any?): String {
        return JSONPath.eval(novelData, "$.chapter_word_number") as String
    }

    private fun getLastUpdateDate(novelData: Any?): String {
        val time = JSONPath.eval(novelData, "$.first_pass_time") as String
        return SimpleDateFormat.getInstance().format(Date(time.toLong() * 1000))
    }

    private fun getEncryptContent(body: Any?): String {
        return JSONPath.eval(body, "$.content") as String
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun getRegisterKey(): String {
        val url = "https://api5-normal-sinfonlineb.fqnovel.com/reading/crypt/registerkey"
        val params = mapOf("aid" to config.aid)
        val crypto = Crypto("ac25c67ddd8f38c1b37a2348828e222e")

        val payload = mapOf(
            "content" to crypto.newRegisterKeyContent(config.serverDeviceId, "0"), "keyver" to 1
        )

        val request =
            Request.Builder().url(url + params.toQueryString()).header("Cookie", "install_id=${config.installId}")
                .header("Content-Type", "application/json")
                .post(JSON.toJSONString(payload).toRequestBody("application/json".toMediaType())).build()

        val response = client.newCall(request).execute()
        val key = JSONPath.extract(response.body!!.string(), "$.data.key") as String
        val byteKey = crypto.decrypt(Base64.decode(key))
        return byteKey.toHex()
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun decryptContent(encryptContent: String): String {
        val key = getRegisterKey()
        val crypto = Crypto(key)
        val byteContent = crypto.decrypt(Base64.decode(encryptContent))
        return GZIPInputStream(ByteArrayInputStream(byteContent)).use {
            it.readBytes().toString(Charsets.UTF_8)
        }
    }

    fun analyzeContent(content: String): String {
        var content = content.replace(Regex("<header>.*?</header>"), "").replace(Regex("<footer>.*?</footer>"), "")
            .replace("</?article>".toRegex(), "").replace(Regex("<p idx=\"\\d+\">"), "\n").replace("</p>", "\n")
            .replace(Regex("<[^>]+>"), "").replace("\\u003c", "").replace("\\u003e", "")

        content = content.replace(Regex("\n{2,}"), "\n").trim()
        content = content.split("\n").joinToString("\n") {
            if (it.isNotBlank()) "    $it" else it
        }
        return content
    }

    private fun getContent(body: Any?) = analyzeContent(
        decryptContent(
            getEncryptContent(body)
        )
    )

    fun downloadChapter(): ChapterData {
        val body = downloadBody()
        val novelData = getNovelData(body)
        return ChapterData(
            content = getContent(body),
            wordCount = getWordCount(novelData),
            lastUpdateDate = getLastUpdateDate(novelData)
        )
    }

}

class TomatoChapterDownloader(val chapterId: String) {
    val url = "https://fanqie.tutuxka.top/?item_ids="
    fun downloadChapter(): ChapterData {
        val request = Request.Builder().url("$url$chapterId").header("User-Agent", randomUserAgent(false)).get().build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("请求失败: ${response.code}")
        }
        val body = response.body!!.string()
        val content = JSONPath.extract(body, "data.content") as String
        return ChapterData(content.substringAfter("\n"))
    }
}

data class ChapterData(
    val content: String, val wordCount: String? = null, val lastUpdateDate: String? = null
)