package com.realstate.scraper

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

@Component
class RateLimiter(
    @Value("\${scraper.rate-limit.requests-per-minute:10}")
    private val requestsPerMinute: Int,

    @Value("\${scraper.rate-limit.cooldown-ms:6000}")
    private val cooldownMs: Long
) {
    private val logger = LoggerFactory.getLogger(RateLimiter::class.java)
    private val semaphore = Semaphore(1)

    @Volatile
    private var lastRequestTime: Long = 0

    fun acquire() {
        try {
            semaphore.acquire()

            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTime
            val minInterval = 60_000L / requestsPerMinute

            if (elapsed < minInterval) {
                val waitTime = minInterval - elapsed
                logger.debug("Rate limiting: waiting ${waitTime}ms")
                Thread.sleep(waitTime)
            }

            lastRequestTime = System.currentTimeMillis()
        } finally {
            semaphore.release()
        }
    }

    fun acquireWithCooldown() {
        acquire()
        Thread.sleep(cooldownMs)
    }

    fun tryAcquire(timeout: Long, unit: TimeUnit): Boolean {
        return try {
            if (semaphore.tryAcquire(timeout, unit)) {
                val now = System.currentTimeMillis()
                val elapsed = now - lastRequestTime
                val minInterval = 60_000L / requestsPerMinute

                if (elapsed < minInterval) {
                    val waitTime = minInterval - elapsed
                    Thread.sleep(waitTime)
                }

                lastRequestTime = System.currentTimeMillis()
                semaphore.release()
                true
            } else {
                false
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
    }
}
