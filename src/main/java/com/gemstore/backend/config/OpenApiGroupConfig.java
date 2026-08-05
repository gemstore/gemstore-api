package com.gemstore.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.models.GroupedOpenApi;

@Configuration
public class OpenApiGroupConfig {

    @Bean
    public GroupedOpenApi listingApi() {

        return GroupedOpenApi.builder()
                .group("listings")
                .pathsToMatch("/api/v1/listings/**")
                .build();
    }
}
