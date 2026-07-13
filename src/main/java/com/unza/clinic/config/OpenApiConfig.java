package com.unza.clinic.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("UNZA Clinic Management System API")
                        .version("1.0.0")
                        .description("""
                            Enterprise Hospital Management System API for University of Zambia Clinic.

                            This API provides comprehensive healthcare management capabilities including:
                            - Patient registration and management
                            - Clinical workflow (encounters, triage, emergency)
                            - Laboratory and radiology orders
                            - Pharmacy and inventory management
                            - Billing and tariff management
                            - Staff scheduling and attendance
                            - Reports and analytics
                            - System administration and audit logging

                            ### Authentication
                            All endpoints (except `/api/login`, `/api/health`, `/api/external/**`) require a Bearer JWT token.
                            Obtain a token by POSTing credentials to `/api/login`.

                            ### Base URL
                            Production: `https://api.unzaclinic.zm/api`
                            Development: `http://localhost:8080/api`
                            """)
                        .contact(new Contact()
                                .name("UNZA Clinic IT Support")
                                .email("it@unza.zm")
                                .url("https://unza.zm"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://unza.zm")));
    }
}
