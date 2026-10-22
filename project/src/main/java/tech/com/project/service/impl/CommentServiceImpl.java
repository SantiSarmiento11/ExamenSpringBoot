package tech.com.project.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.com.project.dto.request.CommentCreateRequestDto;
import tech.com.project.dto.response.CommentResponseDto;
import tech.com.project.exception.ResourceNotFoundException;
import tech.com.project.model.Comment;
import tech.com.project.model.Ticket;
import tech.com.project.model.UserAccount;
import tech.com.project.repository.CommentRepository;
import tech.com.project.repository.TicketAssignmentRepository;
import tech.com.project.repository.TicketRepository;
import tech.com.project.repository.UserAccountRepository;
import tech.com.project.security.services.UserDetailsImpl;
import tech.com.project.service.CommentService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserAccountRepository userAccountRepository;
    private final TicketAssignmentRepository ticketAssignmentRepository;

    @Override
    @Transactional
    public CommentResponseDto addComment(Integer ticketId, CommentCreateRequestDto request) {
        UserDetailsImpl userDetails = getAuthenticatedUserDetails();
        Integer currentUserId = userDetails.getId();
        List<String> roles = userDetails.getRoleNames();

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket con ID " + ticketId + " no encontrado."));

        UserAccount author = userAccountRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con ID " + currentUserId + " no encontrado."));

        // Verificación de autorización para agregar comentarios (RF-05)
        validateUserCanComment(ticket, currentUserId, roles);

        Comment comment = Comment.builder()
                .ticket(ticket)
                .author(author)
                .content(request.getContent().trim())
                .createdAt(LocalDateTime.now())
                .build();

        Comment savedComment = commentRepository.save(comment);
        log.info("Comentario registrado con ID {} en ticket {} por usuario {}", savedComment.getId(), ticket.getCode(), author.getEmail());

        return mapToResponseDto(savedComment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsByTicketId(Integer ticketId) {
        UserDetailsImpl userDetails = getAuthenticatedUserDetails();
        Integer currentUserId = userDetails.getId();
        List<String> roles = userDetails.getRoleNames();

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket con ID " + ticketId + " no encontrado."));

        // Verificación de acceso para consultar comentarios del ticket
        validateUserCanAccessTicket(ticket, currentUserId, roles);

        List<Comment> comments = commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
        return comments.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private void validateUserCanComment(Ticket ticket, Integer currentUserId, List<String> roles) {
        if (ticket.getStatus() != null && "CERRADA".equalsIgnoreCase(ticket.getStatus().getName())) {
            throw new IllegalArgumentException("No es posible agregar nuevos comentarios en un ticket que se encuentra en estado CERRADA.");
        }

        if (roles.contains("ADMIN")) {
            return; // ADMIN puede comentar siempre
        }

        boolean isRequester = ticket.getRequester() != null && ticket.getRequester().getId().equals(currentUserId);

        if (roles.contains("TECHNICIAN")) {
            boolean isAssigned = ticketAssignmentRepository.existsByTicketIdAndTechnicianIdAndUnassignedAtIsNull(ticket.getId(), currentUserId);
            if (!isAssigned && !isRequester) {
                throw new AccessDeniedException("No cuenta con permisos para agregar comentarios en un ticket que no está asignado a usted ni es su solicitante.");
            }
        } else {
            // Rol USER
            if (!isRequester) {
                throw new AccessDeniedException("No cuenta con permisos para comentar en tickets creados por otros usuarios.");
            }
        }
    }

    private void validateUserCanAccessTicket(Ticket ticket, Integer currentUserId, List<String> roles) {
        if (roles.contains("ADMIN")) {
            return;
        }

        boolean isRequester = ticket.getRequester() != null && ticket.getRequester().getId().equals(currentUserId);

        if (roles.contains("TECHNICIAN")) {
            boolean isAssigned = ticketAssignmentRepository.existsByTicketIdAndTechnicianIdAndUnassignedAtIsNull(ticket.getId(), currentUserId);
            if (!isAssigned && !isRequester) {
                throw new AccessDeniedException("No cuenta con permisos para ver los comentarios de este ticket.");
            }
        } else {
            if (!isRequester) {
                throw new AccessDeniedException("No cuenta con permisos para consultar comentarios de un ticket ajeno.");
            }
        }
    }

    private CommentResponseDto mapToResponseDto(Comment comment) {
        UserAccount author = comment.getAuthor();
        String authorRoles = author.getUserRoles() != null
                ? author.getUserRoles().stream()
                        .map(ur -> ur.getRole() != null ? ur.getRole().getName() : "")
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.joining(", "))
                : "";

        return CommentResponseDto.builder()
                .id(comment.getId())
                .ticketId(comment.getTicket().getId())
                .ticketCode(comment.getTicket().getCode())
                .authorId(author.getId())
                .authorName(author.getFirstName() + " " + author.getLastName())
                .authorEmail(author.getEmail())
                .authorRoles(authorRoles)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    private UserDetailsImpl getAuthenticatedUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserDetailsImpl)) {
            throw new AccessDeniedException("Sesión no válida o usuario no autenticado.");
        }
        return (UserDetailsImpl) auth.getPrincipal();
    }
}
