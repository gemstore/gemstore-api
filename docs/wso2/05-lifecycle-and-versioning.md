# API Lifecycle and Versioning

## Overview

Gem Store uses WSO2 API Manager lifecycle, versioning, and revision
capabilities to manage changes to the API independently from the Spring Boot
application.

The currently managed API is:

```text
Name: GemStoreListingsAPI
Display Name: Gem Store Listings API
Version: 1.0.0
Context: /gemstore
Lifecycle State: PUBLISHED
Gateway Environment: Default
```

The externally exposed gateway URL therefore follows the API context and
version:

```text
https://localhost:8243/gemstore/1.0.0/...
```

For example:

```text
https://localhost:8243/gemstore/1.0.0/api/v1/listings
```

---

# Lifecycle, Version and Revision Are Different

WSO2 provides several mechanisms for managing API evolution.

They solve different problems.

```text
Lifecycle
    |
    | Is the API available to consumers?
    |
    +--> CREATED / PUBLISHED / etc.


Version
    |
    | Has the external API contract changed?
    |
    +--> 1.0.0 / 2.0.0 / ...


Revision
    |
    | Which snapshot of this API configuration
    | is currently deployed to a gateway?
    |
    +--> Revision 10 / 11 / 12 / ...
```

These concepts should not be treated as interchangeable.

---

## 1. API Lifecycle

The API lifecycle represents the publication state of an API.

The Gem Store Listings API is currently:

```text
PUBLISHED
```

This means the API has moved beyond API creation/design and is available as a
published API through WSO2.

The API was verified using APICTL:

```bash
apictl get apis -e dev -k
```

The result included:

```text
NAME                  VERSION   CONTEXT     STATUS       PROVIDER

GemStoreListingsAPI   1.0.0     /gemstore   PUBLISHED    admin
```

The same published state is visible through the WSO2 Publisher interface.

Evidence:

```text
screenshots/05-published-state.png
```

---

## Why Lifecycle Management Matters

Lifecycle management separates:

```text
API exists
```

from:

```text
API is ready for consumers
```

An API may be under development without being treated as a currently published
consumer-facing API.

This becomes especially useful when managing multiple APIs and environments.

Conceptually:

```text
Design / configure API
          |
          v
Create API
          |
          v
Test configuration
          |
          v
Publish API
          |
          v
Expose to API consumers
```

---

# 2. API Versioning

The current Gem Store Listings API version is:

```text
1.0.0
```

Versioning represents the external API contract.

For example:

```text
GemStoreListingsAPI
        |
        +---- Version 1.0.0
```

The version is also reflected in the gateway URL:

```text
/gemstore/1.0.0/
```

For example:

```text
GET https://localhost:8243/gemstore/1.0.0/api/v1/listings
```

---

## When a New API Version Would Be Appropriate

A new API version should generally be considered when a change affects the
consumer-facing contract in a way that requires consumers to migrate.

For example, suppose version 1.0.0 provides:

```json
{
  "id": 27,
  "title": "Natural Blue Sapphire",
  "price": 30000
}
```

A compatible internal implementation change does not necessarily require a
new public API version.

However, a breaking contract change might justify introducing another API
version.

Examples include:

- removing an existing endpoint
- removing a response field required by clients
- changing the meaning or type of an existing field
- changing required request fields incompatibly
- changing resource structures in a breaking way

A future major version might therefore be represented as:

```text
GemStoreListingsAPI 1.0.0
GemStoreListingsAPI 2.0.0
```

allowing consumers to migrate deliberately instead of unexpectedly breaking
existing clients.

---

# 3. API Revisions

A revision is different from an API version.

A revision represents a snapshot of the API configuration that can be deployed
to a gateway environment.

The Gem Store project created several revisions while API management features
were being developed.

APICTL was used to inspect them:

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
Deploy current operation-level throttling and gateway
configuration.
```

The API version remained:

```text
1.0.0
```

throughout these changes.

This demonstrates an important distinction:

```text
API Version
1.0.0
   |
   +---- Revision 8
   |
   +---- Revision 9
   |
   +---- Revision 10
   |
   +---- Revision 11
   |
   +---- Revision 12
```

Creating a revision does not automatically mean creating a new consumer-facing
API version.

---

# Why These Changes Used Revisions Instead of Versions

The changes represented by these revisions were mainly API management and
gateway configuration changes.

Examples included:

```text
Correlation policy
        ↓
Revision

Endpoint timeout configuration
        ↓
Revision

Endpoint suspension configuration
        ↓
Revision

Operation throttling
        ↓
Revision
```

These changes did not require introducing:

```text
/gemstore/2.0.0
```

because the consumer-facing API contract remained version 1.0.0.

This allows gateway configuration to evolve without unnecessarily creating a
new public API version.

---

# Revision Deployment

Creating a revision and deploying a revision are also distinct concepts.

Conceptually:

```text
Working API configuration
          |
          | Create revision
          v
      Revision 12
          |
          | Deploy
          v
   Default Gateway
```

APICTL showed:

```text
REVISION   DESCRIPTION                                      GATEWAY_ENVS

12         Deploy current operation-level throttling       [Default]
           and gateway configuration.
```

The `[Default]` value is important because it confirms that Revision 12 is
associated with the Default gateway environment.

Evidence:

```text
screenshots/04-revision-deployment.png
```

---

# Why Deployment Verification Matters

Seeing a configuration inside Publisher does not by itself prove that the
gateway is executing that configuration.

The effective flow is:

```text
Modify API configuration
        |
        v
Working API
        |
        v
Create revision
        |
        v
Deploy revision
        |
        v
Gateway receives configuration
        |
        v
Runtime request
```

This became particularly important during throttling testing.

The operation configuration contained:

```text
GET /api/v1/listings/search
GemStoreSearch30PerMin
```

but runtime throttling needed the relevant API configuration to be deployed to
the gateway.

After the current revision was deployed, runtime testing demonstrated:

```text
Request 33: HTTP 429
Request 34: HTTP 429
Request 35: HTTP 429
```

Therefore, runtime testing was used in addition to Publisher configuration to
verify actual gateway behavior.

---

# Working Copy vs Deployed Revision

The API configuration being edited in Publisher can be understood as the
working configuration.

The gateway serves a deployed revision.

Conceptually:

```text
              WSO2 Publisher

         Working API configuration
                    |
            make configuration
                 changes
                    |
                    v
             Create revision
                    |
                    v
              Revision N
                    |
                    | Deploy
                    v
             Default Gateway
                    |
                    v
               API traffic
```

This provides a controlled boundary between API configuration changes and
runtime gateway deployment.

---

# APICTL and Lifecycle Management

APICTL provides command-line access to the same API management workflow.

The environment was configured as:

```text
dev
```

and API availability was verified with:

```bash
apictl get apis -e dev -k
```

The existing API was exported using:

```bash
apictl export api \
  -n GemStoreListingsAPI \
  -v 1.0.0 \
  -e dev \
  -k
```

The exported API was then stored inside the Git repository under:

```text
wso2/
└── apis/
    └── listings-api/
        └── GemStoreListingsAPI-1.0.0/
```

The artifact contains API configuration such as:

```text
api.yaml
api_meta.yaml
Definitions/swagger.yaml
Policies/
Docs/
```

---

# Updating the API with APICTL

The version-controlled artifact can be imported or updated using:

```bash
apictl import api \
  -f wso2/apis/listings-api/GemStoreListingsAPI-1.0.0 \
  -e dev \
  --update \
  -k
```

This command was successfully tested.

The result was:

```text
Successfully imported API.
```

The API remained:

```text
GemStoreListingsAPI
Version 1.0.0
PUBLISHED
```

and the gateway endpoint continued returning:

```text
HTTP 200
```

after the APICTL update.

This verifies that API configuration can be managed outside the Publisher UI.

---

# Governance Validation Before Deployment

Before importing the API, APICTL dry-run validation was also tested:

```bash
apictl import api \
  -f wso2/apis/listings-api/GemStoreListingsAPI-1.0.0 \
  -e dev \
  --dry-run \
  -k
```

WSO2 governance rules identified warnings such as:

```text
Path parameters should be defined on the path level.
```

and:

```text
Info object should have "license" object.
```

The API management governance rules also identified:

```text
Security is not enabled for all resources.
```

because the current exported operations use:

```yaml
authType: None
```

This validation step is useful because API artifacts can be checked for
governance issues before deployment.

Evidence:

```text
screenshots/14-apictl-governance-validation.png
```

---

# API Version vs Backend Version

The WSO2 API version should also not be confused with the version of the
Spring Boot application itself.

These are separate concerns.

```text
Spring Boot application
        |
        | implementation
        v
Backend deployment


WSO2 API
        |
        | consumer contract
        v
API version 1.0.0
```

A backend implementation can change while the API remains version 1.0.0 as
long as the external contract remains compatible.

For example:

```text
Optimize database query
        ↓
No new API version required


Change internal service implementation
        ↓
No new API version required


Add gateway correlation policy
        ↓
New revision may be appropriate


Change gateway throttling
        ↓
New revision may be appropriate


Remove a required response property
        ↓
Potential breaking API change
        ↓
Consider a new API version
```

---

# Current Development Workflow

The current portfolio workflow is:

```text
Spring Boot / API changes
          |
          v
Generate or update OpenAPI definition
          |
          v
Update WSO2 API configuration
          |
          v
APICTL governance validation
          |
          v
Import/update API
          |
          v
Create/deploy revision when required
          |
          v
Smoke test through :8243
```

APICTL has therefore removed the requirement for every API artifact deployment
to be performed manually through the Publisher interface.

---

# CI/CD Scope

APICTL is suitable for use inside a CI/CD pipeline.

A future fully automated workflow could be:

```text
Developer
   |
   | git push
   v
GitHub
   |
   v
CI Pipeline
   |
   +--> Build Spring Boot
   |
   +--> Run tests
   |
   +--> Validate OpenAPI
   |
   +--> Validate WSO2 artifact
   |
   +--> APICTL import/update
   |
   +--> Deploy revision
   |
   +--> Gateway smoke test
```

However, the current local portfolio environment does not claim that this
entire deployment process runs automatically after every GitHub push.

The local WSO2 API Manager instance is not directly reachable from a normal
cloud-hosted CI runner.

A self-hosted runner or externally reachable deployment environment would be
needed to perform that local deployment automatically.

The project therefore demonstrates the APICTL deployment mechanism without
claiming infrastructure that has not been implemented.

---

# Example Change Decision

A useful decision process for future changes is:

```text
                    API change
                        |
                        v
           Does the external contract
                break consumers?
                  /           \
                No             Yes
                |               |
                v               v
        Keep API version     Consider new
             1.0.0           API version
                |
                v
       Does gateway config
            change?
          /         \
        No           Yes
        |             |
        v             v
   Backend       Create/deploy
   change          revision
```

This prevents unnecessary API versions while still maintaining controlled
gateway deployments.

---

# Current Result

The Gem Store API currently demonstrates:

| Capability | Current State |
|---|---|
| API name | GemStoreListingsAPI |
| API version | 1.0.0 |
| API context | `/gemstore` |
| Lifecycle | PUBLISHED |
| Gateway | Default |
| Multiple revisions | Yes |
| Current observed deployed revision | Revision 12 |
| Version-controlled API artifact | Yes |
| APICTL export | Verified |
| APICTL dry-run validation | Verified |
| APICTL update/import | Verified |
| Runtime gateway smoke test | Verified |
| Fully automated GitHub → local WSO2 deployment | Not implemented |

The implementation therefore demonstrates the distinction between API
lifecycle, consumer-facing versioning, gateway revisions, and deployment while
keeping the documented capabilities aligned with what was actually tested.