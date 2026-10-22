package tech.com.project.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.com.project.dto.request.TicketAssignRequestDto;
import tech.com.project.dto.request.TicketCreateRequestDto;
import tech.com.project.dto.request.TicketStatusUpdateRequestDto;
import tech.com.project.dto.response.PagedResponseDto;
import tech.com.project.dto.response.TicketHistoryResponseDto;
import tech.com.project.dto.response.TicketResponseDto;
import tech.com.project.exception.InvalidStatusTransitionException;
import tech.com.project.exception.ResourceNotFoundException;
import tech.com.project.model.*;
import tech.com.project.repository.*;
import tech.com.project.repository.specification.TicketSpecification;
import tech.com.project.security.services.UserDetailsImpl;
import tech.com.project.service.TicketService;
import tech.com.project.service.TicketStateMachineService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final TicketStatusRepository ticketStatusRepository;
    private final CategoryRepository categoryRepository;
    private final PriorityRepository priorityRepository;
    private final UserAccountRepository userAccountRepository;
    private final TicketAssignmentRepository ticketAssignmentRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final TicketStateMachineService ticketStateMachineService;

    @Override
    @Transactional
    public TicketResponseDto createTicket(TicketCreateRequestDto request) {
        UserAccount currentUser = getAuthenticatedUser();
        log.info("Creando ticket por usuario: {} [{}]", currentUser.getEmail(), currentUser.getId());

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría con ID " + request.getCategoryId() + " no encontrada."));

        if (Boolean.FALSE.equals(category.getIsActive())) {
            throw new IllegalArgumentException("La categoría seleccionada no se encuentra activa.");
        }

        Priority priority = priorityRepository.findById(request.getPriorityId())
                .orElseThrow(() -> new ResourceNotFoundException("Prioridad con ID " + request.getPriorityId() + " no encontrada."));

        TicketStatus statusAbierta = ticketStatusRepository.findByName("ABIERTA")
                .orElseThrow(() -> new ResourceNotFoundException("Estado inicial 'ABIERTA' no configurado en la base de datos."));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueDate = calculateDueDate(priority, now);
        String code = generateNextTicketCode();

        Ticket ticket = Ticket.builder()
                .code(code)
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .category(category)
                .priority(priority)
                .status(statusAbierta)
                .requester(currentUser)
                .dueDate(dueDate)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);

        // Registro de auditoría inicial (RF-05)
        TicketHistory initialHistory = TicketHistory.builder()
                .ticket(savedTicket)
                .previousStatus(null)
                .newStatus(statusAbierta)
                .changedBy(currentUser)
                .changedAt(now)
                .build();
        ticketHistoryRepository.save(initialHistory);

        log.info("Ticket creado exitosamente con código [{}] y fecha límite SLA [{}]", code, dueDate);
        return mapToResponseDto(savedTicket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponseDto> getTickets() {
        UserDetailsImpl userDetails = getAuthenticatedUserDetails();
        Integer currentUserId = userDetails.getId();
        List<String> roles = userDetails.getRoleNames();

        List<Ticket> tickets;

        if (roles.contains("ADMIN")) {
            log.debug("Listando todos los tickets para rol ADMIN [{}]", currentUserId);
            tickets = ticketRepository.findAllByOrderByCreatedAtDesc();
        } else if (roles.contains("TECHNICIAN")) {
            log.debug("Listando tickets asignados y propios para TECHNICIAN [{}]", currentUserId);
            tickets = ticketRepository.findActiveByTechnicianIdOrRequesterId(currentUserId);
        } else {
            log.debug("Listando tickets propios para USER [{}]", currentUserId);
            tickets = ticketRepository.findByRequesterIdOrderByCreatedAtDesc(currentUserId);
        }

        return mapTicketsToDtos(tickets);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponseDto getTicketById(Integer id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket con ID " + id + " no encontrado."));

        UserDetailsImpl userDetails = getAuthenticatedUserDetails();
        Integer currentUserId = userDetails.getId();
        List<String> roles = userDetails.getRoleNames();

        // Verificación de autorización de acceso
        if (!roles.contains("ADMIN")) {
            boolean isRequester = ticket.getRequester() != null && ticket.getRequester().getId().equals(currentUserId);
            if (roles.contains("TECHNICIAN")) {
                boolean isAssigned = ticketAssignmentRepository.existsByTicketIdAndTechnicianIdAndUnassignedAtIsNull(ticket.getId(), currentUserId);
                if (!isAssigned && !isRequester) {
                    throw new AccessDeniedException("No cuenta con permisos para ver este ticket ya que no está asignado a usted ni es su solicitante.");
                }
            } else {
                if (!isRequester) {
                    throw new AccessDeniedException("No cuenta con permisos para consultar este ticket ajeno.");
                }
            }
        }

        return mapToResponseDto(ticket);
    }

    @Override
    @Transactional
    public TicketResponseDto assignTicket(Integer id, TicketAssignRequestDto request) {
        UserDetailsImpl userDetails = getAuthenticatedUserDetails();
        if (!userDetails.getRoleNames().contains("ADMIN")) {
            throw new AccessDeniedException("Solo un usuario con rol ADMIN puede asignar técnicos a los tickets (CA-001).");
        }

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket con ID " + id + " no encontrado."));

        if (Boolean.TRUE.equals(ticket.getStatus().getIsFinal())) {
            throw new InvalidStatusTransitionException("No se puede asignar un ticket que se encuentra en estado final: " + ticket.getStatus().getName());
        }

        UserAccount technician = userAccountRepository.findById(request.getTechnicianId())
                .orElseThrow(() -> new ResourceNotFoundException("Técnico con ID " + request.getTechnicianId() + " no encontrado."));

        boolean hasTechRole = technician.getUserRoles().stream()
                .anyMatch(ur -> "TECHNICIAN".equalsIgnoreCase(ur.getRole().getName()));

        if (!hasTechRole) {
            throw new IllegalArgumentException("El usuario con ID " + request.getTechnicianId() + " no tiene el rol de TECHNICIAN.");
        }

        if (Boolean.FALSE.equals(technician.getIsActive())) {
            throw new IllegalArgumentException("El técnico seleccionado no tiene su cuenta activa.");
        }

        UserAccount currentAdmin = getAuthenticatedUser();
        TicketStatus statusAsignada = ticketStatusRepository.findByName("ASIGNADA")
                .orElseThrow(() -> new ResourceNotFoundException("Estado 'ASIGNADA' no encontrado en el sistema."));

        TicketStatus previousStatus = ticket.getStatus();

        // Si el estado es ABIERTA, validar la transición formal hacia ASIGNADA
        if ("ABIERTA".equalsIgnoreCase(previousStatus.getName())) {
            ticketStateMachineService.validateTransition(ticket, statusAsignada, currentAdmin, userDetails.getRoleNames());
            ticket.setStatus(statusAsignada);
        }

        LocalDateTime now = LocalDateTime.now();
        ticket.setUpdatedAt(now);

        // Desasignar técnico previo si existiera una asignación activa
        ticketAssignmentRepository.findFirstByTicketIdAndUnassignedAtIsNullOrderByAssignedAtDesc(ticket.getId())
                .ifPresent(assignment -> {
                    assignment.setUnassignedAt(now);
                    ticketAssignmentRepository.save(assignment);
                });

        // Registrar nueva asignación formal (RF-03, CA-001)
        TicketAssignment newAssignment = TicketAssignment.builder()
                .ticket(ticket)
                .technician(technician)
                .assignedBy(currentAdmin)
                .assignedAt(now)
                .build();
        ticketAssignmentRepository.save(newAssignment);

        // Registrar en historial de auditoría
        TicketHistory history = TicketHistory.builder()
                .ticket(ticket)
                .previousStatus(previousStatus)
                .newStatus(statusAsignada)
                .changedBy(currentAdmin)
                .changedAt(now)
                .build();
        ticketHistoryRepository.save(history);

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket [{}] asignado al técnico [{}] por el administrador [{}]", ticket.getCode(), technician.getEmail(), currentAdmin.getEmail());
        return mapToResponseDto(savedTicket);
    }

    @Override
    @Transactional
    public TicketResponseDto updateTicketStatus(Integer id, TicketStatusUpdateRequestDto request) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket con ID " + id + " no encontrado."));

        String targetStatusName = request.getStatus().trim().toUpperCase();

        if ("ASIGNADA".equalsIgnoreCase(targetStatusName)) {
            throw new InvalidStatusTransitionException("La transición al estado ASIGNADA se realiza exclusivamente mediante la asignación de un técnico (PUT /api/tickets/" + id + "/assign) conforme a RN-02 y CA-001.");
        }

        TicketStatus targetStatus = ticketStatusRepository.findByName(targetStatusName)
                .orElseThrow(() -> new ResourceNotFoundException("Estado destino '" + targetStatusName + "' no existe en el catálogo."));

        UserDetailsImpl userDetails = getAuthenticatedUserDetails();
        UserAccount currentUser = getAuthenticatedUser();

        // Validación estricta de la máquina de estados por rol y permisos (RN-02)
        ticketStateMachineService.validateTransition(ticket, targetStatus, currentUser, userDetails.getRoleNames());

        TicketStatus previousStatus = ticket.getStatus();
        LocalDateTime now = LocalDateTime.now();

        ticket.setStatus(targetStatus);
        ticket.setUpdatedAt(now);
        Ticket savedTicket = ticketRepository.save(ticket);

        // Registro de auditoría en historial (RF-05)
        TicketHistory history = TicketHistory.builder()
                .ticket(savedTicket)
                .previousStatus(previousStatus)
                .newStatus(targetStatus)
                .changedBy(currentUser)
                .changedAt(now)
                .build();
        ticketHistoryRepository.save(history);

        log.info("Estado del ticket [{}] actualizado de [{}] a [{}] por usuario [{}]",
                ticket.getCode(), previousStatus.getName(), targetStatusName, currentUser.getEmail());
        return mapToResponseDto(savedTicket);
    }

    private LocalDateTime calculateDueDate(Priority priority, LocalDateTime baseTime) {
        if (priority == null || priority.getName() == null) {
            return baseTime.plusHours(24);
        }
        String pName = priority.getName().trim().toUpperCase();
        return switch (pName) {
            case "CRITICA" -> baseTime.plusHours(1);
            case "ALTA" -> baseTime.plusHours(2);
            case "MEDIA" -> baseTime.plusHours(8);
            case "BAJA" -> baseTime.plusHours(24);
            default -> baseTime.plusHours(24);
        };
    }

    private synchronized String generateNextTicketCode() {
        Optional<Ticket> topTicket = ticketRepository.findTopByOrderByIdDesc();
        long nextNum = 1;
        if (topTicket.isPresent()) {
            String lastCode = topTicket.get().getCode();
            if (lastCode != null && lastCode.startsWith("TK-")) {
                try {
                    nextNum = Long.parseLong(lastCode.substring(3)) + 1;
                } catch (NumberFormatException e) {
                    nextNum = ticketRepository.count() + 1;
                }
            } else {
                nextNum = ticketRepository.count() + 1;
            }
        }

        String code = String.format("TK-%04d", nextNum);
        while (ticketRepository.existsByCode(code)) {
            nextNum++;
            code = String.format("TK-%04d", nextNum);
        }
        return code;
    }

    private List<TicketResponseDto> mapTicketsToDtos(List<Ticket> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> ticketIds = tickets.stream().map(Ticket::getId).toList();
        List<TicketAssignment> activeAssignments = ticketAssignmentRepository.findActiveAssignmentsByTicketIds(ticketIds);
        Map<Integer, TicketAssignment> assignmentMap = activeAssignments.stream()
                .collect(Collectors.toMap(a -> a.getTicket().getId(), a -> a, (a1, a2) -> a1));

        return tickets.stream()
                .map(ticket -> mapToResponseDto(ticket, assignmentMap.get(ticket.getId())))
                .collect(Collectors.toList());
    }

    private TicketResponseDto mapToResponseDto(Ticket ticket) {
        TicketAssignment activeAssignment = ticketAssignmentRepository
                .findFirstByTicketIdAndUnassignedAtIsNullOrderByAssignedAtDesc(ticket.getId())
                .orElse(null);
        return mapToResponseDto(ticket, activeAssignment);
    }

    private TicketResponseDto mapToResponseDto(Ticket ticket, TicketAssignment activeAssignment) {
        Integer techId = null;
        String techName = null;
        String techEmail = null;

        if (activeAssignment != null && activeAssignment.getTechnician() != null) {
            UserAccount tech = activeAssignment.getTechnician();
            techId = tech.getId();
            techName = tech.getFirstName() + " " + tech.getLastName();
            techEmail = tech.getEmail();
        }

        String requesterFullName = ticket.getRequester() != null
                ? ticket.getRequester().getFirstName() + " " + ticket.getRequester().getLastName()
                : null;

        return TicketResponseDto.builder()
                .id(ticket.getId())
                .code(ticket.getCode())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .categoryId(ticket.getCategory() != null ? ticket.getCategory().getId() : null)
                .categoryName(ticket.getCategory() != null ? ticket.getCategory().getName() : null)
                .priorityId(ticket.getPriority() != null ? ticket.getPriority().getId() : null)
                .priorityName(ticket.getPriority() != null ? ticket.getPriority().getName() : null)
                .statusId(ticket.getStatus() != null ? ticket.getStatus().getId() : null)
                .statusName(ticket.getStatus() != null ? ticket.getStatus().getName() : null)
                .requesterId(ticket.getRequester() != null ? ticket.getRequester().getId() : null)
                .requesterName(requesterFullName)
                .requesterEmail(ticket.getRequester() != null ? ticket.getRequester().getEmail() : null)
                .assignedTechnicianId(techId)
                .assignedTechnicianName(techName)
                .assignedTechnicianEmail(techEmail)
                .dueDate(ticket.getDueDate())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponseDto> searchTickets(
            Integer categoryId,
            Integer priorityId,
            Integer statusId,
            Integer technicianId,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            String query
    ) {
        Specification<Ticket> spec = buildRoleSecuredSpecification(categoryId, priorityId, statusId, technicianId, dateFrom, dateTo, query);
        List<Ticket> tickets = ticketRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
        return mapTicketsToDtos(tickets);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponseDto<TicketResponseDto> searchTicketsPaged(
            Integer categoryId,
            Integer priorityId,
            Integer statusId,
            Integer technicianId,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            String query,
            Pageable pageable
    ) {
        Specification<Ticket> spec = buildRoleSecuredSpecification(categoryId, priorityId, statusId, technicianId, dateFrom, dateTo, query);
        Page<Ticket> page = ticketRepository.findAll(spec, pageable);
        List<TicketResponseDto> dtos = mapTicketsToDtos(page.getContent());
        return PagedResponseDto.<TicketResponseDto>builder()
                .content(dtos)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketHistoryResponseDto> getTicketHistory(Integer id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket con ID " + id + " no encontrado."));

        UserDetailsImpl userDetails = getAuthenticatedUserDetails();
        Integer currentUserId = userDetails.getId();
        List<String> roles = userDetails.getRoleNames();

        if (!roles.contains("ADMIN")) {
            boolean isRequester = ticket.getRequester() != null && ticket.getRequester().getId().equals(currentUserId);
            if (roles.contains("TECHNICIAN")) {
                boolean isAssigned = ticketAssignmentRepository.existsByTicketIdAndTechnicianIdAndUnassignedAtIsNull(ticket.getId(), currentUserId);
                if (!isAssigned && !isRequester) {
                    throw new AccessDeniedException("No cuenta con permisos para ver el historial de este ticket ya que no está asignado a usted ni es su solicitante.");
                }
            } else {
                if (!isRequester) {
                    throw new AccessDeniedException("No cuenta con permisos para consultar el historial de un ticket ajeno.");
                }
            }
        }

        List<TicketHistory> histories = ticketHistoryRepository.findByTicketIdOrderByChangedAtAsc(id);
        return histories.stream()
                .map(h -> TicketHistoryResponseDto.builder()
                        .id(h.getId())
                        .ticketId(ticket.getId())
                        .ticketCode(ticket.getCode())
                        .previousStatusId(h.getPreviousStatus() != null ? h.getPreviousStatus().getId() : null)
                        .previousStatusName(h.getPreviousStatus() != null ? h.getPreviousStatus().getName() : null)
                        .newStatusId(h.getNewStatus() != null ? h.getNewStatus().getId() : null)
                        .newStatusName(h.getNewStatus() != null ? h.getNewStatus().getName() : null)
                        .changedById(h.getChangedBy() != null ? h.getChangedBy().getId() : null)
                        .changedByName(h.getChangedBy() != null ? h.getChangedBy().getFirstName() + " " + h.getChangedBy().getLastName() : null)
                        .changedByEmail(h.getChangedBy() != null ? h.getChangedBy().getEmail() : null)
                        .changedAt(h.getChangedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private Specification<Ticket> buildRoleSecuredSpecification(
            Integer categoryId,
            Integer priorityId,
            Integer statusId,
            Integer technicianId,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            String query
    ) {
        UserDetailsImpl userDetails = getAuthenticatedUserDetails();
        Integer currentUserId = userDetails.getId();
        List<String> roles = userDetails.getRoleNames();

        Integer effectiveRequesterId = null;
        Integer effectiveTechnicianId = null;
        Integer technicianOrRequesterId = null;

        if (roles.contains("ADMIN")) {
            effectiveTechnicianId = technicianId;
        } else if (roles.contains("TECHNICIAN")) {
            if (technicianId != null && !technicianId.equals(currentUserId)) {
                throw new AccessDeniedException("No cuenta con permisos para filtrar o consultar tickets asignados a otros técnicos.");
            }
            technicianOrRequesterId = currentUserId;
        } else {
            // USER solo ve sus propios tickets
            effectiveRequesterId = currentUserId;
        }

        return TicketSpecification.withFilters(
                categoryId,
                priorityId,
                statusId,
                effectiveRequesterId,
                effectiveTechnicianId,
                technicianOrRequesterId,
                dateFrom,
                dateTo,
                query
        );
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
