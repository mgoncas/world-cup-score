# World Cup Score API

Este proyecto es un microservicio REST desarrollado en Java con Spring Boot que simula el procesamiento concurrente de apuestas en eventos deportivos. Forma parte de una prueba técnica orientada a evaluar habilidades en diseño de sistemas concurrentes, arquitectura de microservicios y buenas prácticas de desarrollo en entornos reales.

---

## 1. Tecnología usada, arquitectura y solución técnica

### Tecnologías principales:
- **Java 17**
- **Spring Boot 3**
- **Spring Web** (para la construcción de la API REST)
- **Spring Actuator** (para observabilidad)
- **Springdoc OpenAPI/Swagger UI** (documentación de endpoints)
- **JUnit 5** y **Mockito** (para testing)
- **Jacoco** (para cobertura de código)
- **PITEST** (para análisis de comportamientos de test en mutaciones)
- **Maven** (gestión del proyecto y dependencias)

### Arquitectura:
- Arquitectura basada en los siguientes paquetes:
    - `controller`: expone los endpoints de la API.
    - `service`: contiene la lógica de negocio y procesamiento concurrente.
    - `model`: contiene las entidades y enums necesarios.
    - `config`: inicialización, configuración de beans, dataset inicial, etc.
    - `exception`: control de errores centralizado.

### Solución de concurrencia:
- El procesamiento de apuestas se realiza mediante **un pool de workers** gestionado con múltiples hilos (`ExecutorService`).
- Se utiliza una **cola concurrente** (`BlockingQueue`) para garantizar el orden de llegada.
- Se emplean estructuras como `DoubleAdder` y `Collections.synchronizedList()` para el manejo eficiente y seguro de métricas agregadas.

### Control de excepciones:
- Las excepciones se gestionan globalmente mediante una clase anotada con `@RestControllerAdvice`, que captura errores y devuelve respuestas consistentes (`ResponseEntity<ErrorDetails>`).

### Configuración
El proyecto cuenta con una clase de configuración responsable de preparar el sistema al iniciar y al finalizar la ejecución de la aplicación:

- **Inicialización con `@PostConstruct`:**  
  Al arrancar la aplicación, se generan automáticamente **100 apuestas en estado `OPEN`** que se inyectan en el sistema. Esto permite tener una base de datos inicial simulada y validar que el procesamiento de apuestas funciona correctamente desde el principio.

- **Apagado con `@PreDestroy`:**  
  Antes de que la aplicación se detenga, se invoca automáticamente el método `shutdownSystem()` del `BetProcessor`. Este mecanismo asegura un **apagado ordenado**, esperando a que los hilos (workers) finalicen el procesamiento de todas las apuestas pendientes antes de cerrar la aplicación.

- **Parámetro configurable para el número de workers:**  
  En el archivo `application.properties` se define el parámetro:

  ```properties
  bet.processor.workers=5

---

## 2. Endpoints expuestos

| Método | Ruta                     | Descripción                                                                 |
|--------|--------------------------|-----------------------------------------------------------------------------|
| POST   | `/api/bets`              | Simula la llegada de una nueva apuesta o actualización de una existente.   |
| POST   | `/api/shutdown`          | Inicia el apagado ordenado del sistema, asegurando que se procese todo.    |
| GET    | `/api/summary`           | Devuelve un resumen global de estadísticas de apuestas procesadas.         |
| GET    | `/api/bets/review`       | Devuelve la lista de apuestas que fueron marcadas para revisión.           |

---

## 3. Documentación Swagger

El proyecto expone la documentación de la API mediante **Swagger UI** gracias a la integración con `springdoc-openapi`.

### Acceso a la interfaz:
http://localhost:8080/swagger-ui/index.html

## 4. Pruebas y cobertura

El proyecto cuenta con una batería completa de pruebas para asegurar la calidad del código y la fiabilidad del sistema:

### ✅ Pruebas unitarias

- Validan el comportamiento aislado de componentes individuales como:


- Se apoyan en **JUnit 5** y **Mockito** para simular dependencias.

### 🔄 Pruebas de integración

- Verifican flujos reales de uso de la API REST utilizando **MockMvc**.


### 📊 Cobertura de código con Jacoco

Se incluye **Jacoco** para generar un informe de cobertura que muestra qué partes del código han sido ejecutadas durante las pruebas.

#### Ejecución:

```bash
mvn clean test
```

#### Resultado:

El informe HTML estará disponible en:
```bash
target/site/jacoco/index.html
```

## 5. Pruebas de mutación con PITEST

El proyecto incorpora **[PITEST](https://pitest.org/)**, una herramienta de análisis de mutaciones que evalúa la calidad de las pruebas automatizadas.

### 🧬 ¿Qué es una prueba de mutación?

PITEST modifica (mutando) partes del código fuente de forma controlada (por ejemplo, cambiar `>` por `<`, `true` por `false`, etc.) y luego ejecuta los tests.

- Si los tests **fallan**, significa que han detectado el error → ✅ Test efectivo.
- Si los tests **pasan**, significa que no están validando bien esa lógica → ⚠️ Posible debilidad en los tests.

### 🚀 Ejecución

Para lanzar las pruebas de mutación, puedes ejecutar el siguiente comando:

```bash
mvn org.pitest:pitest-maven:mutationCoverage
```

### 📂 Resultado

El informe HTML se genera en:
````target/pit-reports/index.html````


Desde allí puedes consultar:

- ✅ El porcentaje total de mutaciones detectadas (mutation coverage).
- 📄 Una lista detallada de cada clase y método evaluado.
- ❌ Las mutaciones **sobrevivientes** (es decir, cambios que los tests no lograron detectar).

> ⚠️ Un número alto de mutaciones sobrevivientes indica que puede haber tests poco robustos o casos no contemplados. PITEST te ayuda a fortalecer esas áreas.

Este tipo de análisis complementa la cobertura de Jacoco, y se enfoca en validar la **efectividad real** de los tests, no solo si pasan o no.

## 6. Observabilidad con Spring Boot Actuator

El proyecto incluye **Spring Boot Actuator**, una herramienta fundamental para la observabilidad y monitorización de aplicaciones en producción.

### 🔍 ¿Qué proporciona Actuator?

Actuator expone endpoints que permiten acceder a información clave del estado y comportamiento de la aplicación, como:

- **Salud del sistema**
- **Métricas de rendimiento**
- **Información de configuración**
- **Integración con herramientas de monitorización externas** (Prometheus, Grafana, etc.)

### 📡 End
