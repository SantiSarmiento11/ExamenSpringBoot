package tech.com.project.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/demo")
@Tag(name = "2. Verificación de Roles y Autorización (RBAC)", description = "Endpoints de demostración para verificar el control de acceso según el rol del usuario")
public class AuthDemoController {

    @GetMapping("/public")
    @Operation(summary = "Endpoint Público", description = "Accesible por cualquier cliente sin necesidad de token JWT.")
    public ResponseEntity<Map<String, String>> publicEndpoint() {
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Acceso público concedido. No se requirió token JWT."
        ));
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(
            summary = "Endpoint para Empleados / Usuarios (USER)",
            description = "Accesible únicamente por usuarios con rol USER o ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Map<String, String>> userEndpoint() {
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Acceso concedido para rol USER.",
                "scope", "Consulta de tickets propios y adición de comentarios"
        ));
    }

    @GetMapping("/technician")
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('ADMIN')")
    @Operation(
            summary = "Endpoint para Técnicos de Soporte (TECHNICIAN)",
            description = "Accesible únicamente por especialistas con rol TECHNICIAN o ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Map<String, String>> technicianEndpoint() {
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Acceso concedido para rol TECHNICIAN.",
                "scope", "Gestión de incidencias asignadas y notas de resolución"
        ));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Endpoint Exclusivo de Administrador (ADMIN)",
            description = "Accesible únicamente por usuarios con rol ADMIN. Si un USER o TECHNICIAN intenta ingresar, obtendrá HTTP 403 Forbidden.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Map<String, String>> adminEndpoint() {
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Acceso concedido para rol ADMIN.",
                "scope", "Asignación manual de técnicos, métricas y administración integral"
        ));
    }
}
