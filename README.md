# Store Backend

Java 21 REST API built with Spring Boot and Maven.

## Requirements

- JDK 21
- Maven 3.9+

## Run

```powershell
mvn spring-boot:run
```

The API listens on `http://localhost:8080`.

## API

- `GET /api/v1/health` - application status
- `GET /api/v1/products` - list products
- `GET /api/v1/products/{id}` - get a product
- `POST /api/v1/products` - create a product
- `PUT /api/v1/products/{id}` - replace a product
- `DELETE /api/v1/products/{id}` - delete a product
- `GET /actuator/health` - Spring Boot Actuator health

Example request body:

```json
{
  "name": "Sample item",
  "description": "Example product",
  "price": 12.5
}
```

Product data currently lives in memory and is cleared when the application restarts. Persistence, authentication, and environment-specific configuration can be added when their requirements are known.

## Test and package

```powershell
mvn test
mvn package
```

## Source layout

```text
src/main/java/com/example/storebe/
  common/exception/  Shared API exception handling
  health/            Health endpoint
  product/
    controller/      REST endpoints
    dto/             Request and response models
    entity/          Domain model
    exception/       Product-specific errors
    repository/      Data access
    service/         Business logic
```
