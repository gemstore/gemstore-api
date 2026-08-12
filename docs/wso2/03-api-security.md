# API Security

## Overview

Gem Store uses a layered API architecture in which WSO2 API Manager acts as
the API gateway while Spring Security provides the current application-level
authentication and authorization mechanism.

The current security flow is:

```text
Client
   |
   | HTTPS
   | Authorization: Bearer <Gem Store JWT>
   v
WSO2 API Gateway
   |
   | Gateway routing
   | Throttling
   | Gateway policies
   | Correlation handling
   |
   | Forward Authorization header
   v
Spring Boot
   |
   v
JwtFilter
   |
   | Validate token
   | Extract authenticated user
   | Build SecurityContext
   v
Spring Security
   |
   v
Protected Resource
```

This distinction is important: the current Gem Store implementation does not
use WSO2 as the primary identity provider for application users.

Authentication is currently performed by the existing Gem Store JWT security
implementation.

---

## Public and Protected Resources

The Gem Store API contains both public and protected operations.

For example, the public listings endpoint can be accessed through the gateway:

```http
GET /gemstore/1.0.0/api/v1/listings
```

Example gateway URL:

```text
https://localhost:8243/gemstore/1.0.0/api/v1/listings?page=0&size=1
```

A successful request returns:

```text
HTTP/1.1 200
```

This demonstrates that clients can access the Spring Boot Listings API through
the WSO2 Gateway rather than directly through port 8080.

Evidence:

```text
screenshots/09-public-request-200.png
```

---

## Protected Resource

The following endpoint requires an authenticated Gem Store user:

```http
GET /api/v1/listings/my
```

Through WSO2, the request becomes:

```text
GET https://localhost:8243/gemstore/1.0.0/api/v1/listings/my
```

This endpoint provides a useful security test because the same resource can be
tested both without and with a valid JWT.

---

## Request Without JWT

A request was sent without an Authorization header:

```bash
curl -k -i \
  "https://localhost:8243/gemstore/1.0.0/api/v1/listings/my"
```

The response was:

```text
HTTP/1.1 401
```

This demonstrates that an unauthenticated request cannot successfully access
the protected Spring Boot resource.

Evidence:

```text
screenshots/10-protected-request-401.png
```

---

## Request With Gem Store JWT

A valid Gem Store JWT was then supplied using the standard HTTP Authorization
header:

```bash
curl -k -i \
  -H "Authorization: Bearer $JWT" \
  "https://localhost:8243/gemstore/1.0.0/api/v1/listings/my"
```

The response was:

```text
HTTP/1.1 200
```

and the API returned the authenticated user's listing data.

Evidence:

```text
screenshots/11-protected-request-200-jwt.png
```

The test therefore demonstrates:

```text
No JWT
   |
   v
WSO2 Gateway
   |
   v
Spring Security
   |
   v
401 Unauthorized


Valid Gem Store JWT
   |
   v
WSO2 Gateway
   |
   v
Spring Security
   |
   v
200 OK
```

---

## Spring Security Responsibility

The Spring Boot application currently owns application authentication.

The security layer includes the application's JWT filter and Spring Security
configuration.

Conceptually, processing occurs as follows:

```text
HTTP Request
     |
     v
JwtFilter
     |
     | Read Authorization header
     |
     v
Extract Bearer token
     |
     v
Validate JWT
     |
     v
Determine authenticated user
     |
     v
Populate Spring SecurityContext
     |
     v
Controller / Service
```

This allows existing Gem Store authentication to continue working even though
the application is now accessed through WSO2.

---

## Authorization Header Forwarding

The client sends:

```http
Authorization: Bearer <JWT>
```

WSO2 forwards the request to the Spring Boot backend.

Spring Security can therefore process the same application JWT that would have
been used when calling the backend directly.

This provides a useful migration architecture:

```text
Existing application security
             +
       API management layer
```

rather than requiring the application's authentication architecture to be
replaced immediately.

---

## HTTPS Gateway

Clients use the HTTPS WSO2 Gateway endpoint:

```text
https://localhost:8243
```

For example:

```text
https://localhost:8243/gemstore/1.0.0/api/v1/listings
```

The local development environment uses WSO2's development certificate, so
command-line testing currently uses:

```bash
curl -k
```

The `-k` option disables certificate verification and is appropriate only for
this local development/testing environment.

It should not be used as the normal certificate strategy in a production
deployment.

---

## CORS Configuration

The exported API configuration contains gateway CORS configuration.

The allowed development origin is:

```text
http://localhost:5173
```

which corresponds to the local frontend development environment.

Configured methods include:

```text
GET
POST
PUT
DELETE
PATCH
OPTIONS
```

Configured request headers include:

```text
Authorization
Content-Type
Accept
Origin
```

CORS configuration controls which browser origins are permitted to interact
with the API.

It should not be treated as an authentication mechanism.

---

## Rate Limiting as a Security Control

WSO2 also provides gateway-level protection through operation-specific
throttling policies.

Examples include:

```text
POST /api/v1/listings
    → GemStorePostListing10PerMin

GET /api/v1/listings/search
    → GemStoreSearch30PerMin

PUT /api/v1/listings/{id}
    → GemStoreUpdate20PerMin

DELETE /api/v1/listings/{id}
    → GemStoreDelete20PerMin
```

This protects backend resources from excessive request rates independently of
the application's business logic.

Runtime testing confirmed that requests exceeding the configured search limit
eventually received:

```text
HTTP 429
```

The detailed throttling implementation and evidence are documented separately
in:

```text
04-throttling.md
```

---

## Correlation IDs and Security Diagnostics

Gateway correlation logging was enabled to trace individual requests through
WSO2.

A request could be followed using its activity/correlation identifier through
gateway components such as:

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
Backend
```

This is useful for security diagnostics because it provides visibility into
which gateway stages processed a particular request.

Evidence:

```text
screenshots/12-correlation-trace.png
```

---

## APICTL Governance Security Finding

APICTL governance validation was executed before deployment using:

```bash
apictl import api \
  -f wso2/apis/listings-api/GemStoreListingsAPI-1.0.0 \
  -e dev \
  --dry-run \
  -k
```

The WSO2 API Management Guidelines reported:

```text
Security is not enabled for all resources.
```

for operations whose exported API configuration contains:

```yaml
authType: None
```

This finding is intentionally documented rather than hidden.

It identifies an important distinction between the current application security
architecture and native WSO2 gateway authentication.

Evidence:

```text
screenshots/14-apictl-governance-validation.png
```

---

## Current Security Boundary

The current implementation can therefore be summarized as:

```text
                    CURRENT IMPLEMENTATION

Client
   |
   | HTTPS
   v
+---------------------------+
| WSO2 API Gateway          |
|                           |
| Routing                   |
| Throttling                |
| Policies                  |
| Correlation / tracing     |
+-------------+-------------+
              |
              | Authorization: Bearer <Gem Store JWT>
              v
+---------------------------+
| Spring Boot               |
|                           |
| JwtFilter                 |
| Spring Security           |
| Authentication            |
| Authorization             |
+---------------------------+
```

WSO2 currently manages the API boundary, while Spring Boot remains responsible
for validating Gem Store user identity.

---

## Future Native WSO2 Identity Integration

A future production-oriented architecture could move more authentication
responsibility to the API management layer.

For example:

```text
Client
   |
   | OAuth2 access token
   v
WSO2 API Gateway
   |
   | Validate token
   | Validate scopes
   | Enforce API security
   v
Spring Boot
   |
   | Trusted identity context
   | Domain authorization
   v
Business Logic
```

This could introduce:

- WSO2-managed OAuth2 access tokens
- API subscriptions
- scopes
- gateway-level token validation
- application registration through the Developer Portal
- centralized API consumer identity management

Spring Security would still remain useful for domain-specific authorization
inside the application.

This is intentionally documented as future work and is not claimed as part of
the current implementation.

---

## Security Results

The implementation demonstrates several distinct security and governance
controls:

| Control | Implementation | Verified |
|---|---|---|
| HTTPS gateway access | WSO2 Gateway | Yes |
| Protected application endpoint | Spring Security | Yes |
| JWT authentication | Gem Store JWT | Yes |
| Unauthorized request rejection | HTTP 401 | Yes |
| Authenticated request | HTTP 200 | Yes |
| Authorization header forwarding | WSO2 → Spring Boot | Yes |
| CORS configuration | WSO2 API configuration | Yes |
| Operation throttling | WSO2 | Yes |
| Rate-limit rejection | HTTP 429 | Yes |
| Correlation tracing | WSO2 | Yes |
| Governance security validation | APICTL | Yes |
| Native WSO2 OAuth enforcement | Future work | No |

The security architecture therefore combines the existing Gem Store application
security model with gateway-level API management controls while clearly
identifying native WSO2 identity integration as a future enhancement.