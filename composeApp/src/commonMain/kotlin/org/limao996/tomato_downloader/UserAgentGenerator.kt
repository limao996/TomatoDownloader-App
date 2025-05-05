import kotlin.random.Random

object UserAgentGenerator {
    private val desktopUserAgents = listOf(
        // Chrome
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%d.0.%d.%d Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%d.0.%d.%d Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%d.0.%d.%d Safari/537.36",

        // Firefox
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:%d.0) Gecko/20100101 Firefox/%d.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:%d.0) Gecko/20100101 Firefox/%d.0",
        "Mozilla/5.0 (X11; Linux x86_64; rv:%d.0) Gecko/20100101 Firefox/%d.0",

        // Safari
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/%d.0 Safari/605.1.15",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/%d.0 Mobile/15E148 Safari/604.1"
    )

    private val mobileUserAgents = listOf(
        // Android
        "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%d.0.%d.%d Mobile Safari/537.36",
        "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%d.0.%d.%d Mobile Safari/537.36",

        // iOS
        "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/%d.0 Mobile/15E148 Safari/604.1",
        "Mozilla/5.0 (iPad; CPU OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/%d.0 Mobile/15E148 Safari/604.1"
    )

    fun randomUserAgent(isMobile: Boolean? = null): String {
        val useMobile = isMobile ?: Random.nextBoolean()
        val template = if (useMobile) {
            mobileUserAgents.random()
        } else {
            desktopUserAgents.random()
        }

        return when {
            template.contains("Chrome") -> {
                val major = Random.nextInt(90, 120)  // Chrome 90-119
                val build = Random.nextInt(1000, 9999)
                val patch = Random.nextInt(100)
                template.format(major, build, patch)
            }
            template.contains("Firefox") -> {
                val version = Random.nextInt(90, 120)  // Firefox 90-119
                template.format(version, version)
            }
            template.contains("Safari") || template.contains("Version") -> {
                val version = Random.nextInt(13, 16)  // Safari 13-15
                template.format(version)
            }
            else -> template
        }
    }
}