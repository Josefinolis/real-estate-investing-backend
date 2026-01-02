package com.realstate.scraper

import com.realstate.domain.entity.Property
import com.realstate.domain.entity.PropertySource
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

abstract class BaseScraper(
    protected val rateLimiter: RateLimiter
) {
    protected val logger = LoggerFactory.getLogger(this::class.java)

    protected val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
                .build()
            chain.proceed(request)
        }
        .build()

    abstract val source: PropertySource
    abstract val baseUrl: String

    abstract fun scrape(): List<Property>

    protected fun fetchDocument(url: String): Document? {
        rateLimiter.acquire()

        return try {
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()?.let { html ->
                        Jsoup.parse(html, url)
                    }
                } else {
                    logger.warn("Failed to fetch $url: ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("Error fetching $url: ${e.message}", e)
            null
        }
    }

    protected fun extractPrice(text: String?): java.math.BigDecimal? {
        if (text.isNullOrBlank()) return null

        return try {
            val cleanedText = text
                .replace(".", "")
                .replace(",", ".")
                .replace("€", "")
                .replace("/mes", "")
                .replace(" ", "")
                .trim()

            val numberMatch = Regex("\\d+\\.?\\d*").find(cleanedText)
            numberMatch?.value?.toBigDecimalOrNull()
        } catch (e: Exception) {
            null
        }
    }

    protected fun extractNumber(text: String?): Int? {
        if (text.isNullOrBlank()) return null

        return try {
            Regex("\\d+").find(text)?.value?.toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }

    protected fun extractArea(text: String?): java.math.BigDecimal? {
        if (text.isNullOrBlank()) return null

        return try {
            val cleanedText = text
                .replace(".", "")
                .replace(",", ".")
                .replace("m²", "")
                .replace("m2", "")
                .replace(" ", "")
                .trim()

            Regex("\\d+\\.?\\d*").find(cleanedText)?.value?.toBigDecimalOrNull()
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}
