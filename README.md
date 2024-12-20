# Spring Boot Kotlin JWT

This project demonstrates a **Spring Boot** application built with **Kotlin** for JWT-based authentication and authorization. It uses **MongoDB** for data persistence and integrates Swagger for API documentation.


## Features

- **JWT Authentication**: Secure your endpoints with access and refresh tokens.
- **MongoDB Integration**: Persistent storage for user and token information.
- **Swagger UI**: Easily explore and test APIs with an interactive UI.
- **Kotlin**: Concise and modern programming language support.
- **Spring Security**: Implements robust security mechanisms.
- **Unit Testing with TestContainers**: Integration tests use TestContainers to provide isolated, disposable MongoDB instances.

## Requirements

- **Java**: Version 21+
- **Kotlin**: Version 1.9.25
- **Maven**: Version 3.6+
- **MongoDB**: Running instance for data storage


## Getting Started

### Clone the Repository

```bash
git@github.com:dwididit/springboot-kotlin-jwt.git
cd springboot-kotlin-jwt/
```

### Build and Run
1. Build and run the Project:
   ```bash
   docker-compose up --build -d
   ```
2. Swagger UI
    ```bash
    Access Swagger UI: http://localhost:9090/swagger-ui.html
    ```
