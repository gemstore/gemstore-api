# Business Problem

## Overview

Gem Store is a gemstone marketplace platform where sellers can publish
gemstone listings and users can discover, search, and interact with those
listings.

The platform exposes backend functionality through REST APIs implemented
using Spring Boot. As the platform grows and additional services are
introduced, exposing backend services directly to clients creates several
API management challenges.

## Problem

Directly exposing backend services makes responsibilities such as the
following harder to manage consistently:

- API lifecycle management
- API versioning
- API discovery and documentation
- Gateway-level security
- Request throttling and rate limiting
- API governance
- Deployment and revision management
- Request tracing and observability
- Controlled exposure of backend services

For example, the Listings service runs locally on:

    http://localhost:8080

Clients should not need to depend directly on the internal backend
location.

Instead, the API is exposed through the WSO2 API Gateway using a managed
API context:

    https://localhost:8243/gemstore/1.0.0

This separates the public API interface from the internal implementation.

## Solution

WSO2 API Manager was introduced as the API management layer between API
consumers and the Gem Store backend services.

The request flow becomes:

    Client
      |
      v
    WSO2 API Gateway
      |
      |-- API routing
      |-- throttling
      |-- API lifecycle
      |-- policies
      |-- request correlation
      |
      v
    Spring Boot Gem Store Backend
      |
      v
    Database / Supporting Services

The Spring Boot application remains responsible for business logic,
application authentication and authorization, data validation, and data
access.

WSO2 API Manager provides the API management capabilities around those
services.

## Why This Architecture

This architecture provides a clear separation of responsibilities.

### Spring Boot

Responsible for:

- Gem Store business logic
- Listing management
- Application-level JWT authentication
- Authorization
- Persistence
- Domain validation

### WSO2 API Manager

Responsible for:

- API gateway routing
- API publication
- API lifecycle management
- API revisions and deployments
- API documentation and discovery
- Operation-level throttling
- API governance validation
- Gateway policies
- Request correlation and tracing

## Example

A client requesting listings calls:

    GET https://localhost:8243/gemstore/1.0.0/api/v1/listings

instead of directly calling:

    GET http://localhost:8080/api/v1/listings

WSO2 receives the request and applies the configured gateway policies
before forwarding the request to the Spring Boot service.

This provides a managed API boundary while keeping the backend service
focused on application functionality.

## Outcome

The Gem Store API is no longer treated simply as a collection of Spring
Boot endpoints.

It is managed as a versioned API product with:

- a defined API contract
- lifecycle states
- deployed revisions
- gateway policies
- throttling rules
- developer documentation
- governance validation
- APICTL deployment artifacts
- gateway-level observability

This creates an architecture closer to how APIs are governed and operated
in enterprise environments.