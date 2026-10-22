package tech.com.project.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.com.project.dto.request.TicketRatingRequestDto;
import tech.com.project.dto.response.CsatMetricsResponseDto;
import tech.com.project.dto.response.TechnicianCsatDto;
import tech.com.project.dto.response.TicketRatingResponseDto;
import tech.com.project.exception.ResourceNotFoundException;
import tech.com.project.model.Ticket;
import tech.com.project.model.TicketAssignment;
import tech.com.project.model.TicketRating;
import tech.com.project.model.UserAccount;
import tech.com.project.repository.TicketAssignmentRepository;
import tech.com.project.repository.TicketRatingRepository;
import tech.com.project.repository.TicketRepository;
import tech.com.project.repository.UserAccountRepository;
import tech.com.project.security.services.UserDetailsImpl;
import tech.com.project.service.TicketRatingService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de calificaciones CSAT y métricas de satisfacción.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketRatingServiceImpl implements TicketRatingService {

    private final TicketRatingRepository ticketRatingRepository;
    private final TicketRepository ticketRepository;
    private final UserAccountRepository userAccountRepository;
    private final TicketAssignmentRepository ticketAssignmentRepository;

    @Override
    @Transactional
    public TicketRatingResponseDto registrarCalificacion(Integer ticketId, TicketRatingRequestDto request) {
        // 1. Validar existencia del ticket
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket con ID " + ticketId + " no encontrado."));

        // 2. Validar que el usuario autenticado sea el creador del ticket
        UserAccount currentUser = getAuthenticatedUser();
        if (!ticket.getRequester().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Solo el usuario solicitante puede calificar el ticket.");
        }

        // 3. Validar que el ticket esté en estado 'CERRADA'
        if (ticket.getStatus() == null || !ticket.getStatus().getName().equalsIgnoreCase("CERRADA")) {
            throw new IllegalArgumentException("Solo se pueden calificar tickets en estado CERRADA");
        }

        // 4. Validar que el ticket no haya sido calificado previamente
        if (ticketRatingRepository.existsByTicketId(ticketId)) {
            throw new IllegalArgumentException("El ticket ya cuenta con una calificación registrada.");
        }

        // 5. Identificar el técnico asignado (activo o último asignado)
        UserAccount technician = ticketAssignmentRepository.findFirstByTicketIdAndUnassignedAtIsNullOrderByAssignedAtDesc(ticketId)
                .map(TicketAssignment::getTechnician)
                .orElseGet(() -> ticketAssignmentRepository.findByTicketIdOrderByAssignedAtDesc(ticketId)
                        .stream()
                        .findFirst()
                        .map(TicketAssignment::getTechnician)
                        .orElse(null));

        // 6. Guardar TicketRating con fechaCalificacion = LocalDateTime.now() y retornar TicketRatingResponseDto
        TicketRating rating = TicketRating.builder()
                .ticket(ticket)
                .requester(currentUser)
                .technician(technician)
                .puntuacion(request.getPuntuacion())
                .comentario(request.getComentario())
                .fechaCalificacion(LocalDateTime.now())
                .build();

        TicketRating savedRating = ticketRatingRepository.save(rating);
        log.info("Calificación registrada con éxito para el ticket {} (ID: {}) con puntuación {}",
                ticket.getCode(), ticketId, savedRating.getPuntuacion());

        return mapToResponseDto(savedRating);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketRatingResponseDto obtenerCalificacionTicket(Integer ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new ResourceNotFoundException("Ticket con ID " + ticketId + " no encontrado.");
        }

        return ticketRatingRepository.findByTicketId(ticketId)
                .map(this::mapToResponseDto)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró calificación para el ticket con ID " + ticketId));
    }

    @Override
    @Transactional(readOnly = true)
    public CsatMetricsResponseDto obtenerMetricasCsat() {
        Double averageRating = ticketRatingRepository.getAverageRating();
        Long totalRatings = ticketRatingRepository.countTotalRatings();

        List<Object[]> rawPerTechnician = ticketRatingRepository.getAverageRatingPerTechnician();
        List<TechnicianCsatDto> desglosePorTecnico = rawPerTechnician.stream().map(row -> {
            Integer techId = (Integer) row[0];
            String firstName = (String) row[1];
            String lastName = (String) row[2];
            Double avg = row[3] != null ? Math.round(((Number) row[3]).doubleValue() * 100.0) / 100.0 : 0.0;
            Long count = row[4] != null ? ((Number) row[4]).longValue() : 0L;
            return TechnicianCsatDto.builder()
                    .technicianId(techId)
                    .technicianName((firstName + " " + lastName).trim())
                    .promedio(avg)
                    .totalCalificaciones(count)
                    .build();
        }).collect(Collectors.toList());

        return CsatMetricsResponseDto.builder()
                .promedioGeneral(averageRating != null ? Math.round(averageRating * 100.0) / 100.0 : 0.0)
                .totalCalificaciones(totalRatings != null ? totalRatings : 0L)
                .desglosePorTecnico(desglosePorTecnico)
                .build();
    }

    private TicketRatingResponseDto mapToResponseDto(TicketRating rating) {
        Integer technicianId = null;
        String technicianName = null;
        if (rating.getTechnician() != null) {
            technicianId = rating.getTechnician().getId();
            technicianName = (rating.getTechnician().getFirstName() + " " + rating.getTechnician().getLastName()).trim();
        }

        String requesterName = null;
        if (rating.getRequester() != null) {
            requesterName = (rating.getRequester().getFirstName() + " " + rating.getRequester().getLastName()).trim();
        }

        return TicketRatingResponseDto.builder()
                .id(rating.getId())
                .ticketId(rating.getTicket() != null ? rating.getTicket().getId() : null)
                .puntuacion(rating.getPuntuacion())
                .comentario(rating.getComentario())
                .fechaCalificacion(rating.getFechaCalificacion())
                .requesterId(rating.getRequester() != null ? rating.getRequester().getId() : null)
                .requesterName(requesterName)
                .technicianId(technicianId)
                .technicianName(technicianName)
                .build();
    }

    private UserDetailsImpl getAuthenticatedUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            throw new AccessDeniedException("No se encuentra una sesión de usuario válida o autenticada.");
        }
        return userDetails;
    }

    private UserAccount getAuthenticatedUser() {
        UserDetailsImpl userDetails = getAuthenticatedUserDetails();
        return userAccountRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("El usuario de la sesión actual no fue encontrado en la base de datos."));
    }
}
