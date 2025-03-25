# World Cup Score API

This project is a REST microservice developed in Java with Spring Boot that simulates concurrent processing of sports event betting. It is part of a technical test aimed at evaluating skills in concurrent system design, microservice architecture, and best development practices in real-world environments.

---

## 1. Technology Used, Architecture, and Technical Solution

### Main Technologies:
- **Java 17**
- **Spring Boot 3**
- **Spring Web** (for building REST API)
- **Spring Actuator** (for observability)
- **Springdoc OpenAPI/Swagger UI** (endpoint documentation)
- **JUnit 5** and **Mockito** (for testing)
- **Jacoco** (for code coverage)
- **PITEST** (for mutation testing analysis)
- **Maven** (project and dependency management)

### Architecture:
- Architecture based on the following packages:
    - `controller`: exposes API endpoints.
    - `service`: contains business logic and concurrent processing.
    - `model`: contains necessary entities and enums.
    - `config`: initialization, bean configuration, initial dataset, etc.
    - `exception`: centralized error handling.

### Concurrency Solution:
- Bet processing is performed using a **worker pool** managed with multiple threads (`ExecutorService`).
- A **concurrent queue** (`BlockingQueue`) is used to guarantee order of arrival.
- Structures like `DoubleAdder` and `Collections.synchronizedList()` are employed for efficient and safe aggregate metrics management.

### Exception Handling:
- Exceptions are globally managed through a class annotated with `@RestControllerAdvice`, which captures errors and returns consistent responses (`ResponseEntity<ErrorDetails>`).

### Configuration
The project includes a configuration class responsible for preparing the system when starting and ending application execution:

- **Initialization with `@PostConstruct`:**  
  Upon application startup, **100 bets in `OPEN` state** are automatically generated and injected into the system. This allows having a simulated initial database and validates that bet processing works correctly from the beginning.

- **Shutdown with `@PreDestroy`:**  
  Before the application stops, the `shutdownSystem()` method of `BetProcessor` is automatically invoked. This mechanism ensures an **orderly shutdown**, waiting for threads (workers) to finish processing all pending bets before closing the application.

- **Configurable parameter for number of workers:**  
  In the `application.properties` file, the following parameter is defined:

  ```properties
  bet.processor.workers=5
  ```

---

## 2. Exposed Endpoints

| Method | Path                    | Description                                                                 |
|--------|------------------------|-----------------------------------------------------------------------------|
| POST   | `/api/bets`            | Simulates the arrival of a new bet or update of an existing one.           |
| POST   | `/api/shutdown`        | Initiates orderly system shutdown, ensuring everything is processed.        |
| GET    | `/api/summary`         | Returns a global summary of processed bet statistics.                       |
| GET    | `/api/bets/review`     | Returns the list of bets marked for review.                                |

---

## 3. Swagger Documentation

The project exposes API documentation through **Swagger UI** thanks to integration with `springdoc-openapi`.

### Access to the interface:
http://localhost:8080/swagger-ui/index.html

## 4. Testing and Coverage

The project includes a comprehensive test suite to ensure code quality and system reliability:

### ✅ Unit Tests

- Validate isolated behavior of individual components such as:
    - Service layer logic
    - Utility methods
    - Edge case handling

- Rely on **JUnit 5** and **Mockito** to simulate dependencies.

### 🔄 Integration Tests

- Verify real API REST usage flows using **MockMvc**.
- Test complete request-response cycles
- Validate endpoint behaviors under different scenarios

### 📊 Code Coverage with Jacoco

**Jacoco** is included to generate a coverage report showing which parts of the code have been executed during tests.

#### Execution:

```bash
mvn clean test
```

#### Result:

The HTML report will be available at:
```bash
target/site/jacoco/index.html
```

## 5. Mutation Testing with PITEST

The project incorporates **[PITEST](https://pitest.org/)**, a mutation analysis tool that evaluates the quality of automated tests.

### 🧬 What is Mutation Testing?

PITEST modifies (mutating) parts of the source code in a controlled manner (for example, changing `>` to `<`, `true` to `false`, etc.) and then runs the tests.

- If tests **fail**, it means they detected the error → ✅ Effective Test.
- If tests **pass**, it means they are not validating that logic well → ⚠️ Possible test weakness.

### 🚀 Execution

To run mutation tests, execute:

```bash
mvn org.pitest:pitest-maven:mutationCoverage
```

### 📂 Result

The HTML report is generated at:
```
target/pit-reports/index.html
```

From there you can consult:

- ✅ Total percentage of detected mutations (mutation coverage).
- 📄 A detailed list of each evaluated class and method.
- ❌ **Surviving mutations** (changes that tests failed to detect).

> ⚠️ A high number of surviving mutations indicates that tests might be weak or cases might not be contemplated. PITEST helps you strengthen these areas.

This type of analysis complements Jacoco's coverage and focuses on validating the **real effectiveness** of tests, not just whether they pass or not.

## 6. Observability with Spring Boot Actuator

The project includes **Spring Boot Actuator**, a fundamental tool for observability and monitoring of production applications.

### 🔍 What Does Actuator Provide?

Actuator exposes endpoints that allow access to key information about the system's state and behavior, such as:

- **System Health**
- **Performance Metrics**
- **Configuration Information**
- **Integration with External Monitoring Tools** (Prometheus, Grafana, etc.)

### 📡 End
