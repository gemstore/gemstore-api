# API Throttling and Rate Limiting

## Overview

Gem Store uses WSO2 API Manager to enforce operation-level rate limiting at
the API Gateway.

Rate limiting protects backend services from excessive request traffic and
allows different API operations to have different traffic limits based on
their purpose and expected usage.

The basic request flow is:

```text
Client
   |
   | API Request
   v
WSO2 API Gateway
   |
   | Identify API operation
   |
   v
ThrottleHandler
   |
   | Evaluate configured policy
   |
   +-----------------------+
   |                       |
Within limit          Limit exceeded
   |                       |
   v                       v
Forward request        HTTP 429
   |                   Too Many Requests
   v
Spring Boot
```

This means excessive traffic can be rejected by the gateway before it reaches
the Gem Store backend.

---

## Why Operation-Level Throttling

Not every API operation has the same cost or risk.

For example:

```text
GET /api/v1/listings
```

is a general read operation and can reasonably support more requests.

However:

```text
POST /api/v1/listings
```

creates application data and should have a lower request limit.

Similarly:

```text
GET /api/v1/listings/search
```

may cause database filtering and search processing, so it has its own policy.

For this reason, Gem Store uses different throttling policies for different
operations instead of applying one identical limit to the entire API.

---

## Configured Policies

The WSO2 API artifact contains the following operation-level throttling
configuration.

| HTTP Method | Resource | Throttling Policy |
|---|---|---|
| GET | `/api/v1/listings` | `GemStoreListing100PerMin` |
| POST | `/api/v1/listings` | `GemStorePostListing10PerMin` |
| PUT | `/api/v1/listings/{id}` | `GemStoreUpdate20PerMin` |
| PATCH | `/api/v1/listings/{id}` | `GemStorePatch20PerMin` |
| DELETE | `/api/v1/listings/{id}` | `GemStoreDelete20PerMin` |
| GET | `/api/v1/listings/search` | `GemStoreSearch30PerMin` |
| POST | `/api/v1/listings/upload` | `GemStoreSearch5PerMin` |

Some other operations currently use:

```text
Unlimited
```

because custom limits were not configured for every resource during this
portfolio implementation.

---

## WSO2 Advanced Throttling Policies

The custom policies were configured through the WSO2 API Manager Admin Portal.

Examples include:

```text
GemStoreDelete20PerMin
    Request Count: 20
    Time Unit: 1 minute

GemStoreListing100PerMin
    Request Count: 100
    Time Unit: 1 minute

GemStorePatch20PerMin
    Request Count: 20
    Time Unit: 1 minute

GemStorePostListing10PerMin
    Request Count: 10
    Time Unit: 1 minute

GemStoreSearch30PerMin
    Request Count: 30
    Time Unit: 1 minute

GemStoreUpdate20PerMin
    Request Count: 20
    Time Unit: 1 minute
```

These policies are managed centrally by WSO2 rather than being hard-coded
inside the Spring Boot controllers.

Evidence:

```text
screenshots/07-throttling-policies.png
```

---

## API Operation Configuration

The exported WSO2 API configuration stores the policy associated with each
operation.

For example:

```yaml
target: /api/v1/listings/search
verb: GET
authType: None
throttlingPolicy: GemStoreSearch30PerMin
```

and:

```yaml
target: /api/v1/listings
verb: POST
authType: None
throttlingPolicy: GemStorePostListing10PerMin
```

This configuration is stored in:

```text
wso2/apis/listings-api/GemStoreListingsAPI-1.0.0/api.yaml
```

Therefore, the API's operation-level throttling assignments are represented
inside the version-controlled WSO2 API artifact.

---

## Runtime Throttling Test

The search endpoint was selected to verify throttling behavior.

The configured operation was:

```text
GET /api/v1/listings/search

Policy:
GemStoreSearch30PerMin
```

The test repeatedly invoked the API through the WSO2 HTTPS Gateway:

```bash
for i in {1..35}; do
  echo -n "Request $i: "

  curl -k -s -o /dev/null \
    -w "HTTP %{http_code}\n" \
    "https://localhost:8243/gemstore/1.0.0/api/v1/listings/search?query=ruby&page=0&size=5"
done
```

The important point is that the request was sent through:

```text
localhost:8243
```

rather than directly to:

```text
localhost:8080
```

Therefore, the request passed through the WSO2 Gateway and its throttling
pipeline.

---

## Test Result

During the captured test, successful responses were initially returned:

```text
Request 13: HTTP 200
Request 14: HTTP 200
Request 15: HTTP 200
...
Request 30: HTTP 200
Request 31: HTTP 200
Request 32: HTTP 200
```

The gateway then began rejecting requests:

```text
Request 33: HTTP 429
Request 34: HTTP 429
Request 35: HTTP 429
```

This provides runtime evidence that the configured gateway throttling was
actively enforcing a request limit.

Evidence:

```text
screenshots/08-throttling-429.png
```

---

## Why the 429 Did Not Appear Exactly at Request 31

The policy is configured as:

```text
30 requests / minute
```

but the captured terminal sequence showed HTTP 429 beginning at request 33.

This should not be interpreted as a test proving that exactly 32 requests are
permitted by a 30-request policy.

The terminal output is evidence that throttling occurred, but the exact
request number at which a gateway rejects traffic can depend on the active
throttling window and runtime state when the test begins.

For portfolio purposes, the important verified behavior is:

```text
Requests accepted
       |
       v
Threshold reached
       |
       v
WSO2 rejects subsequent traffic
       |
       v
HTTP 429
```

The test demonstrates enforcement rather than attempting to benchmark the
internal throttling algorithm.

---

## HTTP 429

When the request limit is exceeded, WSO2 rejects additional requests with:

```text
HTTP 429 Too Many Requests
```

The significance of this response is that the gateway can stop excessive
traffic before the request needs to be processed by the Spring Boot business
logic.

Conceptually:

```text
Normal traffic

Client
   |
   v
WSO2
   |
   v
Spring Boot
   |
   v
HTTP 200


Excess traffic

Client
   |
   v
WSO2
   |
   X  Rate limit exceeded
   |
   +----> HTTP 429

Spring Boot is not required to process the rejected request.
```

---

## Separation from Spring Boot

The rate limits are not implemented inside `ListingController`.

This is an important architectural separation.

Without an API gateway, application code might need to handle concerns such as:

```text
request counting
rate-limit storage
traffic policies
429 generation
policy administration
```

With WSO2:

```text
Spring Boot
    → business logic

WSO2
    → API traffic governance
```

This keeps traffic-management policy outside the application's domain logic.

---

## Different Limits for Different Operations

Operation-level policies allow the API to reflect the characteristics of
different endpoints.

For example:

```text
Read listings
GET /listings
        ↓
100 requests/minute


Search listings
GET /listings/search
        ↓
30 requests/minute


Create listing
POST /listings
        ↓
10 requests/minute


Modify listing
PUT/PATCH /listings/{id}
        ↓
20 requests/minute


Delete listing
DELETE /listings/{id}
        ↓
20 requests/minute
```

This is more flexible than using one global rate limit for every operation.

---

## Deployment and Throttling

Changes to API configuration must reach the deployed gateway revision before
runtime behavior reflects the updated API configuration.

During testing, a new API revision was deployed to the Default gateway.

APICTL confirmed the deployed revision with the gateway environment:

```text
REVISION 12
GATEWAY_ENVS [Default]
```

After the current configuration was deployed, runtime testing successfully
produced HTTP 429 responses.

This was an important verification step because the configuration visible in
Publisher is not, by itself, proof that the gateway is executing that exact
revision.

---

## Throttling in the Gateway Processing Trace

WSO2 correlation tracing also showed the gateway throttling component being
executed during request processing.

The trace included:

```text
ThrottleHandler
doThrottle
```

as part of the request pipeline.

The observed flow included components such as:

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

Evidence:

```text
screenshots/12-correlation-trace.png
```

This complements the HTTP 429 test by showing throttling as part of the
gateway request-processing path.

---

## Development Environment

The current throttling configuration is intended to demonstrate API management
capabilities in the local Gem Store portfolio environment.

The current architecture is:

```text
Client
   |
   v
https://localhost:8243
   |
   v
WSO2 API Gateway
   |
   | throttling
   v
http://localhost:8080
   |
   v
Spring Boot
```

The policy values used here are demonstration values and should not
automatically be treated as production capacity limits.

Production rate limits should normally be selected using factors such as:

- expected traffic
- backend capacity
- database capacity
- operation cost
- consumer requirements
- service-level objectives
- load-test results

---

## Result

The Gem Store WSO2 integration demonstrates real gateway-level rate limiting.

The implementation provides:

- centrally managed throttling policies
- different limits for different API operations
- policy assignments stored in the API artifact
- enforcement at the API Gateway
- HTTP 429 responses when traffic is throttled
- correlation evidence showing the `ThrottleHandler`
- separation of traffic governance from Spring Boot business logic

The successful runtime test confirms that throttling is not only configured
in the WSO2 management interface but is actively enforced by the deployed
gateway.