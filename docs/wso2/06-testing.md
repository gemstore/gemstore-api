# WSO2 API Integration Testing

## Overview

The Gem Store WSO2 integration was tested at multiple layers rather than
considering successful configuration in the Publisher UI sufficient evidence.

The verification strategy covered:

- WSO2 gateway health
- API deployment state
- public API routing
- protected application resources
- JWT forwarding
- throttling enforcement
- APICTL governance validation
- APICTL deployment
- revision deployment
- correlation tracing
- backend health

The overall testing flow was:

```text
WSO2 Health
     |
     v
API Deployment
     |
     v
Public Gateway Request
     |
     v
Protected Request
     |
     v
JWT Authentication Test
     |
     v
Throttling Test
     |
     v
Correlation Trace
     |
     v
APICTL Validation
     |
     v
APICTL Deployment
     |
     v
Post-deployment Smoke Test
```

---

# 1. WSO2 Gateway Health Check

Before testing Gem Store APIs, the WSO2 gateway startup health endpoint was
verified.

Command:

```bash
curl -k -i \
  "https://localhost:9443/api/am/gateway/v2/server-startup-healthcheck"
```

Observed result:

```text
HTTP/1.1 200
Server: WSO2 Carbon Server
```

This confirmed that the WSO2 gateway runtime was available before API-level
testing was performed.

---

# 2. API Deployment Verification

APICTL was used to verify that the Gem Store API existed in the configured
development environment.

Command:

```bash
apictl get apis -e dev -k
```

Observed API:

```text
NAME                  VERSION   CONTEXT     STATUS      PROVIDER

GemStoreListingsAPI   1.0.0     /gemstore   PUBLISHED   admin
```

This confirmed:

```text
API       → GemStoreListingsAPI
Version   → 1.0.0
Context   → /gemstore
Lifecycle → PUBLISHED
```

Evidence:

```text
screenshots/13-apictl-api-list.png
```

---

# 3. Revision Deployment Verification

API revisions were inspected using APICTL.

Command:

```bash
apictl get api-revisions \
  -n GemStoreListingsAPI \
  -v 1.0.0 \
  -e dev \
  -k
```

The observed revisions included:

```text
Revision 8
X-Correlation-ID added

Revision 9
Generate unique correlation IDs for gateway requests
and forward them to Spring Boot.

Revision 10
Added correlation ID preservation and automatic generation
for request tracing.

Revision 11
Added endpoint timeout, suspension, and fail-fast
resilience configuration.

Revision 12
Deploy current operation-level throttling and gateway configuration.
```

Revision 12 showed:

```text
GATEWAY_ENVS [Default]
```

This confirmed that the revision was associated with the Default gateway
environment.

Evidence:

```text
screenshots/04-revision-deployment.png
```

---

# 4. Public Gateway Routing Test

A public Listings endpoint was invoked through the WSO2 HTTPS Gateway.

Command:

```bash
curl -k -i \
  "https://localhost:8243/gemstore/1.0.0/api/v1/listings?page=0&size=5"
```

Observed result:

```text
HTTP/1.1 200
Content-Type: application/json
```

The response contained Gem Store listing data.

This verifies the runtime path:

```text
curl
  |
  v
localhost:8243
  |
  v
WSO2 Gateway
  |
  v
localhost:8080
  |
  v
Spring Boot Listings API
  |
  v
JSON response
```

The test is important because the request was sent to the WSO2 gateway port
rather than directly to Spring Boot.

Evidence:

```text
screenshots/09-public-request-200.png
```

---

# 5. Protected Endpoint Without JWT

The protected application endpoint was tested without providing the Gem Store
JWT.

Endpoint:

```text
GET /api/v1/listings/my
```

Gateway request:

```bash
curl -k -i \
  "https://localhost:8243/gemstore/1.0.0/api/v1/listings/my"
```

Observed result:

```text
HTTP/1.1 401
```

The test verifies that an unauthenticated client cannot successfully access
the protected Spring Boot resource.

The current authentication boundary is:

```text
Client
   |
   v
WSO2
   |
   v
Spring Security
   |
   X
401 Unauthorized
```

The HTTP 401 should not be interpreted as WSO2 OAuth validation.

The current Gem Store application authentication mechanism is implemented by
Spring Security.

Evidence:

```text
screenshots/10-protected-request-401.png
```

---

# 6. Protected Endpoint With Gem Store JWT

The same protected endpoint was then called with a valid Gem Store JWT.

Conceptual command:

```bash
curl -k -i \
  -H "Authorization: Bearer $JWT" \
  "https://localhost:8243/gemstore/1.0.0/api/v1/listings/my"
```

Observed result:

```text
HTTP/1.1 200
```

The response contained the authenticated user's listing data.

This verifies the current security path:

```text
Client
   |
   | Authorization: Bearer <Gem Store JWT>
   v
WSO2 Gateway
   |
   | Forward request/header
   v
Spring Boot JwtFilter
   |
   | Validate JWT
   v
Spring Security
   |
   v
Protected controller
   |
   v
HTTP 200
```

Therefore, two complementary security tests were demonstrated:

```text
Without JWT → HTTP 401

With JWT    → HTTP 200
```

Evidence:

```text
screenshots/11-protected-request-200-jwt.png
```

---

# 7. Operation-Level Throttling Test

The search endpoint was used to verify gateway throttling.

Configured operation:

```text
GET /api/v1/listings/search
```

Configured policy:

```text
GemStoreSearch30PerMin
```

Test command:

```bash
for i in {1..35}; do
  echo -n "Request $i: "

  curl -k -s -o /dev/null \
    -w "HTTP %{http_code}\n" \
    "https://localhost:8243/gemstore/1.0.0/api/v1/listings/search?query=ruby&page=0&size=5"
done
```

The captured test initially returned successful responses:

```text
Request 30: HTTP 200
Request 31: HTTP 200
Request 32: HTTP 200
```

The gateway then returned:

```text
Request 33: HTTP 429
Request 34: HTTP 429
Request 35: HTTP 429
```

This verifies that throttling was actively enforced at runtime.

The expected architectural behavior is:

```text
Request
   |
   v
WSO2 Gateway
   |
   v
ThrottleHandler
   |
   +-------- Within limit --------> Spring Boot
   |
   +-------- Limit exceeded ------> HTTP 429
```

Evidence:

```text
screenshots/08-throttling-429.png
```

The test demonstrates that throttling occurred. It does not attempt to prove
that the exact internal threshold window began with request number 1 of the
terminal loop.

---

# 8. Correlation and Request Trace Test

WSO2 correlation logging was used to inspect gateway request processing.

The captured trace showed request processing through gateway components such
as:

```text
HTTP request
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
Backend processing
     |
     v
Response
```

This demonstrates gateway-level request tracing and provides evidence that the
request passed through WSO2's processing pipeline.

Evidence:

```text
screenshots/12-correlation-trace.png
```

---

# 9. APICTL Governance Validation

The exported API artifact was validated before deployment using APICTL
dry-run mode.

Command:

```bash
apictl import api \
  -f wso2/apis/listings-api/GemStoreListingsAPI-1.0.0 \
  -e dev \
  --dry-run \
  -k
```

The governance validation identified REST API design warnings including:

```text
Path parameters should be defined on the path level.
```

and:

```text
Info object should have "license" object.
```

The WSO2 API Management Guidelines also reported:

```text
Security is not enabled for all resources.
```

for operations configured with:

```yaml
authType: None
```

This result is retained as evidence rather than hidden.

It demonstrates that APICTL can identify governance issues before an API
artifact is deployed.

It also documents a known architectural limitation: application JWT
authentication currently remains in Spring Security rather than being
enforced as native WSO2 OAuth security on all gateway resources.

Evidence:

```text
screenshots/14-apictl-governance-validation.png
```

---

# 10. APICTL Import / Update Test

After validation, the API artifact was imported into the development
environment using APICTL.

Command:

```bash
apictl import api \
  -f wso2/apis/listings-api/GemStoreListingsAPI-1.0.0 \
  -e dev \
  --update \
  -k
```

Observed result:

```text
Successfully imported API.
```

This verifies that the exported, version-controlled WSO2 artifact can be used
to update the API without manually rebuilding the complete API configuration
through Publisher.

Evidence:

```text
screenshots/15-apictl-deployment.png
```

---

# 11. Post-Deployment Smoke Test

After the APICTL update, the API list was checked again:

```bash
apictl get apis -e dev -k
```

The API remained:

```text
GemStoreListingsAPI
1.0.0
/gemstore
PUBLISHED
```

The runtime gateway was then tested again:

```bash
curl -k -i \
  "https://localhost:8243/gemstore/1.0.0/api/v1/listings?page=0&size=5"
```

Observed result:

```text
HTTP/1.1 200
```

This provides an important deployment verification sequence:

```text
API artifact
     |
     v
APICTL import --update
     |
     v
Import succeeds
     |
     v
Check API state
     |
     v
Gateway request
     |
     v
HTTP 200
```

A successful CLI import alone is not treated as sufficient evidence of a
working runtime deployment.

---

# 12. Spring Boot Health Test

The backend service health was also verified using Spring Boot Actuator.

Endpoint:

```text
GET /actuator/health
```

Observed application state:

```json
{
  "status": "UP"
}
```

This distinguishes backend availability from gateway availability.

Conceptually:

```text
WSO2 health check
       +
Spring Boot health check
       +
Gateway API smoke test
       =
Better runtime verification
```

---

# 13. Endpoint Resilience Configuration

The WSO2 backend endpoint was configured with fail-fast and suspension
behavior.

The observed configuration included:

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

Automatic retries were intentionally not enabled globally because the API
contains state-changing operations such as:

```text
POST
PUT
PATCH
DELETE
```

Blind retries of non-idempotent operations could create unintended duplicate
effects.

The endpoint configuration is captured separately as portfolio evidence:

```text
screenshots/03-endpoint-configuration.png
```

---

# 14. API Definition and Resource Verification

The API resources were inspected in WSO2 Publisher to verify that the Spring
Boot operations had been imported into the managed API.

The API contains operations including:

```text
GET     /api/v1/listings
POST    /api/v1/listings

GET     /api/v1/listings/{id}
PUT     /api/v1/listings/{id}
PATCH   /api/v1/listings/{id}
DELETE  /api/v1/listings/{id}

GET     /api/v1/listings/search

GET     /api/v1/listings/my
```

Additional listing operations are also represented in the imported API
definition.

Evidence:

```text
screenshots/02-api-resources.png
```

---

# Test Matrix

The final verification matrix is:

| Test | Expected | Observed | Result |
|---|---|---|---|
| WSO2 gateway health | HTTP 200 | HTTP 200 | PASS |
| API exists in dev | Listed | Listed | PASS |
| API lifecycle | PUBLISHED | PUBLISHED | PASS |
| Revision deployment | Default gateway | Revision 12 `[Default]` | PASS |
| Public listings request | HTTP 200 | HTTP 200 | PASS |
| Protected request without JWT | HTTP 401 | HTTP 401 | PASS |
| Protected request with JWT | HTTP 200 | HTTP 200 | PASS |
| Search throttling | HTTP 429 after limit enforcement | HTTP 429 observed | PASS |
| Correlation tracing | Gateway processing trace | Trace observed | PASS |
| APICTL governance check | Validation output | Warnings/errors reported | PASS |
| APICTL import/update | Successful import | Successful import | PASS |
| Post-import gateway smoke test | HTTP 200 | HTTP 200 | PASS |
| Spring Boot health | UP | UP | PASS |

---

# What Was Not Claimed

The testing documentation intentionally does not claim capabilities that were
not implemented or fully demonstrated.

The current project does not claim:

- native WSO2 OAuth authentication for all API operations
- automated GitHub Actions deployment into the local WSO2 instance
- production TLS certificate validation
- production-scale load testing of WSO2
- multi-node WSO2 gateway testing
- automatic failover between multiple backend instances
- production monitoring infrastructure

These are potential future improvements rather than current test results.

---

# Testing Outcome

The tests demonstrate that the WSO2 integration is functioning as an actual
runtime API management layer rather than only as a configured Publisher
project.

The verified flow is:

```text
API Definition
      |
      v
WSO2 Configuration
      |
      v
Revision Deployment
      |
      v
WSO2 Gateway
      |
      +---- Public request ------> HTTP 200
      |
      +---- Protected request ---> Spring Security
      |                               |
      |                         401 / JWT / 200
      |
      +---- Excess traffic -----> HTTP 429
      |
      +---- Correlation --------> Request trace
      |
      v
Spring Boot Backend
```

Combined with APICTL validation and deployment testing, this provides
repeatable evidence that the Gem Store API can be governed, deployed, invoked,
secured at the application layer, throttled, and traced through WSO2 API
Manager.