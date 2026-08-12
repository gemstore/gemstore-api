package com.gemstore.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME =
            "bearerAuth";

    @Bean
    public OpenAPI gemStoreOpenApi() {

        SecurityScheme jwtSecurityScheme =
                new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description(
                                "Enter the Gem Store JWT token"
                        );

        return new OpenAPI()
                .info(new Info()
                        .title("Gem Store REST API")
                        .description(
                                "REST API for the Gem Store gemstone "
                                        + "marketplace platform."
                        )
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Mohammed Musharraf")
                                .url("https://musharraf.dev")
                                .email("mnmmusharraf@gmail.com")
                        )
                )
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        SECURITY_SCHEME_NAME,
                                        jwtSecurityScheme
                                )
                );
    }
}