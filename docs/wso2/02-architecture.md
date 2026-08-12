# Architecture

## Overview

Gem Store uses WSO2 API Manager as an API management and gateway layer in front of the Spring Boot backend.

The architecture separates API management responsibilities from application business logic.

```text
                         API Consumer
                    React / curl / Partner
                              |
                              | HTTPS
                              | :8243
                              v
                 +---------------------------+
                 |     WSO2 API Gateway      |
                 |                           |
                 |  GemStoreListingsAPI      |
                 |  Context: /gemstore       |
                 |  Version: 1.0.0           |
                 |                           |
                 |  - Routing                |
                 |  - Throttling             |
                 |  - Correlation IDs        |
                 |  - Gateway policies       |
                 |  - Revision deployment    |
                 |  - Request tracing        |
                 +-------------+-------------+
                               |
                               | HTTP
                               | localhost:8080
                               v
                 +---------------------------+
                 |   Spring Boot Backend     |
                 |                           |
                 |  - REST controllers       |
                 |  - Business logic         |
                 |  - Gem Store JWT          |
                 |  - Authorization           |
                 |  - Validation              |
                 |  - Persistence             |
                 +-------------+-------------+
                               |
                   +-----------+-----------+
                   |                       |
                   v                       v
            +-------------+         +-------------+
            | PostgreSQL  |         | Supporting  |
            | Database    |         | Services    |
            +-------------+         +-------------+
```

## Request Flow

A typical public listing request follows this path:

```text
Client
  |
  | GET
  | https://localhost:8243/gemstore/1.0.0/api/v1/listings
  |
  v
WSO2 API Gateway
  |
  | 1. Match API and resource
  | 2. Execute gateway policies
  | 3. Apply operation-level throttling
  | 4. Generate/preserve correlation information
  | 5. Forward request
  |
  v
Spring Boot
  |
  | GET /api/v1/listings
  |
  | 6. Execute controller/service logic
  | 7. Query application data
  |
  v
PostgreSQL
  |
  v
Spring Boot response
  |
  v
WSO2 Gateway
  |
  v
Client
```

The consumer therefore interacts with the managed gateway URL rather than the internal Spring Boot endpoint.

## Gateway Layer

The WSO2 API Gateway is exposed locally through:

```text
HTTPS Gateway:
https://localhost:8243

HTTP Gateway:
http://localhost:8280
```

The managed Listings API uses:

```text
API Name: GemStoreListingsAPI
Display Name: Gem Store Listings API
Version: 1.0.0
Context: /gemstore
```

A public request therefore uses a URL such as:

```text
https://localhost:8243/gemstore/1.0.0/api/v1/listings
```

WSO2 then forwards the request to the configured backend endpoint:

```text
http://localhost:8080
```

Both Production and Sandbox endpoints currently use the same local Spring Boot instance because this project is running as a local portfolio environment.

In a deployed architecture, these would normally point to separate environment-specific services.

## Spring Boot Backend

Spring Boot remains the application layer.

It is responsible for:

* REST controllers
* listing business logic
* service-layer processing
* application validation
* Gem Store JWT authentication
* authorization
* PostgreSQL persistence
* messaging and other application functionality

WSO2 does not replace these responsibilities.

Instead, it creates a managed boundary around the API.

## Security Flow

Gem Store currently uses application-issued JWTs that are validated by Spring Security.

For a protected endpoint, the flow is:

```text
Client
   |
   | Authorization: Bearer <Gem Store JWT>
   v
WSO2 Gateway
   |
   | forwards request
   v
Spring Boot JwtFilter
   |
   | validate JWT
   | extract user ID
   | extract role
   | create SecurityContext
   v
Protected controller
```

This was demonstrated using:

```text
GET /api/v1/listings/my
```

Without a Gem Store JWT:

```text
HTTP 401
```

With a valid Gem Store JWT:

```text
HTTP 200
```

Native WSO2 OAuth-based authentication is not currently used as the primary application identity mechanism.

## Operation-Level Throttling

WSO2 applies rate limits at individual API operations.

Examples include:

```text
GET    /api/v1/listings
       → GemStoreListing100PerMin

GET    /api/v1/listings/search
       → GemStoreSearch30PerMin

POST   /api/v1/listings
       → GemStorePostListing10PerMin

PUT    /api/v1/listings/{id}
       → GemStoreUpdate20PerMin

PATCH  /api/v1/listings/{id}
       → GemStorePatch20PerMin

DELETE /api/v1/listings/{id}
       → GemStoreDelete20PerMin
```

Runtime testing demonstrated successful requests followed by:

```text
HTTP 429 Too Many Requests
```

when the configured search limit was exceeded.

## Correlation and Request Tracing

WSO2 correlation logging provides visibility into the processing of an individual gateway request.

A single activity ID can be followed through stages including:

```text
HTTP request received
       |
       v
CORSRequestHandler
       |
       v
APIKeyValidator
       |
       v
APIAuthenticationHandler
       |
       v
ThrottleHandler
       |
       v
Gateway mediation
       |
       v
Backend invocation
       |
       v
Response processing
```

This provides gateway-level traceability when investigating failures or latency.

## API Management Plane

The architecture also contains WSO2 management components that are not directly in the normal request path.

```text
                 +-----------------------+
                 |   Publisher Portal    |
                 |                       |
                 | API design            |
                 | resources             |
                 | endpoints             |
                 | policies              |
                 | revisions             |
                 | lifecycle             |
                 +-----------+-----------+
                             |
                             v
                 +-----------------------+
                 |      WSO2 APIM        |
                 +-----------+-----------+
                             |
             +---------------+---------------+
             |                               |
             v                               v
   +-------------------+           +--------------------+
   | Developer Portal  |           |   Admin Portal     |
   |                   |           |                    |
   | API discovery     |           | throttling policy  |
   | documentation     |           | administration     |
   | Try Out           |           | gateway settings   |
   +-------------------+           +--------------------+
```

The Publisher is used by API creators, while the Developer Portal provides a consumer-facing API catalog.

The Admin Portal is used for administrative capabilities such as custom rate-limiting policies.

## API Deployment Architecture

API configuration is also stored outside the Publisher UI using WSO2 API Controller.

```text
WSO2 Publisher
      |
      | apictl export api
      v
Version-controlled API artifact
      |
      | Git
      v
wso2/apis/listings-api/
      |
      | apictl import api --update
      v
WSO2 API Manager
      |
      | revision deployment
      v
Default Gateway
```

The repository currently contains an exported WSO2 API artifact with:

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

This makes the API configuration version-controlled and deployable through APICTL instead of existing only inside the Publisher UI.

## Resilience

The gateway endpoint configuration includes fail-fast and backend resilience behavior.

The configured endpoint behavior includes:

```text
Connection timeout:
3000 ms

Initial suspension:
5000 ms

Maximum suspension:
30000 ms

Suspension factor:
2

Automatic retries:
0
```

Automatic retries were intentionally avoided because the API contains state-changing operations such as POST, PUT, PATCH, and DELETE.

Blindly retrying these operations could produce duplicate or unintended changes.

## Health Monitoring

Health checks were verified for both the gateway platform and backend application.

WSO2 gateway startup health:

```text
GET /api/am/gateway/v2/server-startup-healthcheck
→ HTTP 200
```

Spring Boot Actuator:

```text
GET /actuator/health

{
  "status": "UP"
}
```

These checks provide basic runtime readiness information for the API management and backend layers.

## Current Local Deployment

The portfolio environment currently runs on one development machine:

```text
Browser / curl
      |
      v
localhost:8243
WSO2 Gateway
      |
      v
localhost:8080
Spring Boot
      |
      v
PostgreSQL
```

Because this is a local demonstration environment, some production concerns are intentionally outside the current scope.

These include:

* dedicated development, test, and production infrastructure
* externally hosted WSO2 Gateway nodes
* load-balanced gateway clusters
* Kubernetes deployment
* automated GitHub Actions deployment
* centralized production monitoring
* native WSO2 identity integration

## Architecture Outcome

The resulting architecture demonstrates separation between:

```text
Application development
        ↓
Spring Boot

API management
        ↓
WSO2 API Manager

API deployment artifacts
        ↓
APICTL + Git

Runtime traffic governance
        ↓
Gateway policies and throttling

Application security
        ↓
Gem Store JWT + Spring Security
```

This allows Gem Store backend services to remain focused on application functionality while WSO2 provides a governed API boundary around them.
