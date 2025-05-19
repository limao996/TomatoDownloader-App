import RequestHandler.getHeaders
import io.documentnode.epub4j.domain.Author
import io.documentnode.epub4j.domain.Book
import io.documentnode.epub4j.domain.Date
import io.documentnode.epub4j.domain.Resource
import io.documentnode.epub4j.epub.EpubWriter
import kotlinx.coroutines.*
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream


class TomatoEpubBuilder(override val book: TomatoBook) : TomatoBuilder() {
    constructor(bookId: String) : this(TomatoBook(bookId))

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun build(file: File, onBuild: (String) -> Unit) =
        withContext(Dispatchers.IO.limitedParallelism(32)) {
            onBuild("开始写入书籍信息")
            val epub = Book()
            val metadata = epub.metadata

            onBuild("设置书籍标题")
            metadata.addTitle(book.name)
            onBuild("设置书籍作者")
            metadata.addAuthor(Author(book.author))
            onBuild("设置书籍简介")
            metadata.addDescription(book.description)
            onBuild("设置书籍语言")
            metadata.language = "zh-CN"
            onBuild("设置创建时间")
            metadata.addDate(Date(java.util.Date()))
            onBuild("创建封面章节")
            epub.addSection(
                "封面", Resource(makeCoverHtml(book.name, book.author), "cover.xhtml")
            )
            onBuild("获取书籍目录")
            val chapters = book.list()
            onBuild("创建路由章节")
            epub.addSection(
                "目录", Resource(makeNavHtml(chapters.map {
                    it.title
                }), "nav.xhtml")
            )

            onBuild("创建简介章节")
            epub.addSection(
                "简介", Resource(makeIntroHtml(book.name, book.author, book.description), "intro.xhtml")
            )

            book.cover?.let {
                onBuild("获取封面图片")
                val request = Request.Builder().url(it).headers(getHeaders()).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.byteStream()?.also { stream ->
                        onBuild("设置书籍封面")
                        epub.setCoverImage(
                            Resource(
                                stream, "cover.png"
                            )
                        )
                    }
                }
            }

            onBuild("准备下载正文章节")
            val jobs = mutableListOf<Deferred<Pair<String, ChapterData>>>()
            for ((index, chapter) in chapters.withIndex()) {
                jobs.add(async {
                    onBuild("下载正文章节 (${index + 1})")
                    chapter.title to chapter.data
                })
            }

            for ((index, job) in jobs.withIndex()) {
                val (title, data) = job.await()
                epub.addSection(
                    title, Resource(makeContentHtml(title, data).byteInputStream(), "chapter${index + 1}.xhtml")
                )
                onBuild("创建正文章节 (${index + 1})")
            }

            onBuild("创建Epub文件")
            val epubWriter = EpubWriter()
            FileOutputStream(file).use {
                it.buffered(65535).use {
                    epubWriter.write(epub, it)
                }
            }
            onBuild("完成 共${book.chapterCount}章")
        }
}