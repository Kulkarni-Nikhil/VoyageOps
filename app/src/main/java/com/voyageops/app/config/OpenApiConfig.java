package com.voyageops.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI voyageOpsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("VoyageOps API")
                        .description("""
                                Backend API for VoyageOps — a collaborative travel planning platform.
                                Features: trip planning, collaborative memberships, expense & budget tracking,
                                weather forecasts, points-of-interest discovery, Redis caching and background jobs.
                                """)
                        .version("v1.0.0")
                        .contact(new Contact().name("VoyageOps"))
                        .license(new License().name("MIT")));
    }
}