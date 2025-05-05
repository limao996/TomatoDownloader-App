import UserAgentGenerator.randomUserAgent
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.*
import okhttp3.Headers.Companion.toHeaders
import org.jsoup.Jsoup
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.random.Random


object RequestHandler {

    val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS).cookieJar(object : CookieJar {
            private val cookieStore = mutableMapOf<String, List<Cookie>>()

            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                cookieStore[url.host] = cookies
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host] ?: emptyList()
            }
        }).build()

    fun getHeaders(): Headers {
        val headers = mutableMapOf(
            "User-Agent" to randomUserAgent(false)
        )
        headers["Cookie"] = "novel_web_id=" + Random.nextLong(1000000000000000000L, 3960666597597448470L)
        return headers.toHeaders()
    }


    data class BookInfo(val name: String, val author: String, val description: String, val cover: String? = null)

    fun getBookInfo(bookId: String): BookInfo {
        val url = "https://fanqienovel.com/page/$bookId"
        val request = Request.Builder().url(url).headers(getHeaders()).build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("网络请求失败，状态码: ${response.code}")
        }

        val html = response.body?.string() ?: ""
        val soup = Jsoup.parse(html)

        // 获取书名
        val nameElement = soup.selectFirst("h1")
        val name = nameElement?.text() ?: "未知书名"

        // 获取作者
        val authorNameElement = soup.selectFirst("div.author-name")
        val authorName = authorNameElement?.selectFirst("span.author-name-text")?.text() ?: "未知作者"

        // 获取简介
        val descriptionElement = soup.selectFirst("div.page-abstract-content")
        val description = descriptionElement?.selectFirst("p")?.text() ?: "无简介"

        // 获取封面
        val scriptTag = soup.selectFirst("script[type=application/ld+json]")
        val coverUrl = scriptTag?.let {
            val jsonData = Gson().fromJson(it.data(), JsonObject::class.java)
            jsonData.getAsJsonArray("image")?.let { images ->
                if (images.size() > 0) {
                    images[0].asString
                } else {
                    null
                }
            } ?: jsonData.get("image")?.asString
        }

        return BookInfo(name, authorName, description, coverUrl)
    }

    data class Chapter(val id: String, val title: String, val index: Int, val bookId: String) {
        val content by lazy {
            analyzeContent(id, inputStream)
        }
        val inputStream: InputStream
            get() = downloadContent(id)

    }

    fun extractChapters(bookId: String): List<Chapter> {
        val url = "https://fanqienovel.com/page/$bookId"
        val request = Request.Builder().url(url).headers(getHeaders()).build()

        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: ""
        val soup = Jsoup.parse(html)

        val chapters = mutableListOf<Chapter>()
        val chapterItems = soup.select("div.chapter-item")

        for ((index, item) in chapterItems.withIndex()) {
            val aTag = item.selectFirst("a") ?: continue
            val title = aTag.text().trim()

            chapters.add(
                Chapter(
                    aTag.attr("href").split("/").last(), title, index, bookId
                )
            )
        }

        return chapters
    }


    fun downloadContent(chapterId: String): InputStream {
        val apiUrl = "https://qimao.tutuxka.eu.org/api/fanqie.php?item_ids=$chapterId"
        val request = Request.Builder().url(apiUrl).headers(getHeaders()).build()

        val response = client.newCall(request).execute()
        return response.body?.byteStream() ?: throw IOException("无法下载章节 $chapterId，API 可能已失效或网络错误。")
    }

    fun analyzeContent(chapterId: String, stream: InputStream): String {
        val text = stream.bufferedReader().use { it.readText() }
        val data = Gson().fromJson(text, Map::class.java)

        if (data["code"] as Double == 200.0) {
            val dataMap = data["data"] as? Map<*, *> ?: emptyMap<Any, Any>()
            var content =
                dataMap["content"] as? String ?: throw IOException("无法下载章节 $chapterId，API 可能已失效或网络错误。")

            // 移除HTML标签
            content = content.replace(Regex("<header>.*?</header>"), "").replace(Regex("<footer>.*?</footer>"), "")
                .replace("</?article>".toRegex(), "").replace(Regex("<p idx=\"\\d+\">"), "\n").replace("</p>", "\n")
                .replace(Regex("<[^>]+>"), "").replace("\\u003c", "").replace("\\u003e", "")

            // 处理可能的重复章节标题行
            val title = dataMap["title"] as? String ?: ""
            if (title.isNotEmpty() && content.startsWith(title)) {
                content = content.substring(title.length).trimStart()
            }

            content = content.replace(Regex("\n{2,}"), "\n").trim()
            content = content.split("\n").joinToString("\n") {
                if (it.isNotBlank()) "    $it" else it
            }
            return content
        }

        throw IOException("无法下载章节 $chapterId，API 可能已失效或网络错误。")
    }
}
