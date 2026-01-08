package com.realstate.scraper

import com.microsoft.playwright.*
import jakarta.annotation.PreDestroy
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Component
class PlaywrightBrowserManager {
    private val logger = LoggerFactory.getLogger(PlaywrightBrowserManager::class.java)

    private var playwright: Playwright? = null
    private var browser: Browser? = null
    private val lock = ReentrantLock()

    companion object {
        private const val DEFAULT_TIMEOUT = 60000.0 // 60 seconds
        private const val PAGE_LOAD_WAIT = 5000L // 5 seconds extra wait for JS
        private const val SCROLL_WAIT = 2000L // 2 seconds after scroll
    }

    private fun ensureBrowser(): Browser {
        lock.withLock {
            if (browser == null || !browser!!.isConnected) {
                logger.info("Initializing Playwright browser...")
                playwright = Playwright.create()
                browser = playwright!!.chromium().launch(
                    BrowserType.LaunchOptions()
                        .setHeadless(true)
                        .setArgs(listOf(
                            "--disable-blink-features=AutomationControlled",
                            "--disable-dev-shm-usage",
                            "--no-sandbox",
                            "--disable-setuid-sandbox",
                            "--disable-accelerated-2d-canvas",
                            "--disable-gpu",
                            "--window-size=1920,1080",
                            "--start-maximized",
                            "--disable-infobars",
                            "--disable-extensions",
                            "--disable-plugins-discovery",
                            "--lang=es-ES"
                        ))
                )
                logger.info("Playwright browser initialized successfully")
            }
            return browser!!
        }
    }

    fun fetchPage(url: String, waitForSelector: String? = null): Document? {
        return try {
            val browser = ensureBrowser()

            val context = browser.newContext(
                Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
                    .setViewportSize(1920, 1080)
                    .setLocale("es-ES")
                    .setTimezoneId("Europe/Madrid")
                    .setExtraHTTPHeaders(mapOf(
                        "Accept-Language" to "es-ES,es;q=0.9,en;q=0.8",
                        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
                        "Accept-Encoding" to "gzip, deflate, br",
                        "Connection" to "keep-alive",
                        "Upgrade-Insecure-Requests" to "1",
                        "Sec-Fetch-Dest" to "document",
                        "Sec-Fetch-Mode" to "navigate",
                        "Sec-Fetch-Site" to "none",
                        "Sec-Fetch-User" to "?1",
                        "Cache-Control" to "max-age=0"
                    ))
                    .setJavaScriptEnabled(true)
            )

            context.use { ctx ->
                val page = ctx.newPage()
                page.setDefaultTimeout(DEFAULT_TIMEOUT)

                // Add stealth scripts to evade detection
                page.addInitScript("""
                    // Override webdriver property
                    Object.defineProperty(navigator, 'webdriver', { get: () => undefined });

                    // Override plugins
                    Object.defineProperty(navigator, 'plugins', { get: () => [1, 2, 3, 4, 5] });

                    // Override languages
                    Object.defineProperty(navigator, 'languages', { get: () => ['es-ES', 'es', 'en'] });

                    // Override platform
                    Object.defineProperty(navigator, 'platform', { get: () => 'Win32' });

                    // Remove automation indicators
                    delete window.cdc_adoQpoasnfa76pfcZLmcfl_Array;
                    delete window.cdc_adoQpoasnfa76pfcZLmcfl_Promise;
                    delete window.cdc_adoQpoasnfa76pfcZLmcfl_Symbol;
                """.trimIndent())

                // Navigate to the page
                logger.debug("Navigating to: $url")
                page.navigate(url, Page.NavigateOptions()
                    .setWaitUntil(com.microsoft.playwright.options.WaitUntilState.NETWORKIDLE)
                    .setTimeout(DEFAULT_TIMEOUT))

                // Handle cookie consent dialogs
                handleCookieConsent(page)

                // Wait for page to fully load
                Thread.sleep(PAGE_LOAD_WAIT)

                // Wait for specific selector if provided
                if (waitForSelector != null) {
                    // Try each selector in the comma-separated list
                    val selectors = waitForSelector.split(",").map { it.trim() }
                    var found = false
                    for (selector in selectors) {
                        try {
                            page.waitForSelector(selector, Page.WaitForSelectorOptions().setTimeout(10000.0))
                            logger.debug("Found selector: $selector")
                            found = true
                            break
                        } catch (e: TimeoutError) {
                            logger.debug("Selector not found: $selector")
                        }
                    }
                    if (!found) {
                        logger.warn("None of the selectors found on $url: $waitForSelector")
                    }
                }

                // Scroll down progressively to trigger lazy loading
                scrollPage(page)

                val html = page.content()
                logger.debug("Page HTML length: ${html.length} characters")
                Jsoup.parse(html, url)
            }
        } catch (e: Exception) {
            logger.error("Error fetching page with Playwright: $url - ${e.message}", e)
            null
        }
    }

    private fun handleCookieConsent(page: Page) {
        try {
            // Common cookie consent button selectors
            val consentSelectors = listOf(
                "button#didomi-notice-agree-button",
                "button[id*='accept']",
                "button[class*='accept']",
                "button:has-text('Aceptar')",
                "button:has-text('Acepto')",
                "button:has-text('Accept')",
                "button:has-text('OK')",
                "[data-testid='accept-cookies']",
                ".didomi-continue-without-agreeing",
                "#onetrust-accept-btn-handler"
            )

            for (selector in consentSelectors) {
                try {
                    val element = page.locator(selector).first()
                    if (element.isVisible()) {
                        element.click()
                        logger.debug("Clicked cookie consent button: $selector")
                        Thread.sleep(1000)
                        break
                    }
                } catch (e: Exception) {
                    // Selector not found, try next
                }
            }
        } catch (e: Exception) {
            logger.debug("No cookie consent dialog found or error handling it: ${e.message}")
        }
    }

    private fun scrollPage(page: Page) {
        try {
            // Scroll progressively to trigger lazy loading
            page.evaluate("window.scrollTo(0, document.body.scrollHeight * 0.25)")
            Thread.sleep(500)
            page.evaluate("window.scrollTo(0, document.body.scrollHeight * 0.5)")
            Thread.sleep(500)
            page.evaluate("window.scrollTo(0, document.body.scrollHeight * 0.75)")
            Thread.sleep(500)
            page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
            Thread.sleep(SCROLL_WAIT)
            // Scroll back up to ensure all content is loaded
            page.evaluate("window.scrollTo(0, 0)")
            Thread.sleep(500)
        } catch (e: Exception) {
            logger.debug("Error during scroll: ${e.message}")
        }
    }

    fun fetchPageWithRetry(url: String, waitForSelector: String? = null, maxRetries: Int = 3): Document? {
        repeat(maxRetries) { attempt ->
            val document = fetchPage(url, waitForSelector)
            if (document != null) {
                return document
            }
            logger.warn("Retry ${attempt + 1}/$maxRetries for $url")
            Thread.sleep(2000L * (attempt + 1)) // Exponential backoff
        }
        return null
    }

    @PreDestroy
    fun shutdown() {
        lock.withLock {
            logger.info("Shutting down Playwright browser...")
            try {
                browser?.close()
                playwright?.close()
            } catch (e: Exception) {
                logger.error("Error shutting down Playwright: ${e.message}")
            }
            browser = null
            playwright = null
        }
    }
}
