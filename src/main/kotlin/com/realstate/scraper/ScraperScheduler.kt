package com.realstate.scraper

import com.realstate.service.AlertService
import com.realstate.service.PropertyService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ScraperScheduler(
    private val idealistaScraper: IdealistaScraper,
    private val fotocasaScraper: FotocasaScraper,
    private val pisosComScraper: PisosComScraper,
    private val propertyService: PropertyService,
    private val alertService: AlertService,
    @Value("\${scraper.enabled:true}")
    private val scraperEnabled: Boolean
) {
    private val logger = LoggerFactory.getLogger(ScraperScheduler::class.java)

    private val scrapers: List<BaseScraper> by lazy {
        listOf(idealistaScraper, fotocasaScraper, pisosComScraper)
    }

    @Scheduled(cron = "\${scraper.schedule.cron:0 */30 * * * *}")
    fun runScheduledScraping() {
        if (!scraperEnabled) {
            logger.info("Scraper is disabled, skipping scheduled run")
            return
        }

        logger.info("Starting scheduled scraping job")
        scrapeAll()
        logger.info("Scheduled scraping job completed")
    }

    fun scrapeAll() {
        var totalProperties = 0
        var newProperties = 0

        for (scraper in scrapers) {
            try {
                logger.info("Starting scraper: ${scraper.source}")
                val properties = scraper.scrape()
                totalProperties += properties.size

                for (property in properties) {
                    try {
                        val saved = propertyService.saveOrUpdateProperty(property)
                        if (saved.firstSeenAt == saved.lastSeenAt) {
                            newProperties++
                        }
                    } catch (e: Exception) {
                        logger.error("Error saving property ${property.externalId}: ${e.message}")
                    }
                }

                logger.info("Completed scraper ${scraper.source}: ${properties.size} properties")
            } catch (e: Exception) {
                logger.error("Error running scraper ${scraper.source}: ${e.message}", e)
            }
        }

        logger.info("Scraping completed. Total: $totalProperties, New: $newProperties")

        // Check for matches with alerts
        try {
            alertService.checkNewMatchesAndNotify()
        } catch (e: Exception) {
            logger.error("Error checking alerts: ${e.message}", e)
        }
    }

    fun scrapeSource(source: String) {
        val scraper = scrapers.find { it.source.name.equals(source, ignoreCase = true) }

        if (scraper == null) {
            logger.warn("Unknown scraper source: $source")
            return
        }

        logger.info("Running single scraper: ${scraper.source}")
        val properties = scraper.scrape()

        for (property in properties) {
            try {
                propertyService.saveOrUpdateProperty(property)
            } catch (e: Exception) {
                logger.error("Error saving property ${property.externalId}: ${e.message}")
            }
        }

        logger.info("Single scraper completed: ${properties.size} properties")
    }
}
