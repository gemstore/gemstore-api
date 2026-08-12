# CI/CD and API Deployment Automation

## Overview

Gem Store uses WSO2 API Controller (`apictl`) to make API Manager
configuration reproducible and suitable for CI/CD workflows.

Initially, the Gem Store Listings API was configured manually through the
WSO2 Publisher UI.

The workflow was then improved by:

1. installing APICTL
2. registering the local WSO2 development environment
3. exporting the existing API
4. storing the exported API artifact in Git
5. validating the artifact using APICTL governance checks
6. updating WSO2 using APICTL
7. verifying the deployed API through the gateway

This changes the deployment model from:

```text
Developer
    |
    v
WSO2 Publisher UI
    |
    v
Manually configure API
```

toward:

```text
Source Code
    +
WSO2 API Artifact
    |
    v
Git
    |
    v
Validation
    |
    v
APICTL
    |
    v
WSO2 API Manager
    |
    v
Gateway Smoke Test
```

The current portfolio implementation demonstrates the deployment mechanism
needed for CI/CD.

A fully automatic GitHub Actions deployment into the local WSO2 instance has
not been implemented.

---

# 1. APICTL Installation

WSO2 API Controller 4.7.1 was installed on the development machine.

The installation was verified using:

```bash
which apictl
```

Result:

```text
/usr/local/bin/apictl
```

The installed version was verified using:

```bash
apictl version
```

Observed result:

```text
Version: 4.7.1
Build Date: 2026-07-01 06:05:27 UTC
```

APICTL can therefore be invoked globally from the command line.

---

# 2. WSO2 Development Environment Registration

The local WSO2 API Manager environment was registered with APICTL.

Command:

```bash
apictl add env dev \
  --apim https://localhost:9443
```

APICTL automatically configured the default token endpoint:

```text
https://localhost:9443/oauth2/token
```

The configured environments were inspected using:

```bash
apictl get envs
```

The environment was registered as:

```text
Environment: dev
API Manager: https://localhost:9443
Token:       https://localhost:9443/oauth2/token
```

This allows later APICTL commands to use:

```text
-e dev
```

instead of repeatedly supplying the complete WSO2 endpoint configuration.

---

# 3. API Discovery Through APICTL

Connectivity between APICTL and WSO2 API Manager was verified using:

```bash
apictl get apis -e dev -k
```

Observed API:

```text
NAME                  VERSION   CONTEXT     STATUS      PROVIDER

GemStoreListingsAPI   1.0.0     /gemstore   PUBLISHED   admin
```

This verified that APICTL could communicate with the Publisher APIs and
retrieve the Gem Store API.

Evidence:

```text
screenshots/13-apictl-api-list.png
```

---

# 4. Exporting the Existing API

The existing Publisher-managed API was exported using APICTL.

Command:

```bash
apictl export api \
  -n GemStoreListingsAPI \
  -v 1.0.0 \
  -e dev \
  -k
```

APICTL produced:

```text
Successfully exported API!
```

and created:

```text
~/.wso2apictl/exported/apis/dev/GemStoreListingsAPI_1.0.0.zip
```

This converted the API configuration stored in WSO2 into a portable API
artifact.

---

# 5. Exported API Artifact

The exported archive contained configuration including:

```text
GemStoreListingsAPI-1.0.0/
│
├── Definitions/
│   └── swagger.yaml
│
├── Docs/
│
├── Policies/
│   ├── CorrelationIDManagement_v2_api.j2
│   └── CorrelationIDManagement_v2_api.yaml
│
├── api.yaml
│
└── api_meta.yaml
```

The artifact captures important WSO2 API configuration such as:

- API metadata
- API context
- API version
- backend endpoints
- resources and operations
- throttling assignments
- CORS configuration
- API policies
- OpenAPI definition
- documentation metadata

This means important API Manager configuration is no longer represented only
inside the Publisher UI.

---

# 6. Version Controlling the API Artifact

The exported API artifact was placed inside the Gem Store Git repository.

Repository location:

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

This enables API management configuration to evolve together with the
application source code.

Conceptually:

```text
Git Repository
│
├── src/
│   └── Spring Boot application
│
├── pom.xml
│
├── wso2/
│   └── APIs and policies
│
└── docs/
    └── WSO2 architecture and evidence
```

This is an important improvement over maintaining the API exclusively through
manual Publisher configuration.

---

# 7. Governance Validation Before Deployment

Before updating the WSO2 environment, APICTL dry-run validation was tested.

Command:

```bash
apictl import api \
  -f wso2/apis/listings-api/GemStoreListingsAPI-1.0.0 \
  -e dev \
  --dry-run \
  -k
```

The validation ran WSO2 API governance rules against the artifact.

Examples of detected issues included:

```text
Path parameters should be defined on the path level.
```

and:

```text
Info object should have "license" object.
```

The API Management Guidelines also detected:

```text
Security is not enabled for all resources.
```

because the exported API operations currently use:

```yaml
authType: None
```

while Gem Store JWT validation is handled by the Spring Boot application.

The dry-run demonstrates an important CI/CD principle:

```text
API artifact
     |
     v
Validate
     |
     +---- unacceptable result ---> stop deployment
     |
     +---- acceptable result -----> deployment
```

The current portfolio project records these governance findings instead of
claiming a completely warning-free API definition.

Evidence:

```text
screenshots/14-apictl-governance-validation.png
```

---

# 8. API Deployment Using APICTL

The API artifact was then used to update the development WSO2 environment.

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

The `--update` option allows the command to update the existing API rather
than requiring the API to be recreated manually.

This proves the deployment path:

```text
Git-controlled API artifact
           |
           v
        APICTL
           |
           v
    WSO2 Publisher
           |
           v
       Managed API
```

Evidence:

```text
screenshots/15-apictl-deployment.png
```

---

# 9. Post-Deployment API Verification

After the APICTL import/update, the API was queried again.

Command:

```bash
apictl get apis -e dev -k
```

The API remained:

```text
GemStoreListingsAPI
Version 1.0.0
Context /gemstore
PUBLISHED
```

This confirmed that the managed API still existed in the expected lifecycle
state after the update.

---

# 10. Gateway Smoke Test

The deployment process was followed by a runtime smoke test through the WSO2
Gateway.

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

The request returned Gem Store listing data.

The important point is that the smoke test targets:

```text
localhost:8243
```

rather than directly calling:

```text
localhost:8080
```

Therefore the test verifies:

```text
Client
   |
   v
WSO2 Gateway
   |
   v
Spring Boot
   |
   v
HTTP 200
```

rather than only proving that Spring Boot itself is running.

---

# 11. Revision Deployment

API configuration updates also use WSO2 revisions to control what is deployed
to the gateway.

The revision state was inspected using:

```bash
apictl get api-revisions \
  -n GemStoreListingsAPI \
  -v 1.0.0 \
  -e dev \
  -k
```

The current observed deployed revision was:

```text
Revision 12
```

with:

```text
GATEWAY_ENVS [Default]
```

The revision description was:

```text
Deploy current operation-level throttling and gateway configuration.
```

This provides another deployment boundary:

```text
Working API
     |
     v
Create Revision
     |
     v
Revision 12
     |
     v
Deploy
     |
     v
Default Gateway
```

---

# 12. Current Deployment Workflow

The workflow currently demonstrated by the project is:

```text
Developer changes
Spring Boot / OpenAPI / WSO2 configuration
               |
               v
         Git working tree
               |
               v
     API artifact validation
               |
               v
     apictl import --update
               |
               v
         WSO2 API Manager
               |
               v
       Revision deployment
               |
               v
          WSO2 Gateway
               |
               v
          Smoke testing
```

At present, the APICTL commands are executed manually from the developer
machine.

This is already different from manually recreating every API configuration
through Publisher because the deployment artifact itself is portable and
version controlled.

---

# 13. What Has Been Automated

The word "automation" should be used precisely.

APICTL automates API Manager operations that would otherwise require manual
Publisher interaction.

For example:

```text
Manual Publisher workflow:

Open browser
   ↓
Open Publisher
   ↓
Configure API
   ↓
Configure resources
   ↓
Configure policies
   ↓
Save/update
```

APICTL allows the version-controlled artifact to be processed using commands:

```text
API artifact
   ↓
apictl validation
   ↓
apictl import/update
   ↓
WSO2 API Manager
```

Therefore, the deployment mechanism is scriptable and CI/CD-ready.

However, the command is currently started manually.

---

# 14. What Has NOT Been Automated

The current project does NOT implement:

```text
git push
   ↓
GitHub Actions
   ↓
automatically connect to local WSO2
   ↓
APICTL import
   ↓
revision deployment
   ↓
automatic smoke test
```

Therefore the project should not claim:

> Every GitHub push automatically deploys the API to WSO2.

That capability has not been implemented.

The correct description is:

> The WSO2 API configuration is version controlled and can be validated and
> deployed reproducibly using APICTL. The workflow is suitable for CI/CD
> automation, while the current local portfolio environment performs the
> APICTL deployment manually.

---

# 15. Why a Normal GitHub-Hosted Runner Is Not Enough for the Local Setup

The current WSO2 environment runs locally:

```text
https://localhost:9443
```

and the gateway runs at:

```text
https://localhost:8243
```

If a GitHub-hosted Actions runner executed:

```bash
apictl import api -e dev
```

then:

```text
localhost
```

would refer to the GitHub runner itself, not the developer laptop.

Conceptually:

```text
Developer Laptop

localhost:9443
     |
     v
   WSO2


GitHub Runner

localhost:9443
     |
     X
No local WSO2 here
```

Therefore the cloud runner cannot simply use the current local endpoint.

---

# 16. Options for Full Deployment Automation

A future implementation could solve this in several ways.

## Option A — Self-Hosted GitHub Actions Runner

A runner could execute on a machine with network access to WSO2.

```text
GitHub
   |
   | workflow job
   v
Self-hosted Runner
   |
   v
WSO2 :9443
   |
   v
Gateway :8243
```

For the current local development environment, this would be one practical
approach.

---

## Option B — Remote WSO2 Environment

WSO2 could instead run in an environment reachable by the CI runner.

For example:

```text
GitHub Actions
      |
      v
Remote Dev WSO2
      |
      v
Remote Gateway
```

CI credentials and environment configuration would then be supplied securely
through the CI platform rather than hard-coded into the repository.

---

# 17. Proposed Full CI/CD Pipeline

If full automation is added later, the intended pipeline is:

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
    +--------------------------------+
    |                                |
    v                                v
Build Spring Boot             Validate API Artifact
    |                                |
    v                                v
Run Tests                    APICTL Dry Run
    |                                |
    +---------------+----------------+
                    |
                    v
              Deployment Stage
                    |
                    v
            APICTL Import/Update
                    |
                    v
              Create Revision
                    |
                    v
              Deploy Revision
                    |
                    v
                Gateway
                    |
                    v
               Smoke Tests
```

---

# 18. Proposed Pipeline Stages

A production-style pipeline could contain the following stages.

## Stage 1 — Build

```bash
./mvnw clean package
```

Purpose:

```text
Compile application
        +
Package application
```

---

## Stage 2 — Automated Tests

```bash
./mvnw test
```

Purpose:

```text
Unit tests
Integration tests
Regression checks
```

A deployment should not proceed when required tests fail.

---

## Stage 3 — OpenAPI Validation

The generated OpenAPI definition should be checked before updating WSO2.

Conceptually:

```text
Spring Boot API
      |
      v
OpenAPI definition
      |
      v
Validation
```

This catches API contract problems before gateway deployment.

---

## Stage 4 — WSO2 Governance Validation

The API artifact can be checked using:

```bash
apictl import api \
  -f wso2/apis/listings-api/GemStoreListingsAPI-1.0.0 \
  -e dev \
  --dry-run
```

The pipeline could decide whether specific governance findings should block
deployment.

---

## Stage 5 — APICTL Deployment

After successful validation:

```bash
apictl import api \
  -f wso2/apis/listings-api/GemStoreListingsAPI-1.0.0 \
  -e dev \
  --update
```

This updates the WSO2-managed API.

---

## Stage 6 — Revision Deployment

When the API configuration needs to be propagated to the gateway, the
pipeline would create/deploy the appropriate revision.

Conceptually:

```text
Updated working API
       |
       v
Create revision
       |
       v
Deploy revision
       |
       v
Gateway environment
```

Revision rotation can also be incorporated into an automated APICTL workflow
where appropriate.

---

## Stage 7 — Smoke Test

After deployment, the pipeline should verify the gateway rather than assuming
that a successful deployment command means the API works.

Example:

```bash
curl -k --fail \
  "https://localhost:8243/gemstore/1.0.0/api/v1/listings?page=0&size=5"
```

Expected:

```text
HTTP 200
```

Conceptually:

```text
Deployment successful
       |
       v
Gateway smoke test
       |
       +---- 200 ----> Pipeline success
       |
       +---- failure -> Pipeline failure
```

---

# 19. Environment-Specific Configuration

A real multi-environment deployment should avoid hard-coding one backend URL
for every environment.

For example:

```text
Development
    |
    +--> http://localhost:8080


Test
    |
    +--> https://test-api.example.internal


Production
    |
    +--> https://api.example.internal
```

The same logical API can then be promoted while environment-specific
parameters are supplied during deployment.

Conceptually:

```text
              API Artifact
                   |
          +--------+--------+
          |                 |
          v                 v
       Dev Params        Prod Params
          |                 |
          v                 v
      Dev WSO2          Prod WSO2
```

This is preferable to manually editing the API artifact before every
environment deployment.

The current portfolio implementation uses the local development endpoint:

```text
http://localhost:8080
```

Multi-environment parameterization is a future improvement.

---

# 20. Credentials and CI Secrets

Authentication information required by CI/CD should never be committed
directly into Git.

A future pipeline should use protected CI secrets for values such as:

```text
WSO2 username
WSO2 password
WSO2 endpoint
deployment credentials
```

Conceptually:

```text
Git Repository
      |
      | no credentials
      v
Workflow
      |
      +----> CI Secret Store
                    |
                    v
             Deployment credentials
```

This separates deployment configuration from sensitive authentication
material.

---

# 21. Source-Control Strategy

The Git repository acts as the source of truth for both application code and
WSO2 deployment artifacts.

Conceptually:

```text
Commit
  |
  +---- Spring Boot changes
  |
  +---- OpenAPI changes
  |
  +---- WSO2 artifact changes
  |
  +---- Policy changes
  |
  +---- Documentation
```

This provides:

- change history
- reviewability
- reproducibility
- rollback context
- traceability between application and API management changes

---

# 22. Example Future Developer Workflow

Suppose a new endpoint is added:

```text
GET /api/v1/listings/featured
```

A future fully automated process could be:

```text
Developer updates controller
        |
        v
OpenAPI definition changes
        |
        v
WSO2 artifact updated
        |
        v
git commit
        |
        v
git push
        |
        v
CI starts
        |
        +--> build
        |
        +--> tests
        |
        +--> OpenAPI validation
        |
        +--> APICTL governance validation
        |
        +--> APICTL update
        |
        +--> revision deployment
        |
        +--> gateway smoke test
        |
        v
Deployment result
```

This is the target architecture rather than a claim about the current local
implementation.

---

# 23. Current CI/CD Maturity

The current state can be summarized as:

| Capability | Status |
|---|---|
| GitHub Actions CI workflow | Implemented |
| Trigger on push to `main` | Implemented |
| Trigger on pull request to `main` | Implemented |
| JDK 21 setup | Implemented |
| Maven dependency caching | Implemented |
| `mvn clean verify` build and test | Implemented |
| Test profile using `SPRING_PROFILES_ACTIVE=test` | Implemented |
| Failed Surefire report upload | Implemented |
| JAR packaging | Implemented |
| APICTL installed | Implemented |
| WSO2 dev environment configured | Implemented |
| API discovery using APICTL | Implemented |
| API export | Implemented |
| WSO2 artifact stored in Git | Implemented |
| API governance dry-run | Implemented manually |
| APICTL import/update | Implemented manually |
| Gateway revision deployment | Demonstrated |
| Post-deployment smoke testing | Demonstrated manually |
| GitHub Actions → APICTL deployment | Not implemented |
| Automatic GitHub → local WSO2 deployment | Not implemented |
| Self-hosted runner | Not installed |
| Multi-environment promotion | Not implemented |
| Production secret management | Future improvement |

# 24. Why the Current Scope Is Appropriate

The objective of this portfolio implementation is to demonstrate understanding
of an enterprise API deployment workflow without introducing unnecessary
infrastructure solely for demonstration purposes.

The project already demonstrates the important transition:

```text
Manual-only configuration
          |
          v
Portable API artifact
          |
          v
Version control
          |
          v
Governance validation
          |
          v
Scriptable deployment
          |
          v
Gateway verification
```

The missing step is execution of those commands automatically from a remote
CI runner.

Because WSO2 currently runs only on the local development machine, adding that
step would require additional runner or remote-environment infrastructure.

---

# Final CI/CD Architecture

The current implemented architecture is:

```text
                 Git Repository
                       |
          +------------+-------------+
          |                          |
          v                          v
   Spring Boot Code           WSO2 API Artifact
                                     |
                                     v
                             APICTL Dry-Run
                                     |
                                     v
                             APICTL Import
                                     |
                                     v
                            WSO2 API Manager
                                     |
                                     v
                            Gateway Revision
                                     |
                                     v
                             Default Gateway
                                     |
                                     v
                              Smoke Testing
```

The future fully automated architecture is:

```text
Developer
    |
    | git push
    v
GitHub
    |
    v
CI Runner
    |
    +--> Build
    +--> Test
    +--> Validate OpenAPI
    +--> Validate WSO2 Artifact
    |
    v
APICTL
    |
    v
WSO2 Development Environment
    |
    v
Revision Deployment
    |
    v
Gateway Smoke Test
    |
    v
Environment Promotion
```

The APICTL integration therefore provides the deployment foundation required
for CI/CD while keeping the current portfolio claims aligned with the
infrastructure that has actually been implemented and tested.