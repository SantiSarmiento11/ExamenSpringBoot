package tech.com.project.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tech.com.project.dto.request.TicketRatingRequestDto;
import tech.com.project.dto.response.CsatMetricsResponseDto;
import tech.com.project.dto.response.TicketRatingResponseDto;
import tech.com.project.service.TicketRatingService;

/**
 * Controlador REST para el registro y consulta de calificaciones de satisfacción CSAT.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Calificaciones y Satisfacción", description = "Endpoints de calificación de satisfacción del cliente (CSAT)")
@SecurityRequirement(name = "bearerAuth")
public class TicketRatingController {

    private final TicketRatingService ticketRatingService;

    @PostMapping("/api/tickets/{id}/calificacion")
    @Operation(summary = "Registrar calificación CSAT para un ticket cerrado",
            description = "Permite al solicitante calificar un ticket que se encuentra en estado CERRADA (1-5 estrellas).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Calificación registrada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Ticket no cerrado, ya calificado o datos inválidos"),
            @ApiResponse(responseCode = "403", description = "Usuario no es el solicitante del ticket"),
            @ApiResponse(responseCode = "404", description = "Ticket no encontrado")
    })
    public ResponseEntity<TicketRatingResponseDto> registrarCalificacion(
            @PathVariable("id") Integer id,
            @Valid @RequestBody TicketRatingRequestDto request) {
        TicketRatingResponseDto response = ticketRatingService.registrarCalificacion(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/tickets/{id}/calificacion")
    @Operation(summary = "Consultar calificación CSAT de un ticket",
            description = "Retorna el detalle de la calificación registrada para un ticket específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Calificación encontrada"),
            @ApiResponse(responseCode = "404", description = "Calificación o ticket no encontrado")
    })
    public ResponseEntity<TicketRatingResponseDto> obtenerCalificacionTicket(@PathVariable("id") Integer id) {
        TicketRatingResponseDto response = ticketRatingService.obtenerCalificacionTicket(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/admin/metricas-csat")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Consultar métricas consolidadas CSAT",
            description = "Retorna el promedio general de satisfacción y el desglose de calificaciones por técnico. Exclusivo para administradores.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Métricas CSAT obtenidas con éxito"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: se requiere rol ADMIN")
    })
    public ResponseEntity<CsatMetricsResponseDto> obtenerMetricasCsat() {
        CsatMetricsResponseDto response = ticketRatingService.obtenerMetricasCsat();
        return ResponseEntity.ok(response);
    }
}
