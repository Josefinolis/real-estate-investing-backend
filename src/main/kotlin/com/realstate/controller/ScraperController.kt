package com.realstate.controller

import com.realstate.dto.*
import com.realstate.scraper.ScraperScheduler
import com.realstate.service.ScraperConfigService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/scraper")
class ScraperController(
    private val scraperConfigService: ScraperConfigService,
    private val scraperScheduler: ScraperScheduler
) {

    /**
     * Obtiene la configuración actual del scraper.
     */
    @GetMapping("/config")
    fun getConfig(): ResponseEntity<ScraperConfigDTO> {
        val config = scraperConfigService.getConfigDTO()
        return ResponseEntity.ok(config)
    }

    /**
     * Actualiza la configuración del scraper.
     */
    @PutMapping("/config")
    fun updateConfig(
        @RequestBody updateDTO: ScraperConfigUpdateDTO
    ): ResponseEntity<ScraperConfigDTO> {
        val config = scraperConfigService.updateConfig(updateDTO)
        return ResponseEntity.ok(config)
    }

    /**
     * Obtiene el historial de ejecuciones del scraper.
     */
    @GetMapping("/runs")
    fun getRunHistory(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ScraperRunListDTO> {
        val history = scraperConfigService.getRunHistory(page, size)
        return ResponseEntity.ok(history)
    }

    /**
     * Obtiene la última ejecución del scraper.
     */
    @GetMapping("/runs/last")
    fun getLastRun(): ResponseEntity<ScraperRunDTO?> {
        val lastRun = scraperConfigService.getLastRun()
        return ResponseEntity.ok(lastRun)
    }

    /**
     * Obtiene el estado actual del scraper (running/idle + última ejecución + config).
     */
    @GetMapping("/status")
    fun getStatus(): ResponseEntity<ScraperStatusDTO> {
        val status = scraperConfigService.getStatus()
        return ResponseEntity.ok(status)
    }

    /**
     * Ejecuta el scraper manualmente.
     */
    @PostMapping("/run")
    fun triggerRun(): ResponseEntity<Map<String, Any>> {
        return if (scraperConfigService.isRunning()) {
            ResponseEntity.badRequest().body(
                mapOf(
                    "success" to false,
                    "message" to "Ya hay una ejecución en curso"
                )
            )
        } else {
            // Ejecutar en un hilo separado para no bloquear la respuesta
            Thread {
                scraperScheduler.scrapeAll()
            }.start()

            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Scraping iniciado"
                )
            )
        }
    }

    /**
     * Obtiene las ciudades disponibles para configurar.
     */
    @GetMapping("/cities")
    fun getAvailableCities(): ResponseEntity<List<String>> {
        val cities = listOf(
            "Madrid", "Barcelona", "Valencia", "Sevilla", "Zaragoza", "Málaga", "Murcia",
            "Palma de Mallorca", "Las Palmas de Gran Canaria", "Bilbao", "Alicante", "Córdoba",
            "Valladolid", "Vigo", "Gijón", "L'Hospitalet de Llobregat", "Vitoria-Gasteiz",
            "A Coruña", "Granada", "Elche", "Oviedo", "Santa Cruz de Tenerife", "Badalona",
            "Cartagena", "Terrassa", "Jerez de la Frontera", "Sabadell", "Móstoles",
            "Alcalá de Henares", "Pamplona", "Almería", "San Sebastián", "Santander",
            "Burgos", "Albacete", "Castellón de la Plana", "Logroño", "Badajoz", "Salamanca",
            "Huelva", "Lleida", "Tarragona", "León", "Cádiz", "Jaén", "Ourense", "Lugo",
            "Girona", "Cáceres", "Guadalajara", "Toledo", "Pontevedra", "Palencia",
            "Ciudad Real", "Zamora", "Ávila", "Cuenca", "Huesca", "Segovia", "Soria",
            "Teruel", "Ceuta", "Melilla"
        )
        return ResponseEntity.ok(cities)
    }

    /**
     * Obtiene los tipos de propiedad disponibles.
     */
    @GetMapping("/property-types")
    fun getPropertyTypes(): ResponseEntity<List<String>> {
        val types = listOf(
            "PISO", "APARTAMENTO", "CASA", "CHALET", "DUPLEX", "ATICO", "ESTUDIO", "LOFT", "OTRO"
        )
        return ResponseEntity.ok(types)
    }

    /**
     * Obtiene las opciones de frecuencia disponibles.
     */
    @GetMapping("/frequencies")
    fun getFrequencies(): ResponseEntity<List<Map<String, String>>> {
        val frequencies = listOf(
            mapOf("label" to "Cada 15 minutos", "cron" to "0 */15 * * * *"),
            mapOf("label" to "Cada 30 minutos", "cron" to "0 */30 * * * *"),
            mapOf("label" to "Cada hora", "cron" to "0 0 * * * *"),
            mapOf("label" to "Cada 2 horas", "cron" to "0 0 */2 * * *"),
            mapOf("label" to "Cada 6 horas", "cron" to "0 0 */6 * * *"),
            mapOf("label" to "Cada 12 horas", "cron" to "0 0 */12 * * *"),
            mapOf("label" to "Una vez al día", "cron" to "0 0 8 * * *")
        )
        return ResponseEntity.ok(frequencies)
    }
}
