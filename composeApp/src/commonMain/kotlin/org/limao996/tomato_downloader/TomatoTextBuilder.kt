import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class TomatoTextBuilder(override val book: TomatoBook) : TomatoBuilder() {
    constructor(bookId: String) : this(TomatoBook(bookId))

    override suspend fun build(file: File, onBuild: (String, Float) -> Unit) = withContext(Dispatchers.IO) {
        onBuild("写入书籍信息", 0f)
        val fw = FileWriter(file)
        val bw = BufferedWriter(fw, 65535)

        // 写入书籍信息
        bw.write("书名：${book.name}\n" + "作者：${book.author}\n\n" + "简介：\n${book.description}\n\n\n")

        val chapters = book.list()
        onBuild("添加下载任务 (0/${book.chapterCount!!})", 0f)
        val jobs = mutableListOf<Deferred<String>>()
        for ((index, chapter) in chapters.withIndex()) {
            val title = if (chapter.title.startsWith("第")) chapter.title else "正文 " + chapter.title
            jobs.add(async { "$title\n\n${chapter.data.content}\n\n\n" })
            onBuild("添加下载任务 (${index + 1} / ${book.chapterCount!!})", 0f)
        }

        onBuild("写入章节内容", 0f)
        for ((index, job) in jobs.withIndex()) {
            bw.write(job.await())
            onBuild("写入章节内容 (${index + 1} / ${book.chapterCount!!})", (index + 1f) / book.chapterCount!!)
        }
        onBuild("下载完成", 1f)

        bw.close()
        fw.close()
    }
}