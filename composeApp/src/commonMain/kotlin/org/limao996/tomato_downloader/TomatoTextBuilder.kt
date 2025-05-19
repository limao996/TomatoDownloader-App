import kotlinx.coroutines.*
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class TomatoTextBuilder(override val book: TomatoBook) : TomatoBuilder() {
    constructor(bookId: String) : this(TomatoBook(bookId))

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun build(file: File, onBuild: (String) -> Unit) =
        withContext(Dispatchers.IO.limitedParallelism(32)) {
            val fw = FileWriter(file)
            val bw = BufferedWriter(fw, 65535)

            onBuild("写入书籍信息")
            bw.write("书名：${book.name}\n" + "作者：${book.author}\n\n" + "简介：\n${book.description}\n\n\n")

            onBuild("获取书籍目录")
            val chapters = book.list()

            onBuild("准备下载正文章节")
            val jobs = mutableListOf<Deferred<String>>()
            for ((index, chapter) in chapters.withIndex()) {
                val title = if (chapter.title.startsWith("第")) chapter.title else "正文 " + chapter.title
                jobs.add(async {
                    onBuild("下载正文章节 (${index + 1})")
                    "$title\n\n${chapter.data.content}\n\n\n"
                })

            }

            for ((index, job) in jobs.withIndex()) {
                bw.write(job.await())
                onBuild("创建正文章节 (${index + 1})")
            }

            bw.close()
            fw.close()
            onBuild("完成 共${book.chapterCount}章")
        }
}