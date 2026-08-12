# Gem Store — WSO2 API Management Integration Results

## Project Outcome

The Gem Store backend was extended from a directly exposed Spring Boot REST
API into an API-managed architecture using WSO2 API Manager.

Before the integration, clients communicated directly with the Spring Boot
application:

```text
Client
   |
   v
Spring Boot API
   |
   v
Database
```

After the integration:

```text
Client
   |
   v
WSO2 API Gateway
   |
   +---- API routing
   |
   +---- throttling
   |
   +---- correlation / tracing
   |
   +---- endpoint resilience
   |
   v
Spring Boot API
   |
   +---- JWT authentication
   |
   +---- authorization
   |
   +---- business logic
   |
   v
Database
```

The project therefore demonstrates how an application API can be placed behind
an enterprise API management layer without rewriting the existing backend
architecture.

---

# 1. Managed API

The Gem Store Listings API was successfully imported and managed through WSO2
API Manager.

Current API:

```text
Name:       GemStoreListingsAPI
Display:    Gem Store Listings API
Version:    1.0.0
Context:    /gemstore
Lifecycle:  PUBLISHED
Gateway:    Default
```

Gateway requests are exposed through:

```text
https://localhost:8243/gemstore/1.0.0/...
```

For example:

```text
GET /gemstore/1.0.0/api/v1/listings
```

The API contains the Spring Boot Listings operations and is represented by a
version-controlled WSO2 API artifact.

---

# 2. API Gateway Integration

Successful requests were routed through WSO2 rather than directly to Spring
Boot.

Verified request:

```bash
curl -k -i \
  "https://localhost:8243/gemstore/1.0.0/api/v1/listings?page=0&size=5"
```

Observed:

```text
HTTP/1.1 200
Content-Type: application/json
```

The runtime path was therefore:

```text
Client
   |
   | HTTPS :8243
   v
WSO2 Gateway
   |
   | HTTP :8080
   v
Spring Boot
   |
   v
Gem Store response
```

This verifies that WSO2 is operating as an actual gateway in front of the
application rather than only containing an API definition in Publisher.

---

# 3. API Security

The current architecture preserves Gem Store's existing JWT authentication.

Protected resources are not currently authenticated using native WSO2 OAuth.

Instead:

```text
Client
   |
   | Gem Store JWT
   v
WSO2 Gateway
   |
   | forwards Authorization header
   v
Spring Boot JwtFilter
   |
   v
Spring Security
   |
   v
Protected API
```

Two runtime tests demonstrated the security boundary.

Without JWT:

```text
GET /api/v1/listings/my

HTTP 401
```

With a valid Gem Store JWT:

```text
GET /api/v1/listings/my
Authorization: Bearer <JWT>

HTTP 200
```

Therefore the project demonstrates gateway integration without unnecessarily
replacing the application's existing identity implementation.

Native WSO2-managed OAuth/OIDC security remains a possible future
architecture improvement.

---

# 4. Operation-Level Throttling

Different throttling policies were assigned to different API operations.

Examples include:

```text
GET    /api/v1/listings
       GemStoreListing100PerMin

GET    /api/v1/listings/search
       GemStoreSearch30PerMin

POST   /api/v1/listings
       GemStorePostListing10PerMin

PUT    /api/v1/listings/{id}
       GemStoreUpdate20PerMin

PATCH  /api/v1/listings/{id}
       GemStorePatch20PerMin

DELETE /api/v1/listings/{id}
       GemStoreDelete20PerMin
```

Runtime throttling was verified against the search operation.

The captured test produced:

```text
Request 30: HTTP 200
Request 31: HTTP 200
Request 32: HTTP 200
Request 33: HTTP 429
Request 34: HTTP 429
Request 35: HTTP 429
```

This demonstrates that the gateway actively enforced traffic controls rather
than merely displaying policies in Publisher.

---

# 5. Endpoint Resilience

The backend endpoint configuration included timeout and suspension behavior.

Configured values included:

```text
Connection timeout:       3000 ms
Initial suspension:       5000 ms
Maximum suspension:       30000 ms
Suspension factor:        2
Automatic retries:        0
```

Automatic retries were intentionally not enabled globally because Gem Store
contains state-changing operations such as:

```text
POST
PUT
PATCH
DELETE
```

Blind retries of those operations could produce unintended duplicate effects.

This demonstrates consideration of idempotency when designing gateway
resilience behavior.

---

# 6. Correlation and Observability

Correlation handling was introduced so gateway requests can be traced across
the request path.

Conceptually:

```text
Client
   |
   | X-Correlation-ID
   v
WSO2 Gateway
   |
   | preserve / generate correlation ID
   v
Spring Boot
   |
   v
Application processing
```

WSO2 correlation tracing was used to inspect request processing through
gateway components including authentication and throttling handlers.

This provides a foundation for distributed request tracing and production
observability.

The portfolio implementation demonstrates correlation tracing, but does not
claim a complete production monitoring stack.

---

# 7. API Lifecycle Management

The API is currently:

```text
PUBLISHED
```

The project demonstrates the distinction between:

```text
Lifecycle
    |
    +--> Is the API published?


Version
    |
    +--> Which consumer-facing API contract?


Revision
    |
    +--> Which configuration snapshot is deployed?
```

For the current API:

```text
GemStoreListingsAPI
        |
        +---- Version 1.0.0
                 |
                 +---- Revision 8
                 +---- Revision 9
                 +---- Revision 10
                 +---- Revision 11
                 +---- Revision 12
                          |
                          v
                    Default Gateway
```

Revision 12 was observed as deployed to:

```text
[Default]
```

while the public API version remained:

```text
1.0.0
```

This demonstrates controlled gateway configuration changes without
unnecessarily creating new public API versions.

---

# 8. APICTL Deployment

WSO2 API Controller 4.7.1 was installed and configured.

Verified installation:

```text
/usr/local/bin/apictl
```

Version:

```text
4.7.1
```

The local API Manager environment was registered as:

```text
dev
```

APICTL was successfully used to:

```text
Discover APIs
      |
      v
Export API
      |
      v
Store artifact in Git
      |
      v
Governance dry-run
      |
      v
Import/update API
      |
      v
Verify API
      |
      v
Gateway smoke test
```

This moves the project away from a Publisher-only configuration workflow.

---

# 9. API Configuration as Code

The exported WSO2 artifact is stored in the repository:

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

The artifact contains API management configuration such as:

- API metadata
- OpenAPI definition
- backend endpoint configuration
- operations
- throttling assignments
- CORS configuration
- policies
- documentation metadata

This provides version history and reproducibility for API management changes.

---

# 10. API Governance Validation

APICTL dry-run validation was successfully executed against the exported API.

The validation identified issues including:

```text
Path parameters should be defined on the path level.
```

and:

```text
Info object should have "license" object.
```

WSO2 API Management Guidelines also reported:

```text
Security is not enabled for all resources.
```

because the current gateway operations use:

```yaml
authType: None
```

while authentication remains implemented by Spring Security.

These findings are documented rather than hidden.

This demonstrates the ability to include API governance validation in a future
deployment pipeline.

---

# 11. Existing Continuous Integration

The backend repository already contains a GitHub Actions CI workflow:

```text
.github/workflows/ci.yml
```

The workflow runs for:

```text
push → main

pull request → main
```

The implemented CI flow is:

```text
GitHub event
     |
     v
Checkout repository
     |
     v
Set up JDK 21
     |
     v
Cache Maven dependencies
     |
     v
mvn clean verify
     |
     | SPRING_PROFILES_ACTIVE=test
     v
Build + automated tests
     |
     +---- failure
     |        |
     |        v
     |   Upload Surefire reports
     |
     v
Package JAR
```

Therefore application build and test automation already exists.

---

# 12. Current Deployment Automation Level

WSO2 deployment is scriptable through APICTL but is not currently triggered
automatically by GitHub Actions.

The current architecture is:

```text
                Git Repository
                      |
           +----------+----------+
           |                     |
           v                     v
    GitHub Actions         WSO2 Artifact
           |                     |
           v                     v
     Build + Tests         APICTL validation
                                 |
                                 v
                           APICTL update
                                 |
                                 v
                         Revision deployment
                                 |
                                 v
                           Gateway testing
```

The left side is automated through GitHub Actions.

The WSO2 deployment side is currently executed manually from the development
machine.

Therefore the accurate description is:

> Gem Store has automated continuous integration and a reproducible,
> APICTL-based WSO2 deployment workflow. Automatic continuous deployment from
> GitHub into the local WSO2 environment has not been implemented.

---

# 13. Why Automatic GitHub-to-WSO2 Deployment Was Not Added

The current WSO2 development environment runs locally:

```text
https://localhost:9443
```

and the gateway runs locally:

```text
https://localhost:8243
```

A GitHub-hosted runner cannot access those services simply by using
`localhost`, because `localhost` on the runner refers to the runner itself.

For example:

```text
GitHub-hosted runner
        |
        | https://localhost:9443
        v
   Runner itself
        X
   Developer's WSO2
```

Full automatic deployment would therefore require infrastructure such as:

```text
Self-hosted runner
        |
        v
Local WSO2
```

or:

```text
GitHub-hosted runner
        |
        v
Remotely reachable WSO2 environment
```

A self-hosted runner was deliberately not introduced solely for portfolio
demonstration.

---

# 14. Verification Results

The final verification matrix is:

| Capability | Result |
|---|---|
| WSO2 API Manager integration | PASS |
| Listings API imported | PASS |
| API published | PASS |
| Gateway routing | PASS |
| Public request through gateway | HTTP 200 |
| Protected request without JWT | HTTP 401 |
| Protected request with JWT | HTTP 200 |
| Spring Security integration | PASS |
| Operation-level throttling | PASS |
| Throttled response | HTTP 429 |
| Endpoint timeout configuration | PASS |
| Endpoint suspension configuration | PASS |
| Correlation tracing | PASS |
| API revision management | PASS |
| Revision deployed to Default gateway | PASS |
| APICTL installation | PASS |
| APICTL API discovery | PASS |
| APICTL export | PASS |
| API artifact stored in Git | PASS |
| APICTL governance dry-run | PASS |
| APICTL import/update | PASS |
| Post-deployment gateway smoke test | HTTP 200 |
| GitHub Actions build/test CI | PASS |
| Automatic GitHub → local WSO2 CD | NOT IMPLEMENTED |

---

# 15. Evidence

Portfolio evidence is stored under:

```text
docs/wso2/screenshots/
```

The captured evidence includes:

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

These screenshots provide evidence for both configuration and runtime
behavior.

---

# 16. Enterprise API Management Concepts Demonstrated

The WSO2 integration demonstrates practical experience with:

- API gateway architecture
- OpenAPI-driven API management
- API publishing
- API lifecycle management
- API versioning
- gateway revisions
- operation-level throttling
- HTTP 429 rate-limit handling
- JWT forwarding
- Spring Security integration
- API policies
- correlation IDs
- request tracing
- backend endpoint timeout configuration
- endpoint suspension
- API governance validation
- API configuration as code
- APICTL
- Git-based API artifacts
- GitHub Actions CI
- Maven build automation
- deployment smoke testing

The implementation focuses on demonstrating the architectural concepts rather
than only configuring features through the WSO2 UI.

---

# 17. Current Limitations

The current portfolio implementation intentionally does not claim:

- native WSO2 OAuth authentication for all resources
- automatic GitHub Actions deployment to the local WSO2 instance
- a self-hosted GitHub Actions runner
- production-grade TLS certificates
- multi-node WSO2 deployment
- production-scale WSO2 load testing
- multi-environment API promotion
- production secret management
- complete distributed tracing infrastructure
- automatic gateway failover across multiple backend instances

These are future improvements rather than completed functionality.

---

# 18. Future Improvements

A future production-oriented implementation could extend the architecture to:

```text
Developer
    |
    | git push
    v
GitHub
    |
    v
CI/CD
    |
    +--> Build
    |
    +--> Test
    |
    +--> Validate OpenAPI
    |
    +--> API governance validation
    |
    +--> APICTL deployment
    |
    +--> Revision deployment
    |
    +--> Gateway smoke test
    |
    v
Development
    |
    v
Test
    |
    v
Production
```

Additional improvements could include:

- native identity integration where appropriate
- environment-specific deployment parameters
- automated revision management
- secure CI secret storage
- remotely hosted WSO2 environments
- centralized metrics and dashboards
- distributed tracing
- automated API contract tests

---

# 19. Final Architecture

The final demonstrated architecture is:

```text
                         GitHub
                           |
                           v
                    GitHub Actions
                           |
                    Build + Test
                           |
                           v
                     Spring Boot
                           ^
                           |
                           |
Client
   |
   | HTTPS
   v
+-------------------------------+
|       WSO2 API Manager        |
|                               |
|  API Gateway                  |
|  API Lifecycle                |
|  API Versioning               |
|  Revisions                    |
|  Throttling                   |
|  Correlation                  |
|  Endpoint Resilience          |
|  API Governance               |
+-------------------------------+
   |
   | Backend request
   v
+-------------------------------+
|      Gem Store Backend        |
|                               |
|  Spring Boot                  |
|  JwtFilter                    |
|  Spring Security              |
|  Business Services            |
|  Persistence                  |
+-------------------------------+
   |
   v
Database


WSO2 configuration
        ^
        |
      APICTL
        ^
        |
Version-controlled
WSO2 API artifact
```

---

# Conclusion

The WSO2 integration transformed Gem Store from an application exposing REST
endpoints directly into a project demonstrating a managed API architecture.

The implementation successfully demonstrated:

```text
API publication
       +
Gateway routing
       +
Application JWT security
       +
Operation-level throttling
       +
Endpoint resilience
       +
Correlation tracing
       +
Lifecycle management
       +
Revision deployment
       +
API governance
       +
API configuration as code
       +
APICTL deployment
       +
GitHub Actions CI
       +
Runtime verification
```

The result is not presented as a production WSO2 platform.

Instead, it demonstrates a working enterprise API management integration with
clear evidence of which capabilities were implemented, tested, automated, and
left as future improvements.