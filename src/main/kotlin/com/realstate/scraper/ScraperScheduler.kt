package com.realstate.scraper

import com.realstate.domain.entity.PropertySource
import com.realstate.domain.entity.ScraperRun
import com.realstate.service.AlertService
import com.realstate.service.PropertyService
import com.realstate.service.ScraperConfigService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ScraperScheduler(
    private val idealistaScraper: IdealistaScraper,
    private val fotocasaScraper: FotocasaScraper,
    private val pisosComScraper: PisosComScraper,
    private val propertyService: PropertyService,
    private val alertService: AlertService,
    private val scraperConfigService: ScraperConfigService
) {
    private val logger = LoggerFactory.getLogger(ScraperScheduler::class.java)

    private val scrapersMap: Map<String, BaseScraper> by lazy {
        mapOf(
            "IDEALISTA" to idealistaScraper,
            "PISOSCOM" to pisosComScraper,
            "FOTOCASA" to fotocasaScraper
        )
    }

    @Scheduled(cron = "\${scraper.schedule.cron:0 */30 * * * *}")
    fun runScheduledScraping() {
        val config = scraperConfigService.getConfig()

        if (!config.enabled) {
            logger.info("Scraper is disabled, skipping scheduled run")
            return
        }

        logger.info("Starting scheduled scraping job")
        scrapeAll()
        logger.info("Scheduled scraping job completed")
    }

    fun scrapeAll() {
        val config = scraperConfigService.getConfig()

        // Verificar si ya hay una ejecución en curso
        if (scraperConfigService.isRunning()) {
            logger.warn("Scraper already running, skipping")
            return
        }

        // Iniciar registro de ejecución
        var run = scraperConfigService.startRun()
        logger.info("Started scraper run: ${run.id}")

        var totalProperties = 0
        var newProperties = 0
        var updatedProperties = 0
        var priceChanges = 0
        var idealistaCount = 0
        var pisoscomCount = 0
        var fotocasaCount = 0

        try {
            // Obtener scrapers activos según configuración
            val activeScrapers = config.sources.mapNotNull { scrapersMap[it] }

            if (activeScrapers.isEmpty()) {
                logger.warn("No active scrapers configured")
                run = scraperConfigService.failRun(run, "No hay fuentes activas configuradas")
                return
            }

            for (scraper in activeScrapers) {
                try {
                    logger.info("Starting scraper: ${scraper.source}")

                    // Pasar configuración al scraper
                    val properties = scraper.scrapeWithConfig(
                        cities = config.cities.toList(),
                        operationTypes = config.operationTypes.toList()
                    )

                    val scraperCount = properties.size
                    totalProperties += scraperCount

                    // Actualizar contador por fuente
                    when (scraper.source) {
                        PropertySource.IDEALISTA -> idealistaCount = scraperCount
                        PropertySource.PISOSCOM -> pisoscomCount = scraperCount
                        PropertySource.FOTOCASA -> fotocasaCount = scraperCount
                    }

                    for (property in properties) {
                        try {
                            // Aplicar filtros de post-procesamiento
                            if (!passesFilters(property, config)) {
                                continue
                            }

                            val existingProperty = propertyService.findByExternalIdAndSource(
                                property.externalId,
                                property.source
                            )

                            val saved = propertyService.saveOrUpdateProperty(property)

                            if (existingProperty == null) {
                                newProperties++
                            } else {
                                updatedProperties++
                                // Verificar cambio de precio
                                if (existingProperty.price != null && property.price != null &&
                                    existingProperty.price != property.price) {
                                    priceChanges++
                                }
                            }
                        } catch (e: Exception) {
                            logger.error("Error saving property ${property.externalId}: ${e.message}")
                        }
                    }

                    logger.info("Completed scraper ${scraper.source}: $scraperCount properties")
                } catch (e: Exception) {
                    logger.error("Error running scraper ${scraper.source}: ${e.message}", e)
                }
            }

            // Actualizar métricas del run
            run.totalPropertiesFound = totalProperties
            run.newProperties = newProperties
            run.updatedProperties = updatedProperties
            run.priceChanges = priceChanges
            run.idealistaCount = idealistaCount
            run.pisoscomCount = pisoscomCount
            run.fotocasaCount = fotocasaCount

            run = scraperConfigService.completeRun(run)
            logger.info("Scraping completed. Total: $totalProperties, New: $newProperties, Updated: $updatedProperties, Price changes: $priceChanges")

            // Check for matches with alerts
            try {
                alertService.checkNewMatchesAndNotify()
            } catch (e: Exception) {
                logger.error("Error checking alerts: ${e.message}", e)
            }

        } catch (e: Exception) {
            logger.error("Error during scraping: ${e.message}", e)
            scraperConfigService.failRun(run, e.message ?: "Error desconocido", e.stackTraceToString())
        }
    }

    /**
     * Verifica si una propiedad pasa los filtros configurados.
     */
    private fun passesFilters(
        property: com.realstate.domain.entity.Property,
        config: com.realstate.domain.entity.ScraperConfig
    ): Boolean {
        // Filtro de precio mínimo
        config.minPrice?.let { minPrice ->
            if (property.price == null || property.price!! < minPrice) {
                return false
            }
        }

        // Filtro de precio máximo
        config.maxPrice?.let { maxPrice ->
            if (property.price == null || property.price!! > maxPrice) {
                return false
            }
        }

        // Filtro de habitaciones mínimas
        config.minRooms?.let { minRooms ->
            if (property.rooms == null || property.rooms!! < minRooms) {
                return false
            }
        }

        // Filtro de habitaciones máximas
        config.maxRooms?.let { maxRooms ->
            if (property.rooms == null || property.rooms!! > maxRooms) {
                return false
            }
        }

        // Filtro de área mínima
        config.minArea?.let { minArea ->
            if (property.areaM2 == null || property.areaM2!! < minArea) {
                return false
            }
        }

        // Filtro de área máxima
        config.maxArea?.let { maxArea ->
            if (property.areaM2 == null || property.areaM2!! > maxArea) {
                return false
            }
        }

        // Filtro de tipos de propiedad
        config.propertyTypes?.let { types ->
            if (property.propertyType == null ||
                !types.contains(property.propertyType!!.name)) {
                return false
            }
        }

        return true
    }

    fun scrapeSource(source: String) {
        val scraper = scrapersMap[source.uppercase()]

        if (scraper == null) {
            logger.warn("Unknown scraper source: $source")
            return
        }

        val config = scraperConfigService.getConfig()

        logger.info("Running single scraper: ${scraper.source}")
        val properties = scraper.scrapeWithConfig(
            cities = config.cities.toList(),
            operationTypes = config.operationTypes.toList()
        )

        for (property in properties) {
            try {
                if (passesFilters(property, config)) {
                    propertyService.saveOrUpdateProperty(property)
                }
            } catch (e: Exception) {
                logger.error("Error saving property ${property.externalId}: ${e.message}")
            }
        }

        logger.info("Single scraper completed: ${properties.size} properties")
    }
}
