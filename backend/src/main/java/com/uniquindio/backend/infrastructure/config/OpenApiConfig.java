package com.uniquindio.backend.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI: declara el esquema Bearer JWT para que Swagger UI muestre el botón
 * "Authorize". Flujo de prueba: POST /api/auth/login -> copiar el token -> Authorize -> pegarlo
 * (sin el prefijo "Bearer ").
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Marketplace Gamer API",
                version = "1.0",
                description = "API REST del marketplace gamer. Rutas protegidas con JWT; "
                        + "el usuario o vendedor se toma del token, no del body ni de la URL."),
        security = @SecurityRequirement(name = OpenApiConfig.ESQUEMA_JWT))
@SecurityScheme(
        name = OpenApiConfig.ESQUEMA_JWT,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Token obtenido en POST /api/auth/login")
public class OpenApiConfig {

    public static final String ESQUEMA_JWT = "bearerAuth";
}