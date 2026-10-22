package tech.com.project.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import tech.com.project.exception.InvalidStatusTransitionException;
import tech.com.project.model.Ticket;
import tech.com.project.model.TicketAssignment;
import tech.com.project.model.TicketStatus;
import tech.com.project.model.UserAccount;
import tech.com.project.repository.StatusTransitionRepository;
import tech.com.project.repository.TicketAssignmentRepository;
import tech.com.project.service.TicketStateMachineService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketStateMachineServiceImpl implements TicketStateMachineService {

    private final StatusTransitionRepository statusTransitionRepository;
    private final TicketAssignmentRepository ticketAssignmentRepository;

    @Override
    public void validateTransition(Ticket ticket, TicketStatus targetStatus, UserAccount actor, List<String> actorRoles) {
        TicketStatus currentStatus = ticket.getStatus();

        if (currentStatus == null || targetStatus == null) {
            throw new InvalidStatusTransitionException("Los estados actual y destino no pueden ser nulos.");
        }

        String currentName = currentStatus.getName().trim().toUpperCase();
        String targetName = targetStatus.getName().trim().toUpperCase();

        log.info("Validando transición para ticket [{}]: '{}' -> '{}' por usuario '{}' (Roles: {})",
                ticket.getCode(), currentName, targetName, actor.getEmail(), actorRoles);

        if (currentName.equalsIgnoreCase(targetName)) {
            throw new InvalidStatusTransitionException("El ticket ya se encuentra en el estado '" + targetName + "'.");
        }

        if (Boolean.TRUE.equals(currentStatus.getIsFinal())) {
            throw new InvalidStatusTransitionException("El ticket ya está en estado final '" + currentName + "' y no admite transiciones.");
        }

        // Validación estructural: Transición permitida en la base de datos y modelo formal
        boolean transitionExists = statusTransitionRepository.existsByFromStatusAndToStatusAndIsActiveTrue(currentStatus, targetStatus);
        if (!transitionExists) {
            throw new InvalidStatusTransitionException(
                    String.format("Transición inválida: No está permitido saltar de '%s' a '%s'. El ciclo de vida estricto es ABIERTA -> ASIGNADA -> EN_PROCESO -> RESUELTA -> CERRADA.",
                            currentName, targetName)
            );
        }

        // Validación de reglas de negocio por rol y actor (RN-01, RN-02, CA-001)
        switch (currentName) {
            case "ABIERTA" -> {
                if ("ASIGNADA".equals(targetName)) {
                    if (!actorRoles.contains("ADMIN")) {
                        throw new AccessDeniedException("Solo un usuario con rol ADMIN puede asignar un ticket y pasarlo a ASIGNADA (CA-001).");
                    }
                } else {
                    throw new InvalidStatusTransitionException("Desde 'ABIERTA' la única transición válida es hacia 'ASIGNADA'.");
                }
            }

            case "ASIGNADA" -> {
                if ("EN_PROCESO".equals(targetName)) {
                    if (!actorRoles.contains("TECHNICIAN")) {
                        throw new AccessDeniedException("Solo un técnico de soporte puede iniciar la atención del ticket (RN-02).");
                    }
                    validateIsAssignedTechnician(ticket, actor);
                } else {
                    throw new InvalidStatusTransitionException("Desde 'ASIGNADA' la única transición válida es hacia 'EN_PROCESO'.");
                }
            }

            case "EN_PROCESO" -> {
                if ("RESUELTA".equals(targetName)) {
                    if (!actorRoles.contains("TECHNICIAN")) {
                        throw new AccessDeniedException("Solo un técnico de soporte puede marcar el ticket como solucionado (RN-02).");
                    }
                    validateIsAssignedTechnician(ticket, actor);
                } else {
                    throw new InvalidStatusTransitionException("Desde 'EN_PROCESO' la única transición válida es hacia 'RESUELTA'.");
                }
            }

            case "RESUELTA" -> {
                if ("CERRADA".equals(targetName)) {
                    boolean isRequester = ticket.getRequester() != null && ticket.getRequester().getId().equals(actor.getId());
                    boolean isAdmin = actorRoles.contains("ADMIN");

                    if (!isRequester && !isAdmin) {
                        throw new AccessDeniedException("Solo el usuario solicitante o un administrador pueden cerrar el ticket resuelto (RN-02).");
                    }
                } else {
                    throw new InvalidStatusTransitionException("Desde 'RESUELTA' la única transición válida es hacia 'CERRADA'.");
                }
            }

            default -> throw new InvalidStatusTransitionException("El estado actual '" + currentName + "' no admite transiciones.");
        }
    }

    private void validateIsAssignedTechnician(Ticket ticket, UserAccount actor) {
        TicketAssignment currentAssignment = ticketAssignmentRepository
                .findFirstByTicketIdAndUnassignedAtIsNullOrderByAssignedAtDesc(ticket.getId())
                .orElse(null);

        if (currentAssignment == null || !currentAssignment.getTechnician().getId().equals(actor.getId())) {
            throw new AccessDeniedException("Acceso denegado: Solo el técnico asignado a este ticket puede ejecutar esta acción.");
        }
    }
}
