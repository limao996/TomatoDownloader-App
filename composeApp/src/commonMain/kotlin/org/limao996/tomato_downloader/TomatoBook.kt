class TomatoBook(val bookId: String) {
    val bookInfo by lazy { RequestHandler.getBookInfo(bookId) }
    val name by lazy { bookInfo.name }
    val author by lazy { bookInfo.author }
    val description by lazy { bookInfo.description }
    val cover by lazy { bookInfo.cover }
    var chapterCount: Int? = null

    fun list() = RequestHandler.extractChapters(bookId).apply {
        chapterCount = size
    }
}