package tech.com.project.config;

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
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TechNova IT Helpdesk - API REST")
                        .version("1.0.0")
                        .description("Especificación interactiva de la API REST para el sistema centralizado de gestión de incidentes y tickets TechNova IT Helpdesk. Cumple con la especificación de requisitos formal ERS v1.0.0 y arquitectura stateless protegida con JWT.")
                        .contact(new Contact()
                                .name("Equipo de Arquitectura e Infraestructura TechNova")
                                .email("soporte@technova.com")
                                .url("https://technova.internal"))
                        .license(new License()
                                .name("Uso Interno Corporativo - TechNova Solutions")
                                .url("https://technova.internal/terms")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Ingrese su token JWT generado en el endpoint `/api/auth/login`. Formato: `Bearer <token>` o simplemente pegue el token.")
                        ));
    }
}
