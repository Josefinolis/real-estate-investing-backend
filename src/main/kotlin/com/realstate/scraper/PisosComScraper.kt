package com.realstate.scraper

import com.realstate.domain.entity.OperationType
import com.realstate.domain.entity.Property
import com.realstate.domain.entity.PropertySource
import com.realstate.domain.entity.PropertyType
import org.jsoup.nodes.Element
import org.springframework.stereotype.Component

@Component
class PisosComScraper(
    rateLimiter: RateLimiter
) : BaseScraper(rateLimiter) {

    override val source = PropertySource.PISOSCOM
    override val baseUrl = "https://www.pisos.com"

    private val searchUrls = listOf(
        "$baseUrl/venta/pisos-madrid/",
        "$baseUrl/alquiler/pisos-madrid/",
        "$baseUrl/venta/pisos-barcelona/",
        "$baseUrl/alquiler/pisos-barcelona/"
    )

    override fun scrape(): List<Property> {
        val properties = mutableListOf<Property>()

        for (searchUrl in searchUrls) {
            try {
                logger.info("Scraping Pisos.com: $searchUrl")
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

        val items = document.select(".ad-preview, .ad-list-item")

        for (item in items) {
            try {
                val property = parsePropertyItem(item, operationType, city)
                if (property != null) {
                    properties.add(property)
                }
            } catch (e: Exception) {
                logger.warn("Error parsing Pisos.com property: ${e.message}")
            }
        }

        return properties
    }

    private fun parsePropertyItem(item: Element, operationType: OperationType, city: String): Property? {
        val linkElement = item.selectFirst("a.ad-preview__title, a.ad-list-item__title")
            ?: item.selectFirst("a[href*='/piso-']")
            ?: return null

        val href = linkElement.attr("href")
        val externalId = extractIdFromUrl(href) ?: return null

        val title = linkElement.text().trim()
        val priceText = item.selectFirst(".ad-preview__price, .ad-list-item__price")?.text()
        val price = extractPrice(priceText)

        val features = item.select(".ad-preview__char, .ad-list-item__char")
        var rooms: Int? = null
        var area: java.math.BigDecimal? = null
        var bathrooms: Int? = null

        for (feature in features) {
            val text = feature.text().lowercase()
            when {
                text.contains("hab") -> rooms = extractNumber(text)
                text.contains("m²") || text.contains("m2") -> area = extractArea(text)
                text.contains("baño") -> bathrooms = extractNumber(text)
            }
        }

        val address = item.selectFirst(".ad-preview__address, .ad-list-item__address")?.text()?.trim()
        val zone = item.selectFirst(".ad-preview__zone, .ad-list-item__zone")?.text()?.trim()

        val imageUrl = item.selectFirst("img.ad-preview__img, img.ad-list-item__img")?.attr("src")
            ?: item.selectFirst("img")?.attr("data-src")
        val imageUrls = if (imageUrl != null) arrayOf(imageUrl) else null

        val propertyType = inferPropertyType(title)

        return Property(
            externalId = externalId,
            source = PropertySource.PISOSCOM,
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
        val regex = Regex("/piso-[^/]+-([a-z0-9]+)/")
        return regex.find(url)?.groupValues?.get(1)
            ?: Regex("(\\d+)").find(url)?.value
    }

    private fun extractCityFromUrl(url: String): String {
        return when {
            url.contains("madrid") -> "Madrid"
            url.contains("barcelona") -> "Barcelona"
            url.contains("valencia") -> "Valencia"
            url.contains("sevilla") -> "Sevilla"
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
            lowerTitle.contains("ático") -> PropertyType.ATICO
            lowerTitle.contains("dúplex") -> PropertyType.DUPLEX
            lowerTitle.contains("estudio") -> PropertyType.ESTUDIO
            else -> PropertyType.PISO
        }
    }
}
