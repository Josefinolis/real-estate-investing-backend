package com.realstate.service

import com.realstate.domain.entity.PriceHistory
import com.realstate.domain.entity.Property
import com.realstate.domain.entity.PropertySource
import com.realstate.domain.repository.FavoriteRepository
import com.realstate.domain.repository.PriceHistoryRepository
import com.realstate.domain.repository.PropertyRepository
import com.realstate.dto.*
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Service
class PropertyService(
    private val propertyRepository: PropertyRepository,
    private val priceHistoryRepository: PriceHistoryRepository,
    private val favoriteRepository: FavoriteRepository
) {
    private val logger = LoggerFactory.getLogger(PropertyService::class.java)

    fun searchProperties(filter: SearchFilterDTO): PropertyListDTO {
        val pageable = PageRequest.of(
            filter.page,
            filter.size,
            Sort.by(Sort.Direction.DESC, "lastSeenAt")
        )

        val page = propertyRepository.searchProperties(
            city = filter.city,
            postalCode = filter.postalCode,
            operationType = filter.operationType,
            propertyType = filter.propertyType,
            minPrice = filter.minPrice,
            maxPrice = filter.maxPrice,
            minRooms = filter.minRooms,
            maxRooms = filter.maxRooms,
            minBathrooms = filter.minBathrooms,
            minArea = filter.minArea,
            maxArea = filter.maxArea,
            pageable = pageable
        )

        return PropertyListDTO(
            properties = page.content.map { PropertyDTO.fromEntity(it) },
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            currentPage = page.number
        )
    }

    fun getPropertyById(id: UUID, userId: UUID? = null): PropertyDetailDTO? {
        val property = propertyRepository.findById(id).orElse(null) ?: return null
        val priceHistory = priceHistoryRepository.findByPropertyIdOrderByRecordedAtDesc(id)
        val isFavorite = userId?.let {
            favoriteRepository.existsByUserIdAndPropertyId(it, id)
        } ?: false

        return PropertyDetailDTO(
            property = PropertyDTO.fromEntity(property),
            priceHistory = priceHistory.map {
                PriceHistoryDTO(
                    id = it.id!!,
                    price = it.price,
                    recordedAt = it.recordedAt
                )
            },
            isFavorite = isFavorite
        )
    }

    fun getPriceHistory(propertyId: UUID): List<PriceHistoryDTO> {
        return priceHistoryRepository.findByPropertyIdOrderByRecordedAtDesc(propertyId)
            .map { PriceHistoryDTO(it.id!!, it.price, it.recordedAt) }
    }

    @Transactional
    fun saveOrUpdateProperty(property: Property): Property {
        val existing = propertyRepository.findByExternalIdAndSource(
            property.externalId,
            property.source
        )

        if (existing != null) {
            // Update existing property
            existing.apply {
                title = property.title
                description = property.description
                pricePerM2 = property.pricePerM2
                propertyType = property.propertyType
                operationType = property.operationType
                rooms = property.rooms
                bathrooms = property.bathrooms
                areaM2 = property.areaM2
                address = property.address
                city = property.city
                // Update postalCode if the new value is not null
                if (property.postalCode != null) {
                    postalCode = property.postalCode
                }
                zone = property.zone
                latitude = property.latitude
                longitude = property.longitude
                imageUrls = property.imageUrls
                url = property.url
                isActive = true
                lastSeenAt = LocalDateTime.now()
            }

            // Check for price change
            if (property.price != null && existing.price != property.price) {
                logger.info("Price changed for property ${existing.id}: ${existing.price} -> ${property.price}")
                recordPriceChange(existing, property.price!!)
                existing.price = property.price
            }

            return propertyRepository.save(existing)
        } else {
            // New property
            val saved = propertyRepository.save(property)

            // Record initial price
            if (property.price != null) {
                recordPriceChange(saved, property.price!!)
            }

            logger.info("New property saved: ${saved.id} from ${saved.source}")
            return saved
        }
    }

    private fun recordPriceChange(property: Property, newPrice: BigDecimal) {
        val priceHistory = PriceHistory(
            property = property,
            price = newPrice
        )
        priceHistoryRepository.save(priceHistory)
    }

    fun findNewPropertiesForAlert(
        since: LocalDateTime,
        alert: com.realstate.domain.entity.SearchAlert
    ): List<Property> {
        return propertyRepository.findNewPropertiesMatchingCriteria(
            since = since,
            city = alert.city,
            operationType = alert.operationType,
            propertyType = alert.propertyType,
            minPrice = alert.minPrice,
            maxPrice = alert.maxPrice,
            minRooms = alert.minRooms,
            maxRooms = alert.maxRooms,
            minArea = alert.minArea,
            maxArea = alert.maxArea
        )
    }

    fun findByExternalIdAndSource(externalId: String, source: PropertySource): Property? {
        return propertyRepository.findByExternalIdAndSource(externalId, source)
    }
}
