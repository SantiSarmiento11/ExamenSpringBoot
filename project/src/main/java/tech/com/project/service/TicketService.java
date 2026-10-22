package tech.com.project.service;

import org.springframework.data.domain.Pageable;
import tech.com.project.dto.request.TicketAssignRequestDto;
import tech.com.project.dto.request.TicketCreateRequestDto;
import tech.com.project.dto.request.TicketStatusUpdateRequestDto;
import tech.com.project.dto.response.PagedResponseDto;
import tech.com.project.dto.response.TicketHistoryResponseDto;
import tech.com.project.dto.response.TicketResponseDto;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketService {

    /**
     * Crea un nuevo ticket de soporte asociado al usuario autenticado (RF-02).
     * Genera automáticamente el correlativo secuencial TK-XXXX, asigna el estado ABIERTA
     * y calcula la fecha límite (dueDate) en base al SLA de la prioridad (RN-03).
     *
     * @param request Datos de la solicitud de ticket.
     * @return DTO detallado del ticket recién creado.
     */
    TicketResponseDto createTicket(TicketCreateRequestDto request);

    /**
     * Lista los tickets aplicando el filtro de seguridad según el rol del usuario (RF-01, RF-02):
     * - USER: Únicamente sus propios tickets.
     * - TECHNICIAN: Solo tickets asignados a su cuenta.
     * - ADMIN: Todos los tickets registrados en la plataforma.
     *
     * @return Lista de DTOs de tickets correspondientes.
     */
    List<TicketResponseDto> getTickets();

    /**
     * Consulta y filtra tickets según criterios múltiples y rol del usuario (RF-08).
     */
    List<TicketResponseDto> searchTickets(
            Integer categoryId,
            Integer priorityId,
            Integer statusId,
            Integer technicianId,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            String query
    );

    /**
     * Consulta paginada y filtrada de tickets según criterios múltiples y rol (RF-08, RT-01).
     */
    PagedResponseDto<TicketResponseDto> searchTicketsPaged(
            Integer categoryId,
            Integer priorityId,
            Integer statusId,
            Integer technicianId,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            String query,
            Pageable pageable
    );

    /**
     * Consulta el detalle de un ticket específico validando la autorización de acceso.
     *
     * @param id Identificador numérico del ticket.
     * @return DTO del ticket consultado.
     */
    TicketResponseDto getTicketById(Integer id);

    /**
     * Asignación manual de un técnico a un ticket, acción exclusiva de ADMIN (RF-03, CA-001).
     * Cambia automáticamente el estado a ASIGNADA y registra el evento en el historial.
     *
     * @param id      Identificador del ticket.
     * @param request DTO con el ID del técnico asignado.
     * @return DTO del ticket con la nueva asignación y estado ASIGNADA.
     */
    TicketResponseDto assignTicket(Integer id, TicketAssignRequestDto request);

    /**
     * Realiza una transición de estado en el ciclo de vida del ticket (RF-04, RN-02).
     * Valida estrictamente la máquina de estados y las autorizaciones del actor.
     *
     * @param id      Identificador del ticket.
     * @param request DTO con el estado destino.
     * @return DTO del ticket con el nuevo estado aplicado.
     */
    TicketResponseDto updateTicketStatus(Integer id, TicketStatusUpdateRequestDto request);

    /**
     * Consulta la trazabilidad completa e histórico de cambios de estado del ticket (RF-05).
     *
     * @param id Identificador numérico del ticket.
     * @return Lista de registros históricos ordenados cronológicamente.
     */
    List<TicketHistoryResponseDto> getTicketHistory(Integer id);
}
