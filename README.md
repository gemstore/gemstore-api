# 💎 GemStore Backend

### AI-Powered Gemstone Marketplace Platform

Backend service for **GemStore** — a marketplace platform for buying and selling gemstones with **machine learning–based price prediction**, real-time messaging, WebSocket notifications, and enterprise API management through WSO2 API Manager.

[Related Repos](#-related-repositories) ·
[Architecture](#-architecture) ·
[WSO2 API Management](#-enterprise-api-management-with-wso2) ·
[API Docs](#-core-api-endpoints) ·
[Getting Started](#-getting-started)

---

## 📦 Related Repositories

| Repository | Description |
| --- | --- |
| [gemstore-web](https://github.com/mnmmusharraf/gemstore-web) | React user-facing application |
| [gemstore-admin](https://github.com/mnmmusharraf/gemstore-admin) | React admin dashboard |
| [gemstore-ml-service](https://github.com/mnmmusharraf/gemstore-ml-service) | FastAPI ML price prediction service |

---

## 🏗 Architecture

```text
                             +----------------------+
                             |   React Frontend     |
                             |   (gemstore-web)     |
                             +----------+-----------+
                                        |
                                        | HTTPS / REST
                                        v
                             +----------------------+
                             |   WSO2 API Manager   |
                             |----------------------|
                             | API Gateway          |
                             | Throttling           |
                             | Correlation IDs      |
                             | Endpoint Resilience  |
                             | Lifecycle/Revisions  |
                             +----------+-----------+
                                        |
                                        | HTTP
                                        v
                    +-----------------------------------+
                    |        Spring Boot Backend        |
                    |            (This Repo)            |
                    |-----------------------------------|
                    | Auth · Listings · Messaging       |
                    | Notifications · Prediction        |
                    +---------+-------------+-----------+
                              |             |
                     REST API |             | Kafka Events
                              v             v
                    +---------------+  +------------------+
                    |  FastAPI ML   |  |   Apache Kafka   |
                    |  Price Model  |  |   Messaging      |
                    +-------+-------+  +--------+---------+
                            |                   |
                            |                   v
                            |             +-------------+
                            |             |  WebSocket  |
                            |             | Notifications|
                            |             +-------------+
                            |
                            v
                    +---------------+
                    |  ML Prediction|
                    |    Response   |
                    +---------------+

                    +---------------+
                    |  PostgreSQL   |
                    |   Database    |
                    +---------------+
                            ^
                            |
                     Spring Data JPA
```

WSO2 API Manager currently governs the **Listings API**. The FastAPI ML
service remains an internal backend dependency and is not currently managed
as a separate WSO2 API.

---

## ✨ Features

### User & Auth

- Registration, login, and JWT authentication
- Google OAuth2 social login
- Role-based access control
- Protected REST resources
- JWT propagation through the WSO2 gateway

### Marketplace

- Create, search, and manage gemstone listings
- Image uploads per listing
- Like, favorite, and follow system
- Listing view tracking and price history
- Advanced search filters

### Messaging

- Kafka-powered messaging pipeline
- Message delivery status tracking
- Real-time typing events

### Notifications

- WebSocket-based real-time notification delivery
- Events for new messages, followers, and listing interactions

### AI Price Prediction

Integration with an external FastAPI ML service to estimate gemstone prices.

```text
User enters gem details
        │
        ▼
Spring Boot API
        │
        ▼
FastAPI ML Service
        │
        ▼
Predicted price returned to user
```

### Enterprise API Management

The Listings API is managed through WSO2 API Manager with:

- API gateway routing
- API publication
- Developer Portal exposure
- lifecycle management
- revisions
- operation-level throttling
- correlation IDs
- request tracing
- backend resilience configuration
- API governance validation
- APICTL-based deployment

### Admin

- Report moderation tools
- Reported listing management
- User moderation

---

## 🛠 Technology Stack

| Layer | Technology |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.5, Spring Security |
| API Management | WSO2 API Manager 4.7 |
| API Automation | WSO2 API Controller (`apictl`) 4.7.1 |
| Authentication | JWT, OAuth2 (Google) |
| Database | PostgreSQL, Spring Data JPA, Hibernate |
| Migrations | Liquibase |
| Messaging | Apache Kafka, Spring Kafka |
| Real-time | Spring WebSocket |
| HTTP Client | Spring WebClient |
| Mapping | MapStruct |
| API Docs | SpringDoc OpenAPI / Swagger |
| Health Monitoring | Spring Boot Actuator |
| CI | GitHub Actions |
| Build | Maven |
| Testing | JUnit, MockMvc, MockWebServer, H2, Spring Security Test, Kafka Test |

---

## 📁 Project Structure

```text
.
├── .github/
│   └── workflows/
│       └── ci.yml
│
├── docs/
│   └── wso2/
│       ├── 01-business-problem.md
│       ├── 02-architecture.md
│       ├── 03-api-security.md
│       ├── 04-throttling.md
│       ├── 05-lifecycle-and-versioning.md
│       ├── 06-testing.md
│       ├── 07-ci-cd.md
│       ├── 08-results.md
│       └── screenshots/
│
├── src/
│   └── main/
│       ├── java/com/gemstore/backend/
│       │   ├── config/
│       │   ├── controllers/
│       │   ├── services/
│       │   ├── entities/
│       │   ├── repositories/
│       │   ├── dtos/
│       │   ├── mappers/
│       │   └── security/
│       │
│       └── resources/
│
├── wso2/
│   └── apis/
│       └── listings-api/
│           └── GemStoreListingsAPI-1.0.0/
│               ├── Definitions/
│               │   └── swagger.yaml
│               ├── Docs/
│               ├── Policies/
│               ├── api.yaml
│               └── api_meta.yaml
│
└── pom.xml
```

Main Spring Boot package structure:

```text
src/main/java/com/gemstore/backend/
├── config/
│   ├── SecurityConfig
│   ├── JwtFilter
│   ├── KafkaConfig
│   ├── WebSocketAuthConfig
│   └── CorsConfig
├── controllers/
│   ├── auth/
│   ├── listing/
│   ├── user/
│   ├── message/
│   ├── notification/
│   ├── prediction/
│   ├── admin/
│   └── report/
├── services/
│   ├── auth/
│   ├── listing/
│   ├── user/
│   ├── message/
│   ├── notification/
│   ├── prediction/
│   └── report/
├── entities/
│   ├── user/
│   ├── listing/
│   ├── message/
│   ├── notification/
│   └── report/
├── repositories/
├── dtos/
├── mappers/
└── security/
```

---

## 🗄 Database

**Database:** PostgreSQL  
**Migrations:** Managed by Liquibase

Migration files are located in:

```text
src/main/resources/db/changelog/
```

---

## 🤖 ML Integration

The backend communicates with an external FastAPI service for gemstone price prediction.

**Repository:** [gemstore-ml-service](https://github.com/mnmmusharraf/gemstore-ml-service)

**Configuration:**

```properties
gem-price-api.base-url=http://localhost:8000
gem-price-api.predict-endpoint=/predict
gem-price-api.health-endpoint=/health
```

**Example prediction endpoint:**

```text
POST /api/v1/gems/price/predict
```

The ML service currently remains an internal backend service rather than a
separately managed WSO2 API.

---

## 📨 Kafka Messaging

Kafka handles the messaging pipeline including delivery, typing indicators,
and message status updates.

### Kafka Topics

```text
gemstore.messages
gemstore.message-status
gemstore.typing
```

### Configuration

```properties
spring.kafka.bootstrap-servers=localhost:9092
```

### Event Flow

```text
User sends message
        │
        ▼
Controller
        │
        ▼
Kafka Producer
        │
        ▼
gemstore.messages
        │
        ▼
Kafka Consumer
        │
        ▼
PostgreSQL
        │
        ▼
WebSocket notification
```

---

## 🔒 Authentication

### JWT

The application uses its existing Spring Security JWT authentication system.

Standard login returns a signed JWT:

```text
Authorization: Bearer <token>
```

Protected requests follow:

```text
Client
   │
   │ Authorization: Bearer <GemStore JWT>
   ▼
WSO2 API Gateway
   │
   │ Authorization header forwarded
   ▼
Spring Boot JwtFilter
   │
   ▼
Spring Security
   │
   ▼
Protected resource
```

Runtime testing confirmed:

```text
Protected request without JWT → HTTP 401
Protected request with JWT    → HTTP 200
```

WSO2 native OAuth authentication is not currently used for these resources.
It remains a possible future enhancement.

### Google OAuth2

```properties
spring.security.oauth2.client.registration.google.client-id=...
spring.security.oauth2.client.registration.google.client-secret=...
```

---

## 🖼 File Uploads

```properties
app.upload.dir=uploads
spring.servlet.multipart.max-file-size=5MB
```

---

## 📖 API Documentation

Swagger UI is available when the backend is running:

```text
http://localhost:8080/swagger-ui.html
```

The generated OpenAPI definition is also used as part of the API management
workflow.

---

# 🌐 Enterprise API Management with WSO2

GemStore integrates **WSO2 API Manager 4.7** with the existing Spring Boot
backend to demonstrate enterprise API management capabilities.

The integration focuses on the Gem Store Listings API.

## Managed API

```text
Name:       GemStoreListingsAPI
Display:    Gem Store Listings API
Version:    1.0.0
Context:    /gemstore
Lifecycle:  PUBLISHED
Gateway:    Default
```

The managed gateway base URL is:

```text
https://localhost:8243/gemstore/1.0.0
```

Instead of accessing the Spring Boot Listings API directly:

```text
Client
   │
   ▼
http://localhost:8080/api/v1/listings
```

clients can access the managed API through:

```text
Client
   │
   │ HTTPS
   ▼
WSO2 Gateway :8243
   │
   ▼
Spring Boot :8080
```

Example:

```text
https://localhost:8243/gemstore/1.0.0/api/v1/listings
```

---

## WSO2 Capabilities Demonstrated

The integration demonstrates:

- OpenAPI-driven API management
- API Publisher configuration
- Developer Portal publication
- gateway routing
- API lifecycle management
- API revisions
- gateway deployment
- production/sandbox endpoint configuration
- operation-level throttling
- HTTP `429 Too Many Requests` enforcement
- correlation ID handling
- gateway correlation tracing
- backend connection timeout configuration
- endpoint suspension
- API governance validation
- API configuration as code
- APICTL export/import/update
- post-deployment smoke testing

---

## API Gateway Flow

```text
                     Internet / Client
                            │
                            │ HTTPS
                            ▼
                 +----------------------+
                 |   WSO2 API Gateway   |
                 |----------------------|
                 | Routing              |
                 | Throttling           |
                 | Correlation          |
                 | Resilience           |
                 +----------+-----------+
                            │
                            │ HTTP
                            ▼
                 +----------------------+
                 | Spring Boot Backend  |
                 |----------------------|
                 | JwtFilter            |
                 | Spring Security      |
                 | Controllers          |
                 | Services             |
                 +----------+-----------+
                            │
                            ▼
                       PostgreSQL
```

---

## 🚦 Operation-Level Throttling

Different API operations have different traffic limits.

| Operation | Policy |
| --- | --- |
| `GET /api/v1/listings` | `GemStoreListing100PerMin` |
| `GET /api/v1/listings/search` | `GemStoreSearch30PerMin` |
| `POST /api/v1/listings` | `GemStorePostListing10PerMin` |
| `PUT /api/v1/listings/{id}` | `GemStoreUpdate20PerMin` |
| `PATCH /api/v1/listings/{id}` | `GemStorePatch20PerMin` |
| `DELETE /api/v1/listings/{id}` | `GemStoreDelete20PerMin` |

Runtime testing of the search endpoint demonstrated gateway enforcement.

Example result:

```text
Request 29: HTTP 200
Request 30: HTTP 200
Request 31: HTTP 200
Request 32: HTTP 200
Request 33: HTTP 429
Request 34: HTTP 429
Request 35: HTTP 429
```

This verifies that throttling is being enforced by the WSO2 gateway rather
than merely configured in Publisher.

---

## 🛡 Gateway Resilience

The backend endpoint includes timeout and suspension configuration.

```text
Connection timeout:       3000 ms
Initial suspension:       5000 ms
Maximum suspension:       30000 ms
Suspension factor:        2
Automatic retries:        0
```

Automatic retries are intentionally not enabled globally.

GemStore contains state-changing operations such as:

```text
POST
PUT
PATCH
DELETE
```

Automatically retrying these operations without considering idempotency could
cause duplicate or unintended changes.

---

## 🔎 Correlation and Observability

Correlation IDs are used to improve request tracing between WSO2 and the
Spring Boot backend.

```text
Client
   │
   │ X-Correlation-ID
   ▼
WSO2 Gateway
   │
   │ preserve / generate
   ▼
Spring Boot
   │
   ▼
JwtFilter logging
   │
   ▼
Application processing
```

The backend records the incoming correlation ID together with the HTTP method
and request URI.

WSO2 correlation tracing was also used to inspect request processing through
gateway components such as authentication and throttling handlers.

This provides a foundation for production observability without claiming that
the portfolio project contains a complete distributed tracing platform.

---

## ❤️ Health Monitoring

Spring Boot Actuator exposes limited health information:

```properties
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=never
```

This allows infrastructure components to determine whether the application is
available without exposing detailed internal health information publicly.

---

## 🔄 Lifecycle and Revisions

The managed Listings API is currently:

```text
PUBLISHED
```

WSO2 distinguishes API versions from gateway revisions:

```text
GemStoreListingsAPI
        │
        └── Version 1.0.0
                │
                ├── Revision 8
                ├── Revision 9
                ├── Revision 10
                ├── Revision 11
                └── Revision 12
                         │
                         ▼
                  Default Gateway
```

Revision 12 was verified as deployed to the `Default` gateway environment.

This allows gateway configuration to evolve without unnecessarily creating a
new consumer-facing API version for every configuration change.

---

## ⚙️ APICTL

WSO2 API Controller (`apictl`) is used to manage the API from the command line.

Development tooling used for this integration:

```text
APICTL 4.7.1
```

The local API Manager instance is registered as:

```text
dev
```

Example API discovery:

```bash
apictl get apis -e dev -k
```

The API was exported from WSO2 and stored in the Git repository.

---

## 📦 WSO2 API Artifact

The version-controlled artifact is stored under:

```text
wso2/
└── apis/
    └── listings-api/
        └── GemStoreListingsAPI-1.0.0/
            ├── Definitions/
            │   └── swagger.yaml
            ├── Docs/
            ├── Policies/
            ├── api.yaml
            └── api_meta.yaml
```

The artifact captures API management configuration including:

- API metadata
- OpenAPI definition
- operations
- backend endpoints
- throttling assignments
- CORS configuration
- policies
- API documentation metadata

This moves the project away from relying exclusively on manual Publisher
configuration.

---

## 🔍 API Governance Validation

APICTL governance validation can be executed before deployment:

```bash
apictl import api \
  -f wso2/apis/listings-api/GemStoreListingsAPI-1.0.0 \
  -e dev \
  --dry-run \
  -k
```

The governance check identified issues such as:

```text
Path parameters should be defined on the path level.
```

and:

```text
Info object should have "license" object.
```

It also reports gateway resources with:

```text
authType: None
```

because authentication is currently delegated to the existing Spring Boot
security layer.

These findings are intentionally documented rather than hidden.

---

## 🚀 APICTL Deployment

The API can be updated through APICTL:

```bash
apictl import api \
  -f wso2/apis/listings-api/GemStoreListingsAPI-1.0.0 \
  -e dev \
  --update \
  -k
```

The deployment workflow demonstrated in the project is:

```text
WSO2 API artifact
       │
       ▼
Governance dry-run
       │
       ▼
APICTL import/update
       │
       ▼
Revision deployment
       │
       ▼
Gateway
       │
       ▼
Smoke test
```

A successful gateway smoke test returned:

```text
HTTP 200
```

---

## 🔧 CI/CD

The repository contains a GitHub Actions CI workflow:

```text
.github/workflows/ci.yml
```

It runs for:

```text
push → main
pull request → main
```

Current CI:

```text
GitHub event
      │
      ▼
Checkout repository
      │
      ▼
Set up JDK 21
      │
      ▼
Cache Maven dependencies
      │
      ▼
SPRING_PROFILES_ACTIVE=test
      │
      ▼
mvn clean verify
      │
      ├── failure
      │      │
      │      ▼
      │  Upload Surefire reports
      │
      ▼
Package JAR
```

The Spring Boot build and test stages are therefore automated.

WSO2 deployment is currently **scriptable through APICTL but manually
triggered**.

Automatic GitHub Actions → local WSO2 deployment is not currently implemented
because the WSO2 development instance runs locally and is not reachable from a
GitHub-hosted runner.

A future production environment could use:

```text
GitHub
   │
   ▼
GitHub Actions
   │
   ├── Build
   ├── Test
   ├── OpenAPI validation
   ├── Governance validation
   ├── APICTL deployment
   └── Smoke tests
          │
          ▼
        WSO2
          │
          ▼
        DEV
          │
          ▼
        TEST
          │
          ▼
        PROD
```

---

## 📸 WSO2 Portfolio Evidence

Configuration and runtime evidence is stored under:

```text
docs/wso2/screenshots/
```

The evidence set includes:

```text
01-publisher-overview.png
02-api-resources.png
03-endpoint-configuration.png
04-revision-deployment.png
05-published-state.png
06-developer-portal.png
07-throttling-policies.png
08-throttling-429.png
09-public-request-200.png
10-protected-request-401.png
11-protected-request-200-jwt.png
12-correlation-trace.png
13-apictl-api-list.png
14-apictl-governance-validation.png
15-apictl-deployment.png
```

The screenshots demonstrate both configuration and actual runtime behavior.

---

## 📚 WSO2 Documentation

Detailed documentation for the API management implementation is available in:

1. [`01-business-problem.md`](docs/wso2/01-business-problem.md) — business motivation and API management problem
2. [`02-architecture.md`](docs/wso2/02-architecture.md) — WSO2 and GemStore architecture
3. [`03-api-security.md`](docs/wso2/03-api-security.md) — JWT and security architecture
4. [`04-throttling.md`](docs/wso2/04-throttling.md) — operation-level throttling
5. [`05-lifecycle-and-versioning.md`](docs/wso2/05-lifecycle-and-versioning.md) — lifecycle, versions, and revisions
6. [`06-testing.md`](docs/wso2/06-testing.md) — runtime verification and smoke testing
7. [`07-ci-cd.md`](docs/wso2/07-ci-cd.md) — APICTL and CI/CD workflow
8. [`08-results.md`](docs/wso2/08-results.md) — final implementation results

---

## 🚀 Getting Started

### Prerequisites

- Java 21
- Maven
- PostgreSQL
- Apache Kafka
- Python + Uvicorn for the ML service

WSO2 API Manager and APICTL are only required when running the API management
environment.

---

### 1. Clone the repository

```bash
git clone https://github.com/mnmmusharraf/gemstore-backend
cd gemstore-backend
```

### 2. Configure secrets

Create:

```text
src/main/resources/secrets.properties
```

Example:

```properties
DB_PASSWORD=your_password
JWT_SECRET=your_jwt_secret
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
```

Do not commit real secrets to Git.

### 3. Set up PostgreSQL

```sql
CREATE DATABASE gemstore;
```

### 4. Start Kafka

Ensure Kafka is running on:

```text
localhost:9092
```

### 5. Start the ML service

From the ML service repository:

```bash
uvicorn main:app --host 0.0.0.0 --port 8000
```

### 6. Run the backend

```bash
mvn spring-boot:run
```

The backend starts on:

```text
http://localhost:8080
```

### 7. Optional — Access through WSO2

When WSO2 API Manager is running and the Listings API is deployed:

```bash
curl -k \
  "https://localhost:8243/gemstore/1.0.0/api/v1/listings?page=0&size=5"
```

---

## 🧪 Running Tests

Tests use an H2 in-memory database, disabled/mock infrastructure where
appropriate, and mocked external services.

Run:

```bash
mvn test
```

The CI workflow performs the stronger Maven verification lifecycle:

```bash
mvn clean verify -e -B
```

with:

```text
SPRING_PROFILES_ACTIVE=test
```

### Test Coverage

| Type | Examples |
| --- | --- |
| Unit Tests | `GemPriceServiceTest`, `ListingServiceTest`, `UserServiceTest` |
| Integration Tests | `AuthControllerIntegrationTest`, `ListingControllerIntegrationTest`, `GemPriceControllerIntegrationTest` |

WSO2 gateway testing additionally verifies:

```text
Public endpoint                    → HTTP 200
Protected endpoint without JWT     → HTTP 401
Protected endpoint with JWT        → HTTP 200
Throttling limit exceeded          → HTTP 429
APICTL import/update               → Success
Post-deployment gateway smoke test → HTTP 200
```

---

## 🔗 Core API Endpoints

### Authentication

| Method | Endpoint | Description |
| --- | --- | --- |
| POST | `/api/v1/auth/register` | Register a new user |
| POST | `/api/v1/auth/login` | Login and receive JWT |
| POST | `/api/v1/auth/password/reset` | Reset password |

### Users

| Method | Endpoint | Description |
| --- | --- | --- |
| GET | `/api/v1/users/{id}` | Get user profile |
| PUT | `/api/v1/users/profile` | Update profile |
| POST | `/api/v1/users/follow/{id}` | Follow a user |

### Listings

| Method | Endpoint | Description |
| --- | --- | --- |
| POST | `/api/v1/listings` | Create listing |
| GET | `/api/v1/listings` | Get listings |
| GET | `/api/v1/listings/search` | Search listings |
| GET | `/api/v1/listings/{id}` | Get listing |
| PUT | `/api/v1/listings/{id}` | Update listing |
| PATCH | `/api/v1/listings/{id}` | Partially update listing |
| DELETE | `/api/v1/listings/{id}` | Delete listing |
| POST | `/api/v1/listings/{id}/sold` | Mark listing as sold |
| POST | `/api/v1/listings/{id}/archive` | Archive listing |
| GET | `/api/v1/listings/my` | Get authenticated user's listings |

### Messaging

| Method | Endpoint | Description |
| --- | --- | --- |
| POST | `/api/v1/messages` | Send a message |
| GET | `/api/v1/messages/conversation/{id}` | Get conversation |

### Price Prediction

| Method | Endpoint | Description |
| --- | --- | --- |
| POST | `/api/v1/gems/price/predict` | Predict gemstone price |
| GET | `/api/v1/gems/price/health` | ML service health check |

### Example Prediction Request

```json
{
  "gemstoneType": "SAPPHIRE",
  "carat": 1.5,
  "color": "BLUE",
  "clarity": "VVS"
}
```

---

## 🗺 System Design

At the application level:

```text
Request
   │
   ▼
Controller
   │
   │ validation / DTO handling
   ▼
Service Layer
   │
   │ business logic
   ▼
Repository Layer
   │
   │ Spring Data JPA
   ▼
PostgreSQL
```

For managed Listings API requests:

```text
Client
   │
   ▼
WSO2 API Gateway
   │
   ├── Routing
   ├── Throttling
   ├── Correlation
   └── Resilience
   │
   ▼
Spring Boot
   │
   ├── JwtFilter
   ├── Spring Security
   ├── Controllers
   └── Services
   │
   ▼
Persistence / downstream services
```

---

## 📊 WSO2 Integration Results

The portfolio implementation successfully demonstrates:

| Capability | Result |
| --- | --- |
| WSO2 API Manager integration | ✅ Implemented |
| Listings API management | ✅ Implemented |
| API publication | ✅ Published |
| Developer Portal | ✅ Verified |
| Gateway routing | ✅ Verified |
| Public gateway request | ✅ HTTP 200 |
| Existing JWT security through gateway | ✅ Verified |
| Protected request without JWT | ✅ HTTP 401 |
| Protected request with JWT | ✅ HTTP 200 |
| Operation-level throttling | ✅ Implemented |
| Runtime throttling | ✅ HTTP 429 |
| Endpoint timeout configuration | ✅ Implemented |
| Endpoint suspension | ✅ Implemented |
| Correlation tracing | ✅ Demonstrated |
| API lifecycle management | ✅ Demonstrated |
| Gateway revisions | ✅ Demonstrated |
| APICTL installation | ✅ Implemented |
| APICTL export | ✅ Verified |
| API artifact in Git | ✅ Implemented |
| Governance dry-run | ✅ Verified |
| APICTL import/update | ✅ Verified |
| Gateway smoke testing | ✅ HTTP 200 |
| GitHub Actions CI | ✅ Implemented |
| Automatic GitHub → local WSO2 CD | ⏳ Future improvement |

---

## 🔮 Future Improvements

- Redis caching layer
- Elasticsearch for advanced listing search
- Docker + Kubernetes deployment
- Automated GitHub Actions → WSO2 continuous deployment
- Dedicated WSO2 development, test, and production environments
- Environment-specific APICTL deployment parameters
- Native WSO2 OAuth / identity integration where appropriate
- Centralized metrics and dashboards
- Distributed tracing
- Automated API contract testing
- Secure CI/CD secret management
- S3-compatible image storage

---

## 📌 Current API Management Scope

This repository intentionally demonstrates a focused WSO2 integration rather
than claiming a full production API management platform.

Currently implemented:

```text
GemStore Listings API
        │
        ▼
WSO2 API Manager
        │
        ├── Gateway
        ├── Throttling
        ├── Lifecycle
        ├── Revisions
        ├── Correlation
        ├── Resilience
        └── APICTL
```

Not currently claimed as implemented:

```text
Separate WSO2 Valuation API
API Product
Native WSO2 OAuth for all resources
Automatic GitHub → local WSO2 deployment
Self-hosted GitHub Actions runner
Multi-environment production promotion
Production distributed tracing platform
```

These distinctions are intentional so that the repository documentation
reflects capabilities that were actually implemented and verified.

---

## 📄 License

This project is developed as part of the GemStore platform and for portfolio,
learning, and software engineering demonstration purposes.