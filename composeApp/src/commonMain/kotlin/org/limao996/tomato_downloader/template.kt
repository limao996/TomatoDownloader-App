import java.io.ByteArrayInputStream

fun escapeHtml(html: String): String {
    val buffer = StringBuffer()
    for (text in html.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
        .replace("'", "&#39;").split("\n")) {
        if (text.isBlank()) continue
        buffer.append("<p>$text</p>")
    }
    return buffer.toString()
}

fun makeContentHtml(title: String, data: ChapterData): String {
    val title = escapeHtml(title)
    val header = mutableListOf<String>()
    data.wordCount?.let {
        header.add("本章字数：$it")
    }
    data.lastUpdateDate?.let {
        header.add("更新时间：$it")
    }
    val headerFormat = if (header.isNotEmpty()) "<p><small>${header.joinToString("    ")}</small></p>" else ""

    return """<?xml version='1.0' encoding='utf-8'?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" epub:prefix="z3998: http://www.daisy.org/z3998/2012/vocab/structure/#" lang="zh-CN" xml:lang="zh-CN">
    <head>
        <title>$title</title>
    </head>
    <body>
        <h2>$title</h2>
        $headerFormat
        ${escapeHtml(data.content)}
    </body>
</html>""".trimIndent()
}

fun makeCoverHtml(name: String, author: String) = """<?xml version='1.0' encoding='utf-8'?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" epub:prefix="z3998: http://www.daisy.org/z3998/2012/vocab/structure/#" lang="zh-CN" xml:lang="zh-CN">
  <head>
    <title>封面</title>
  </head>
  <body>
    <div class="cover">
      <h1>$name</h1>
      <h2>$author</h2>
      <img class="cover-img" src="cover.png" alt="封面"/>
    </div>
  </body>
</html>""".byteInputStream()

fun makeNavHtml(nav: List<String>): ByteArrayInputStream {
    val buffer = StringBuffer()
    for ((index, title) in nav.withIndex()) {
        buffer.append(
            """
             <li>
                <a href="chapter${index + 1}.xhtml">$title</a>
             </li>
        """
        )
    }
    return """<?xml version='1.0' encoding='utf-8'?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" lang="zh-CN" xml:lang="zh-CN">
  <head>
    <title>目录</title>
  </head>
  <body>
    <nav epub:type="toc" id="id" role="doc-toc">
      <h2>目录</h2>
      <ul>
        <li>
          <a href="cover.xhtml">封面</a>
        </li>
        <li>
          <a href="intro.xhtml">简介</a>
        </li>
        $buffer
      </ul>
    </nav>
  </body>
</html>""".byteInputStream()
}


fun makeIntroHtml(name: String, author: String, description: String) = """<?xml version='1.0' encoding='utf-8'?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" epub:prefix="z3998: http://www.daisy.org/z3998/2012/vocab/structure/#" lang="zh-CN" xml:lang="zh-CN">
  <head>
    <title>简介</title>
  </head>
  <body><h1>$name</h1>
        <h2>作者：$author</h2>
        <div class="description">
            <h3>作品简介</h3>
            ${escapeHtml(description)}
        </div>
        </body>
</html>""".byteInputStream()
