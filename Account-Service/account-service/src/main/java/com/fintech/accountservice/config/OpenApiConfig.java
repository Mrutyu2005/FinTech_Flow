package com.fintech.accountservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Account Service API")
                        .description(
                                "Fintech Microservices — Account Service.\n\n" +
                                "**Features:**\n" +
                                "- User registration and JWT login\n" +
                                "- Bank account creation (SAVINGS/CURRENT/FIXED)\n" +
                                "- Deposit and withdrawal with balance validation\n" +
                                "- Role-based access control (USER vs ADMIN)\n" +
                                "- Admin endpoints: manage users and view all accounts\n\n" +
                                "**Auth Flow:** Register → Login → Copy JWT → Click 'Authorize' → Use all endpoints\n\n" +
                                "**Databases:** This service uses `account_db` — fully isolated from Transaction-Service.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Fintech Demo")
                                .email("demo@fintech.com")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .name("bearerAuth")));
    }
}
