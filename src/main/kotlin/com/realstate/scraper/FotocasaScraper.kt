package com.realstate.scraper

import com.realstate.domain.entity.OperationType
import com.realstate.domain.entity.Property
import com.realstate.domain.entity.PropertySource
import com.realstate.domain.entity.PropertyType
import org.jsoup.nodes.Element
import org.springframework.stereotype.Component

@Component
class FotocasaScraper(
    rateLimiter: RateLimiter
) : BaseScraper(rateLimiter) {

    override val source = PropertySource.FOTOCASA
    override val baseUrl = "https://www.fotocasa.es"

    private val searchUrls = listOf(
        "$baseUrl/es/comprar/viviendas/madrid-capital/todas-las-zonas/l",
        "$baseUrl/es/alquiler/viviendas/madrid-capital/todas-las-zonas/l",
        "$baseUrl/es/comprar/viviendas/barcelona-capital/todas-las-zonas/l",
        "$baseUrl/es/alquiler/viviendas/barcelona-capital/todas-las-zonas/l"
    )

    // Mapping from display name to URL slug
    private val cityToSlug = mapOf(
        "Madrid" to "madrid-capital", "Barcelona" to "barcelona-capital", "Valencia" to "valencia-capital",
        "Sevilla" to "sevilla-capital", "Zaragoza" to "zaragoza-capital", "Málaga" to "malaga-capital",
        "Murcia" to "murcia-capital", "Palma de Mallorca" to "palma-de-mallorca", "Bilbao" to "bilbao",
        "Las Palmas de Gran Canaria" to "las-palmas-de-gran-canaria", "Alicante" to "alicante",
        "Córdoba" to "cordoba-capital", "Valladolid" to "valladolid-capital", "Vigo" to "vigo",
        "Gijón" to "gijon", "Vitoria-Gasteiz" to "vitoria-gasteiz", "A Coruña" to "a-coruna",
        "Granada" to "granada-capital", "Elche" to "elche-elx", "Oviedo" to "oviedo",
        "Santa Cruz de Tenerife" to "santa-cruz-de-tenerife", "Pamplona" to "pamplona-iruna",
        "Almería" to "almeria-capital", "San Sebastián" to "san-sebastian", "Santander" to "santander",
        "Burgos" to "burgos-capital", "Albacete" to "albacete-capital",
        "Castellón de la Plana" to "castellon-de-la-plana", "Logroño" to "logrono",
        "Badajoz" to "badajoz-capital", "Salamanca" to "salamanca-capital", "Huelva" to "huelva-capital",
        "Lleida" to "lleida", "Tarragona" to "tarragona-capital", "León" to "leon-capital",
        "Cádiz" to "cadiz-capital", "Jaén" to "jaen-capital", "Ourense" to "ourense",
        "Lugo" to "lugo", "Girona" to "girona", "Cáceres" to "caceres", "Guadalajara" to "guadalajara-capital",
        "Toledo" to "toledo-capital", "Pontevedra" to "pontevedra", "Palencia" to "palencia",
        "Ciudad Real" to "ciudad-real-capital", "Zamora" to "zamora", "Ávila" to "avila",
        "Cuenca" to "cuenca", "Huesca" to "huesca", "Segovia" to "segovia", "Soria" to "soria",
        "Teruel" to "teruel", "Ceuta" to "ceuta", "Melilla" to "melilla"
    )

    override fun scrape(): List<Property> {
        val properties = mutableListOf<Property>()

        for (searchUrl in searchUrls) {
            try {
                logger.info("Scraping Fotocasa: $searchUrl")
                val pageProperties = scrapeSearchPage(searchUrl)
                properties.addAll(pageProperties)
                logger.info("Found ${pageProperties.size} properties from $searchUrl")
            } catch (e: Exception) {
                logger.error("Error scraping $searchUrl: ${e.message}", e)
            }
        }

        return properties
    }

    override fun scrapeWithConfig(cities: List<String>, operationTypes: List<String>): List<Property> {
        val properties = mutableListOf<Property>()

        // Build URLs dynamically based on cities and operation types
        val dynamicUrls = cities.flatMap { city ->
            val slug = cityToSlug[city]
            if (slug == null) {
                logger.warn("Unknown city for Fotocasa: $city")
                emptyList()
            } else {
                operationTypes.mapNotNull { opType ->
                    when (opType.uppercase()) {
                        "VENTA" -> "$baseUrl/es/comprar/viviendas/$slug/todas-las-zonas/l"
                        "ALQUILER" -> "$baseUrl/es/alquiler/viviendas/$slug/todas-las-zonas/l"
                        else -> {
                            logger.warn("Unknown operation type: $opType")
                            null
                        }
                    }
                }
            }
        }

        logger.info("Fotocasa: scraping ${dynamicUrls.size} URLs for ${cities.size} cities and ${operationTypes.size} operation types")

        for (searchUrl in dynamicUrls) {
            try {
                logger.info("Scraping Fotocasa: $searchUrl")
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

        val items = document.select("article.re-CardPackPremium, article.re-CardPackMinimal")

        for (item in items) {
            try {
                val property = parsePropertyItem(item, operationType, city)
                if (property != null) {
                    properties.add(property)
                }
            } catch (e: Exception) {
                logger.warn("Error parsing Fotocasa property: ${e.message}")
            }
        }

        return properties
    }

    private fun parsePropertyItem(item: Element, operationType: OperationType, city: String): Property? {
        val linkElement = item.selectFirst("a.re-CardPackPremium-info, a.re-CardPackMinimal-info")
            ?: item.selectFirst("a[href*='/vivienda/']")
            ?: return null

        val href = linkElement.attr("href")
        val externalId = extractIdFromUrl(href) ?: return null

        val title = item.selectFirst(".re-CardTitle, .re-CardPackPremium-title")?.text()?.trim()
        val priceText = item.selectFirst(".re-CardPrice, .re-CardPackPremium-price")?.text()
        val price = extractPrice(priceText)

        val features = item.select(".re-CardFeatures-feature, .re-CardPackPremium-feature")
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

        val address = item.selectFirst(".re-CardAddress, .re-CardPackPremium-address")?.text()?.trim()
        val zone = item.selectFirst(".re-CardZone")?.text()?.trim()

        val imageUrl = item.selectFirst("img.re-CardPhoto, img.re-CardPackPremium-photo")?.attr("src")
        val imageUrls = if (imageUrl != null) arrayOf(imageUrl) else null

        val propertyType = inferPropertyType(title)

        return Property(
            externalId = externalId,
            source = PropertySource.FOTOCASA,
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
        val regex = Regex("/(\\d+)(?:/|\\.htm|$)")
        return regex.find(url)?.groupValues?.get(1)
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
            else -> PropertyType.OTRO
        }
    }
}
