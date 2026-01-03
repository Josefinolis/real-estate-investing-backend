# Real State Investing - Backend

API REST para rastrear inmuebles en portales españoles (Idealista, Fotocasa, Pisos.com).

## Stack

- **Kotlin** + **Spring Boot** 3.2
- **PostgreSQL** 14+
- **Firebase Admin SDK** (push notifications)

## API Endpoints

### Propiedades
```
GET  /api/properties              # Listar con filtros
GET  /api/properties/{id}         # Detalle
GET  /api/properties/{id}/price-history
```

### Alertas
```
GET    /api/alerts                # Listar alertas del usuario
POST   /api/alerts                # Crear alerta
PUT    /api/alerts/{id}           # Actualizar
DELETE /api/alerts/{id}           # Eliminar
```

### Favoritos
```
GET    /api/favorites             # Listar favoritos
POST   /api/favorites             # Añadir
DELETE /api/favorites/{propertyId}
```

### Auth
```
POST /api/auth/register           # Registrar usuario
POST /api/auth/fcm-token          # Actualizar FCM token
GET  /api/auth/me                 # Usuario actual
```

## Desarrollo

```bash
# Compilar
./gradlew build

# Ejecutar
./gradlew bootRun

# Tests
./gradlew test
```

## Variables de entorno

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/realstate
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
FIREBASE_CREDENTIALS_PATH=/path/to/firebase-service-account.json
SCRAPER_ENABLED=true
```

## Deploy

El despliegue es automático via GitHub Actions al hacer push a `main`.

- **URL:** http://195.20.235.94/realstate
- **Container:** `realstate-backend`
- **Database:** PostgreSQL `realstate` en `ia-trading-db`

> **Nota:** El acceso externo es via nginx reverse proxy en puerto 80. El puerto 8081 solo es accesible internamente.
