package com.realstate.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.realstate.domain.entity.ScraperConfig
import com.realstate.domain.entity.ScraperRun
import com.realstate.domain.entity.ScraperRunStatus
import com.realstate.domain.repository.ScraperConfigRepository
import com.realstate.domain.repository.ScraperRunRepository
import com.realstate.dto.*
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ScraperConfigService(
    private val configRepository: ScraperConfigRepository,
    private val runRepository: ScraperRunRepository,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(ScraperConfigService::class.java)

    /**
     * Obtiene la configuración actual del scraper.
     * Si no existe, crea una configuración por defecto.
     */
    fun getConfig(): ScraperConfig {
        return configRepository.findFirstByOrderByUpdatedAtDesc()
            ?: createDefaultConfig()
    }

    fun getConfigDTO(): ScraperConfigDTO {
        return ScraperConfigDTO.fromEntity(getConfig())
    }

    /**
     * Actualiza la configuración del scraper.
     */
    @Transactional
    fun updateConfig(updateDTO: ScraperConfigUpdateDTO): ScraperConfigDTO {
        val existing = configRepository.findFirstByOrderByUpdatedAtDesc()

        val config = if (existing != null) {
            existing.apply {
                cities = updateDTO.cities.toTypedArray()
                operationTypes = updateDTO.operationTypes.toTypedArray()
                propertyTypes = updateDTO.propertyTypes?.toTypedArray()
                minPrice = updateDTO.minPrice
                maxPrice = updateDTO.maxPrice
                minRooms = updateDTO.minRooms
                maxRooms = updateDTO.maxRooms
                minArea = updateDTO.minArea
                maxArea = updateDTO.maxArea
                enabled = updateDTO.enabled
                cronExpression = updateDTO.cronExpression
                sources = updateDTO.sources.toTypedArray()
                updatedAt = LocalDateTime.now()
            }
            configRepository.save(existing)
        } else {
            val newConfig = ScraperConfig(
                cities = updateDTO.cities.toTypedArray(),
                operationTypes = updateDTO.operationTypes.toTypedArray(),
                propertyTypes = updateDTO.propertyTypes?.toTypedArray(),
                minPrice = updateDTO.minPrice,
                maxPrice = updateDTO.maxPrice,
                minRooms = updateDTO.minRooms,
                maxRooms = updateDTO.maxRooms,
                minArea = updateDTO.minArea,
                maxArea = updateDTO.maxArea,
                enabled = updateDTO.enabled,
                cronExpression = updateDTO.cronExpression,
                sources = updateDTO.sources.toTypedArray()
            )
            configRepository.save(newConfig)
        }

        logger.info("Scraper configuration updated: enabled=${config.enabled}, cities=${config.cities.size}")
        return ScraperConfigDTO.fromEntity(config)
    }

    /**
     * Crea una configuración por defecto.
     */
    private fun createDefaultConfig(): ScraperConfig {
        val config = ScraperConfig()
        return configRepository.save(config)
    }

    /**
     * Obtiene el historial de ejecuciones paginado.
     */
    fun getRunHistory(page: Int, size: Int): ScraperRunListDTO {
        val pageable = PageRequest.of(page, size.coerceIn(1, 100))
        val pageResult = runRepository.findAllByOrderByStartedAtDesc(pageable)

        return ScraperRunListDTO(
            runs = pageResult.content.map { ScraperRunDTO.fromEntity(it) },
            totalElements = pageResult.totalElements,
            totalPages = pageResult.totalPages,
            currentPage = pageResult.number
        )
    }

    /**
     * Obtiene la última ejecución.
     */
    fun getLastRun(): ScraperRunDTO? {
        return runRepository.findFirstByOrderByStartedAtDesc()?.let {
            ScraperRunDTO.fromEntity(it)
        }
    }

    /**
     * Verifica si hay una ejecución en curso.
     */
    fun isRunning(): Boolean {
        return runRepository.existsByStatus(ScraperRunStatus.RUNNING)
    }

    /**
     * Obtiene el estado actual del scraper.
     */
    fun getStatus(): ScraperStatusDTO {
        return ScraperStatusDTO(
            isRunning = isRunning(),
            lastRun = getLastRun(),
            config = getConfigDTO()
        )
    }

    /**
     * Inicia una nueva ejecución y devuelve el registro.
     */
    @Transactional
    fun startRun(): ScraperRun {
        val config = getConfig()
        val filtersJson = try {
            objectMapper.writeValueAsString(
                mapOf(
                    "cities" to config.cities.toList(),
                    "operationTypes" to config.operationTypes.toList(),
                    "propertyTypes" to config.propertyTypes?.toList(),
                    "minPrice" to config.minPrice,
                    "maxPrice" to config.maxPrice,
                    "minRooms" to config.minRooms,
                    "maxRooms" to config.maxRooms,
                    "sources" to config.sources.toList()
                )
            )
        } catch (e: Exception) {
            null
        }

        val run = ScraperRun(
            filtersUsed = filtersJson
        )
        return runRepository.save(run)
    }

    /**
     * Actualiza una ejecución en curso.
     */
    @Transactional
    fun updateRun(run: ScraperRun): ScraperRun {
        return runRepository.save(run)
    }

    /**
     * Finaliza una ejecución con éxito.
     */
    @Transactional
    fun completeRun(run: ScraperRun): ScraperRun {
        run.markCompleted()
        return runRepository.save(run)
    }

    /**
     * Finaliza una ejecución con error.
     */
    @Transactional
    fun failRun(run: ScraperRun, message: String, details: String? = null): ScraperRun {
        run.markFailed(message, details)
        return runRepository.save(run)
    }
}
