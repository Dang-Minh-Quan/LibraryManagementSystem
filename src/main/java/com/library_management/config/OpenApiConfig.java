package com.library_management.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI libraryOpenApi() {
        return new OpenAPI().info(new Info().title("Library Management API").version("v1")
                .description("Auth/User module. Register, log in, then use Authorize with the access token."))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }

    @Bean
    OpenApiCustomizer errorResponseSchemas() {
        return api -> {
            if (api.getPaths() == null) return;
            Schema<?> error = new ObjectSchema()
                    .addProperty("status", new IntegerSchema().example(401))
                    .addProperty("message", new StringSchema().example("Invalid username or password"))
                    .addProperty("timestamp", new DateTimeSchema());
            error.setRequired(List.of("status", "message", "timestamp"));
            api.getComponents().addSchemas("ApiError", error);
            api.getPaths().values().forEach(path -> path.readOperations().forEach(operation ->
                    operation.getResponses().forEach((code, response) -> {
                        if (code.startsWith("4") || code.startsWith("5")) {
                            response.setContent(new Content().addMediaType("application/json",
                                    new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiError"))));
                        }
                    })));
        };
    }
}
