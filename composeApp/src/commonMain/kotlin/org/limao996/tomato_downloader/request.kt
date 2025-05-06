import UserAgentGenerator.randomUserAgent
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONPath
import okhttp3.*
import okhttp3.Headers.Companion.toHeaders
import org.jsoup.Jsoup
import java.io.IOException
import java.net.Proxy
import java.util.concurrent.TimeUnit
import kotlin.random.Random


val client = OkHttpClient.Builder().proxy(Proxy.NO_PROXY).hostnameVerifier { hostname, session -> true }
    .connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).cookieJar(object : CookieJar {
        private val cookieStore = mutableMapOf<String, List<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }
    }).build()

object RequestHandler {
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

            val jsonData = JSONPath.extract(it.data(), "$.image")
            when (jsonData) {
                is String -> jsonData
                is JSONArray -> jsonData.first() as? String
                is List<*> -> jsonData.first() as? String
                is Array<*> -> jsonData.first() as? String
                else -> null
            }
        }

        return BookInfo(name, authorName, description, coverUrl)
    }

    data class Chapter(val id: String, val title: String, val index: Int, val bookId: String) {
        val chapterDownloader = TomatoChapterDownloader(id)
        val data by lazy {
            chapterDownloader.downloadChapter()
        }
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
}
