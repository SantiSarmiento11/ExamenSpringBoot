package tech.com.project.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.com.project.dto.response.dashboard.DashboardMetricsResponseDto;
import tech.com.project.service.DashboardService;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Endpoints de métricas analíticas e inteligencia operativa para administradores")
@SecurityRequirement(name = "bearerAuth")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener métricas ejecutivas del Dashboard (RF-07)", description = "Retorna métricas cuantitativas de tickets, SLA, tiempos promedio de resolución, distribución por categoría/prioridad y carga de trabajo técnica. Exclusivo para usuarios con rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Métricas consolidadas obtenidas con éxito"),
            @ApiResponse(responseCode = "401", description = "No autenticado o token JWT inválido"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: se requiere rol ADMIN")
    })
    public ResponseEntity<DashboardMetricsResponseDto> getDashboardMetrics() {
        DashboardMetricsResponseDto metrics = dashboardService.getDashboardMetrics();
        return ResponseEntity.ok(metrics);
    }
}
