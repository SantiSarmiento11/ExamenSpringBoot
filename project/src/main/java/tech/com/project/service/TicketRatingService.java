package tech.com.project.service;

import tech.com.project.dto.request.TicketRatingRequestDto;
import tech.com.project.dto.response.CsatMetricsResponseDto;
import tech.com.project.dto.response.TicketRatingResponseDto;

/**
 * Servicio para la gestión de calificaciones de satisfacción (CSAT) y métricas.
 */
public interface TicketRatingService {

    TicketRatingResponseDto registrarCalificacion(Integer ticketId, TicketRatingRequestDto request);

    TicketRatingResponseDto obtenerCalificacionTicket(Integer ticketId);

    CsatMetricsResponseDto obtenerMetricasCsat();
}
