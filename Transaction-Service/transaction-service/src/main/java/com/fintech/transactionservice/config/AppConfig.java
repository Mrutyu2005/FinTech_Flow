package com.fintech.transactionservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    /**
     * RestTemplate with connection and read timeouts.
     * If Account-Service is down, requests will fail fast (3s connect, 5s read)
     * instead of hanging indefinitely — enabling proper fault isolation.
     *
     * Concept: Circuit-breaker behaviour without a full Hystrix/Resilience4j setup.
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);  // 3 seconds to establish connection
        factory.setReadTimeout(5_000);     // 5 seconds to wait for response
        return new RestTemplate(factory);
    }

    /**
     * OpenAPI / Swagger config — registers Bearer JWT authentication scheme.
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Transaction Service API")
                        .description(
                                "Fintech Microservices — Transaction Service.\n\n" +
                                "**Features:**\n" +
                                "- Fund transfers between accounts\n" +
                                "- Daily transaction limit (₹50,000 for USER, ₹200,000 for ADMIN)\n" +
                                "- Transaction audit trail (sender, receiver, amount, status, timestamp)\n" +
                                "- Fault-tolerant inter-service calls to Account-Service\n\n" +
                                "**Auth:** Login via Account-Service (`POST /api/auth/login`) and use the JWT token here.")
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
