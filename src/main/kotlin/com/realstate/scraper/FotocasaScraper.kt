package com.realstate.scraper

import com.realstate.domain.entity.OperationType
import com.realstate.domain.entity.Property
import com.realstate.domain.entity.PropertySource
import com.realstate.domain.entity.PropertyType
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.springframework.stereotype.Component

@Component
class FotocasaScraper(
    rateLimiter: RateLimiter,
    private val browserManager: PlaywrightBrowserManager
) : BaseScraper(rateLimiter) {

    override val source = PropertySource.FOTOCASA
    override val baseUrl = "https://www.fotocasa.es"

    // Selector to wait for - indicates page has loaded (updated for current Fotocasa HTML structure)
    private val waitForSelector = "article, [data-testid='re-SearchResult'], a[href*='/vivienda/']"

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
        // Use Playwright for Fotocasa (requires JavaScript rendering)
        rateLimiter.acquire()
        val document = browserManager.fetchPageWithRetry(url, waitForSelector) ?: run {
            logger.warn("Failed to fetch page with Playwright: $url")
            return emptyList()
        }

        val properties = mutableListOf<Property>()
        val operationType = if (url.contains("alquiler")) OperationType.ALQUILER else OperationType.VENTA
        val city = extractCityFromUrl(url)

        // Updated selectors for current Fotocasa HTML structure (Tailwind CSS based)
        // Articles contain property cards - filter by those with property links
        val items = document.select("article").filter { article ->
            article.select("a[href*='/vivienda/']").isNotEmpty()
        }

        logger.info("Found ${items.size} property items on page: $url")

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
        // Updated parsing for current Fotocasa HTML structure (Tailwind CSS based)
        val linkElement = item.selectFirst("a[href*='/vivienda/']") ?: return null

        val href = linkElement.attr("href")
        val externalId = extractIdFromUrl(href) ?: return null

        // Get all text content to search for title, price, and features
        val itemText = item.text()

        // Extract title from headline class or first meaningful text
        val title = item.selectFirst(".text-subhead, .text-headline-2")?.text()?.trim()
            ?: item.selectFirst("a[href*='/vivienda/']")?.text()?.takeIf { it.length > 10 }?.trim()

        // Extract price - look for pattern like "889.000 €"
        val priceText = extractPriceText(itemText)
        val price = extractPrice(priceText)

        // Extract features from list items
        val featureItems = item.select("li")
        var rooms: Int? = null
        var area: java.math.BigDecimal? = null
        var bathrooms: Int? = null

        for (featureItem in featureItems) {
            val text = featureItem.text().lowercase()
            when {
                text.contains("hab") && rooms == null -> rooms = extractNumber(text)
                (text.contains("m²") || text.contains("m2")) && area == null -> area = extractArea(text)
                text.contains("baño") && bathrooms == null -> bathrooms = extractNumber(text)
            }
        }

        // Fallback: search in full item text
        if (area == null) {
            val areaMatch = Regex("(\\d+)\\s*m[²2]").find(itemText)
            area = areaMatch?.groupValues?.get(1)?.toBigDecimalOrNull()
        }
        if (rooms == null) {
            val roomsMatch = Regex("(\\d+)\\s*hab").find(itemText.lowercase())
            rooms = roomsMatch?.groupValues?.get(1)?.toIntOrNull()
        }
        if (bathrooms == null) {
            val bathMatch = Regex("(\\d+)\\s*baño").find(itemText.lowercase())
            bathrooms = bathMatch?.groupValues?.get(1)?.toIntOrNull()
        }

        // Extract address from location text
        val address = item.selectFirst(".text-body-2")?.text()?.trim()

        // Extract image URL
        val imageUrl = item.selectFirst("img[src*='http']")?.attr("src")
            ?: item.selectFirst("img")?.attr("src")
        val imageUrls = if (imageUrl != null && imageUrl.startsWith("http")) arrayOf(imageUrl) else null

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
            zone = null,
            imageUrls = imageUrls,
            url = if (href.startsWith("http")) href else "$baseUrl$href"
        )
    }

    private fun extractPriceText(text: String): String? {
        // Match patterns like "889.000 €" or "1.200.000 €"
        val priceMatch = Regex("([\\d.]+)\\s*€").find(text)
        return priceMatch?.groupValues?.get(0)
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
