package com.realstate.scraper

import com.realstate.domain.entity.OperationType
import com.realstate.domain.entity.Property
import com.realstate.domain.entity.PropertySource
import com.realstate.domain.entity.PropertyType
import org.jsoup.nodes.Element
import org.springframework.stereotype.Component

@Component
class IdealistaScraper(
    rateLimiter: RateLimiter
) : BaseScraper(rateLimiter) {

    override val source = PropertySource.IDEALISTA
    override val baseUrl = "https://www.idealista.com"

    // Default search URLs - can be configured
    private val searchUrls = listOf(
        "$baseUrl/venta-viviendas/madrid-madrid/",
        "$baseUrl/alquiler-viviendas/madrid-madrid/",
        "$baseUrl/venta-viviendas/barcelona-barcelona/",
        "$baseUrl/alquiler-viviendas/barcelona-barcelona/"
    )

    override fun scrape(): List<Property> {
        val properties = mutableListOf<Property>()

        for (searchUrl in searchUrls) {
            try {
                logger.info("Scraping Idealista: $searchUrl")
                val pageProperties = scrapeSearchPage(searchUrl)
                properties.addAll(pageProperties)
                logger.info("Found ${pageProperties.size} properties from $searchUrl")
            } catch (e: Exception) {
                logger.error("Error scraping $searchUrl: ${e.message}", e)
            }
        }

        return properties
    }

    private fun scrapeSearchPage(url: String): List<Property> {
        val document = fetchDocument(url) ?: return emptyList()
        val properties = mutableListOf<Property>()

        val operationType = if (url.contains("alquiler")) OperationType.ALQUILER else OperationType.VENTA
        val city = extractCityFromUrl(url)

        val items = document.select("article.item")

        for (item in items) {
            try {
                val property = parsePropertyItem(item, operationType, city)
                if (property != null) {
                    properties.add(property)
                }
            } catch (e: Exception) {
                logger.warn("Error parsing property item: ${e.message}")
            }
        }

        return properties
    }

    private fun parsePropertyItem(item: Element, operationType: OperationType, city: String): Property? {
        val linkElement = item.selectFirst("a.item-link") ?: return null
        val href = linkElement.attr("href")
        val externalId = extractIdFromUrl(href) ?: return null

        val title = item.selectFirst("a.item-link")?.text()?.trim()
        val priceText = item.selectFirst(".item-price")?.text()
        val price = extractPrice(priceText)

        val detailItems = item.select(".item-detail")
        var rooms: Int? = null
        var area: java.math.BigDecimal? = null
        var bathrooms: Int? = null

        for (detail in detailItems) {
            val text = detail.text().lowercase()
            when {
                text.contains("hab") -> rooms = extractNumber(text)
                text.contains("m²") || text.contains("m2") -> area = extractArea(text)
                text.contains("baño") -> bathrooms = extractNumber(text)
            }
        }

        val address = item.selectFirst(".item-detail-char .item-link")?.text()?.trim()
        val zone = item.selectFirst(".item-detail-char .item-link + span")?.text()?.trim()

        val imageUrl = item.selectFirst("img.item-gallery")?.attr("src")
        val imageUrls = if (imageUrl != null) arrayOf(imageUrl) else null

        val propertyType = inferPropertyType(title)

        return Property(
            externalId = externalId,
            source = PropertySource.IDEALISTA,
            title = title,
            price = price,
            operationType = operationType,
            propertyType = propertyType,
            rooms = rooms,
            bathrooms = bathrooms,
            areaM2 = area,
            address = address,
            city = city,
            zone = zone,
            imageUrls = imageUrls,
            url = if (href.startsWith("http")) href else "$baseUrl$href"
        )
    }

    private fun extractIdFromUrl(url: String): String? {
        val regex = Regex("/inmueble/(\\d+)/")
        return regex.find(url)?.groupValues?.get(1)
    }

    private fun extractCityFromUrl(url: String): String {
        return when {
            url.contains("madrid") -> "Madrid"
            url.contains("barcelona") -> "Barcelona"
            url.contains("valencia") -> "Valencia"
            url.contains("sevilla") -> "Sevilla"
            url.contains("bilbao") -> "Bilbao"
            else -> "España"
        }
    }

    private fun inferPropertyType(title: String?): PropertyType {
        if (title == null) return PropertyType.OTRO

        val lowerTitle = title.lowercase()
        return when {
            lowerTitle.contains("piso") -> PropertyType.PISO
            lowerTitle.contains("apartamento") -> PropertyType.APARTAMENTO
            lowerTitle.contains("chalet") -> PropertyType.CHALET
            lowerTitle.contains("casa") -> PropertyType.CASA
            lowerTitle.contains("ático") || lowerTitle.contains("atico") -> PropertyType.ATICO
            lowerTitle.contains("dúplex") || lowerTitle.contains("duplex") -> PropertyType.DUPLEX
            lowerTitle.contains("estudio") -> PropertyType.ESTUDIO
            lowerTitle.contains("loft") -> PropertyType.LOFT
            else -> PropertyType.OTRO
        }
    }
}
