package com.neo.ticket.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    OpenAPI neoTickerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Neo Ticker Service")
                        .description("""
                                Secure ticketing and reservation API.

                                Authenticate with `POST /api/auth/login`, then send the
                                returned access token as `Authorization: Bearer <token>`.
                                Writes that create a reservation require an
                                `Idempotency-Key` header so a retried request cannot
                                book the same seats twice.""")
                        .version("1.0.0")
                        .contact(new Contact().name("Neo Ticker Team"))
                        .license(new License().name("Proprietary")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token issued by /api/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
