import RequestHandler.getHeaders
import io.documentnode.epub4j.domain.Author
import io.documentnode.epub4j.domain.Book
import io.documentnode.epub4j.domain.Resource
import io.documentnode.epub4j.epub.EpubWriter
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream


class TomatoEpubBuilder(override val book: TomatoBook) : TomatoBuilder() {
    constructor(bookId: String) : this(TomatoBook(bookId))

    override suspend fun build(file: File, onBuild: (String, Float) -> Unit) = withContext(Dispatchers.IO) {
        onBuild("写入书籍信息", 0f)
        val epub = Book()
        val metadata = epub.metadata

        metadata.addTitle(book.name)
        metadata.addAuthor(Author(book.author))
        metadata.addDescription(book.description)

        epub.addSection(
            "封面", Resource(makeCoverHtml(book.name, book.author), "cover.xhtml")
        )
        val chapters = book.list()
        epub.addSection(
            "目录", Resource(makeNavHtml(chapters.map {
                it.title
            }), "nav.xhtml")
        )

        epub.addSection(
            "简介", Resource(makeIntroHtml(book.name, book.author, book.description), "intro.xhtml")
        )

        book.cover?.let {
            onBuild("写入书籍封面", 0f)
            val request = Request.Builder().url(it).headers(getHeaders()).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.byteStream()?.also { stream ->
                    epub.setCoverImage(
                        Resource(
                            stream, "cover.png"
                        )
                    )
                }
            }
        }

        onBuild("添加下载任务 (0 / ${book.chapterCount!!})", 0f)
        val jobs = mutableListOf<Deferred<Pair<String, TomatoChapterDownloader.ChapterData>>>()
        for ((index, chapter) in chapters.withIndex()) {
            jobs.add(async { chapter.title to chapter.data })
            onBuild("添加下载任务 (${index + 1} / ${book.chapterCount!!})", 0f)
        }

        onBuild("写入章节内容", 0f)
        for ((index, job) in jobs.withIndex()) {
            val (title, data) = job.await()
            epub.addSection(
                title, Resource(makeContentHtml(title, data).byteInputStream(), "chapter${index + 1}.xhtml")
            )
            onBuild("写入章节内容 (${index + 1} / ${book.chapterCount!!})", (index + 1f) / book.chapterCount!!)
        }

        onBuild("创建Epub文件", 1f)
        val epubWriter = EpubWriter()
        FileOutputStream(file).use {
            it.buffered(65535).use {
                epubWriter.write(epub, it)
            }
        }
        onBuild("下载完成", 1f)
    }
}