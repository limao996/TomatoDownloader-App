import java.io.File

abstract class TomatoBuilder {
    abstract val book: TomatoBook
    abstract suspend fun build(file: File, onBuild: (String, Float) -> Unit)
    suspend fun build(path: String, onBuild: (String, Float) -> Unit) = build(File(path), onBuild)
}


suspend fun buildTextBook(book: TomatoBook, file: File, onBuild: (String, Float) -> Unit) =
    TomatoTextBuilder(book).apply { build(file, onBuild) }


suspend fun buildEpubBook(book: TomatoBook, file: File, onBuild: (String, Float) -> Unit) =
    TomatoEpubBuilder(book).apply { build(file, onBuild) }

suspend fun buildTomatoBook(
    book: TomatoBook, file: File, isEpub: Boolean = false, onBuild: (String, Float) -> Unit
) = if (isEpub) buildEpubBook(book, file, onBuild)
else buildTextBook(book, file, onBuild)


suspend fun buildTextBook(bookId: String, file: File, onBuild: (String, Float) -> Unit) =
    TomatoTextBuilder(bookId).apply { build(file, onBuild) }


suspend fun buildEpubBook(bookId: String, file: File, onBuild: (String, Float) -> Unit) =
    TomatoEpubBuilder(bookId).apply { build(file, onBuild) }

suspend fun buildTomatoBook(
    bookId: String, file: File, isEpub: Boolean = false, onBuild: (String, Float) -> Unit
) = if (isEpub) buildEpubBook(bookId, file, onBuild)
else buildTextBook(bookId, file, onBuild)


suspend fun buildTextBook(bookId: String, path: String, onBuild: (String, Float) -> Unit) =
    buildTextBook(bookId, File(path), onBuild)


suspend fun buildEpubBook(bookId: String, path: String, onBuild: (String, Float) -> Unit) =
    buildEpubBook(bookId, File(path), onBuild)

suspend fun buildTomatoBook(
    bookId: String, path: String, isEpub: Boolean = false, onBuild: (String, Float) -> Unit
) = buildTomatoBook(bookId, File(path), isEpub, onBuild)


suspend fun buildTextBook(book: TomatoBook, path: String, onBuild: (String, Float) -> Unit) =
    buildTextBook(book, File(path), onBuild)


suspend fun buildEpubBook(book: TomatoBook, path: String, onBuild: (String, Float) -> Unit) =
    buildEpubBook(book, File(path), onBuild)

suspend fun buildTomatoBook(
    book: TomatoBook, path: String, isEpub: Boolean = false, onBuild: (String, Float) -> Unit
) = buildTomatoBook(book, File(path), isEpub, onBuild)