# Real State Investing - Backend

API REST desarrollada con Kotlin y Spring Boot para el rastreo de inmuebles en portales españoles.

## Tecnologías

- **Kotlin** 1.9.21
- **Spring Boot** 3.2.1
- **PostgreSQL** 14+
- **Firebase Admin SDK** 9.2.0
- **Jsoup** 1.17.2 (Web Scraping)
- **OkHttp** 4.12.0

## Arquitectura

```
src/main/kotlin/com/realstate/
├── RealStateApplication.kt      # Punto de entrada
├── config/                      # Configuraciones
│   ├── FirebaseConfig.kt        # Inicialización de Firebase
│   ├── SchedulerConfig.kt       # Configuración de tareas programadas
│   └── WebConfig.kt             # CORS y configuración web
├── domain/
│   ├── entity/                  # Entidades JPA
│   │   ├── Property.kt          # Inmueble
│   │   ├── User.kt              # Usuario
│   │   ├── SearchAlert.kt       # Alerta de búsqueda
│   │   ├── Favorite.kt          # Favorito
│   │   └── PriceHistory.kt      # Historial de precios
│   └── repository/              # Repositorios JPA
├── service/                     # Lógica de negocio
│   ├── PropertyService.kt
│   ├── UserService.kt
│   ├── AlertService.kt
│   ├── FavoriteService.kt
│   └── NotificationService.kt
├── controller/                  # Endpoints REST
│   ├── PropertyController.kt
│   ├── UserController.kt
│   ├── AlertController.kt
│   └── FavoriteController.kt
├── scraper/                     # Sistema de scraping
│   ├── BaseScraper.kt           # Clase base abstracta
│   ├── RateLimiter.kt           # Control de rate limiting
│   ├── IdealistaScraper.kt
│   ├── FotocasaScraper.kt
│   ├── PisosComScraper.kt
│   └── ScraperScheduler.kt      # Programador de scraping
└── dto/                         # Data Transfer Objects
```

## Modelo de Datos

### Property (Inmueble)
| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | UUID | Identificador único |
| externalId | String | ID en el portal origen |
| source | Enum | IDEALISTA, FOTOCASA, PISOSCOM |
| title | String | Título del anuncio |
| price | Decimal | Precio en euros |
| operationType | Enum | VENTA, ALQUILER |
| propertyType | Enum | PISO, CASA, CHALET, etc. |
| rooms | Integer | Número de habitaciones |
| bathrooms | Integer | Número de baños |
| areaM2 | Decimal | Superficie en m² |
| city | String | Ciudad |
| zone | String | Zona/Barrio |
| url | String | URL original del anuncio |

### SearchAlert (Alerta)
| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | UUID | Identificador único |
| userId | UUID | Usuario propietario |
| name | String | Nombre de la alerta |
| city | String | Filtro por ciudad |
| minPrice/maxPrice | Decimal | Rango de precio |
| minRooms/maxRooms | Integer | Rango de habitaciones |
| isActive | Boolean | Si la alerta está activa |

## API Endpoints

### Autenticación
```
POST /api/auth/register
  Body: { "firebaseUid": "xxx", "email": "user@example.com" }
  Response: UserDTO

POST /api/auth/fcm-token
  Header: X-Firebase-UID
  Body: { "fcmToken": "xxx" }
  Response: { "success": true }

GET /api/auth/me
  Header: X-Firebase-UID
  Response: UserDTO
```

### Propiedades
```
GET /api/properties
  Query params:
    - city: String (opcional)
    - operationType: VENTA|ALQUILER (opcional)
    - propertyType: PISO|CASA|CHALET|... (opcional)
    - minPrice/maxPrice: Number (opcional)
    - minRooms/maxRooms: Integer (opcional)
    - minArea/maxArea: Number (opcional)
    - page: Integer (default: 0)
    - size: Integer (default: 20, max: 100)
  Response: PropertyListDTO

GET /api/properties/{id}
  Header: X-Firebase-UID (opcional, para saber si es favorito)
  Response: PropertyDetailDTO

GET /api/properties/{id}/price-history
  Response: List<PriceHistoryDTO>
```

### Alertas
```
GET /api/alerts
  Header: X-Firebase-UID
  Query params:
    - activeOnly: Boolean (default: false)
  Response: List<AlertDTO>

POST /api/alerts
  Header: X-Firebase-UID
  Body: CreateAlertRequest
  Response: AlertDTO

PUT /api/alerts/{id}
  Header: X-Firebase-UID
  Body: UpdateAlertRequest
  Response: AlertDTO

DELETE /api/alerts/{id}
  Header: X-Firebase-UID
  Response: 204 No Content
```

### Favoritos
```
GET /api/favorites
  Header: X-Firebase-UID
  Response: List<FavoriteDTO>

POST /api/favorites
  Header: X-Firebase-UID
  Body: { "propertyId": "uuid", "notes": "opcional" }
  Response: FavoriteDTO

DELETE /api/favorites/{propertyId}
  Header: X-Firebase-UID
  Response: 204 No Content

GET /api/favorites/check/{propertyId}
  Header: X-Firebase-UID
  Response: { "isFavorite": true|false }
```

## Configuración

### application.yml
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/realstate_db
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}

  jpa:
    hibernate:
      ddl-auto: update  # Cambiar a 'validate' en producción

firebase:
  credentials-path: ${FIREBASE_CREDENTIALS_PATH:firebase-service-account.json}

scraper:
  enabled: true
  rate-limit:
    requests-per-minute: 10
    cooldown-ms: 6000
  schedule:
    cron: "0 */30 * * * *"  # Cada 30 minutos
```

### Variables de entorno
```bash
DB_USERNAME=realstate_user
DB_PASSWORD=tu_password
FIREBASE_CREDENTIALS_PATH=/path/to/firebase-service-account.json
```

## Sistema de Scraping

### Rate Limiting
El sistema implementa rate limiting para evitar bloqueos:
- **10 requests/minuto** por defecto
- **6 segundos de cooldown** entre requests
- Algoritmo de token bucket

### Portales soportados
| Portal | Ciudades | Estado |
|--------|----------|--------|
| Idealista | Madrid, Barcelona | ✅ Implementado |
| Fotocasa | Madrid, Barcelona | ✅ Implementado |
| Pisos.com | Madrid, Barcelona | ✅ Implementado |

### Flujo de scraping
1. ScraperScheduler ejecuta cada 30 min
2. Cada scraper obtiene listados de su portal
3. Propiedades nuevas → INSERT + registro de precio inicial
4. Propiedades existentes → UPDATE last_seen_at + check precio
5. Si precio cambió → INSERT en price_history
6. AlertService busca matches con alertas activas
7. NotificationService envía push a usuarios

## Ejecución

### Desarrollo
```bash
# Compilar
./gradlew build

# Ejecutar
./gradlew bootRun

# Ejecutar tests
./gradlew test
```

### Producción
```bash
# Generar JAR
./gradlew bootJar

# Ejecutar JAR
java -jar build/libs/real-state-investing-backend-0.0.1-SNAPSHOT.jar
```

### Docker (opcional)
```dockerfile
FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

## Logs

Los logs incluyen información de:
- Inicio/fin de scraping
- Propiedades nuevas encontradas
- Cambios de precio detectados
- Notificaciones enviadas
- Errores de scraping/conexión

```bash
# Ver logs en tiempo real
tail -f logs/spring.log

# Nivel de log configurable
logging.level.com.realstate=DEBUG
```

## Consideraciones

### Legales
El scraping de portales inmobiliarios puede violar sus términos de servicio. Este proyecto es para uso educativo. Considera:
- Usar APIs oficiales si están disponibles
- Respetar robots.txt
- Implementar rate limiting adecuado

### Mantenimiento
Los selectores CSS de los scrapers pueden necesitar actualizaciones cuando los portales cambien su estructura HTML.

### Escalabilidad
Para alto volumen:
- Separar scrapers en microservicio
- Usar Redis para caché
- Implementar cola de mensajes para notificaciones
