package tech.com.project.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import tech.com.project.dto.request.TicketAssignRequestDto;
import tech.com.project.dto.request.TicketCreateRequestDto;
import tech.com.project.dto.request.TicketStatusUpdateRequestDto;
import tech.com.project.dto.response.ApiErrorResponse;
import tech.com.project.dto.response.PagedResponseDto;
import tech.com.project.dto.response.TicketHistoryResponseDto;
import tech.com.project.dto.response.TicketResponseDto;
import tech.com.project.service.TicketService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Tag(name = "2. Gestión Core de Tickets (RF-02, RF-03, RF-04)", description = "Operaciones de creación, listado, asignación manual y transiciones de estado de tickets")
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @Operation(
            summary = "Crear nuevo ticket de soporte (RF-02)",
            description = "Permite a un usuario autenticado registrar un ticket de soporte. Genera automáticamente un código correlativo (ej. TK-0001), estado inicial ABIERTA y fecha límite SLA calculada (RN-03).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Ticket creado exitosamente.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TicketResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos en el cuerpo de la petición.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado. Token requerido.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Categoría o prioridad no encontrada.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public ResponseEntity<TicketResponseDto> createTicket(@Valid @RequestBody TicketCreateRequestDto request) {
        TicketResponseDto response = ticketService.createTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
            summary = "Listar tickets según rol del usuario",
            description = "Consulta la lista de tickets aplicando filtro de seguridad estricto por rol: USER consulta únicamente sus propias solicitudes, TECHNICIAN sus tickets asignados y ADMIN la totalidad de las incidencias.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de tickets obtenido correctamente.",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TicketResponseDto.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado. Token requerido.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public ResponseEntity<List<TicketResponseDto>> getTickets(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer priorityId,
            @RequestParam(required = false) Integer statusId,
            @RequestParam(required = false) Integer technicianId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam(required = false) String query
    ) {
        if (categoryId != null || priorityId != null || statusId != null || technicianId != null
                || dateFrom != null || dateTo != null || (query != null && !query.trim().isEmpty())) {
            return ResponseEntity.ok(ticketService.searchTickets(categoryId, priorityId, statusId, technicianId, dateFrom, dateTo, query));
        }
        return ResponseEntity.ok(ticketService.getTickets());
    }

    @GetMapping("/paged")
    @Operation(
            summary = "Listar tickets de forma paginada con filtros multicriterio",
            description = "Retorna una página de tickets con metadata de paginación (totalElements, totalPages, page, size) aplicando filtros multicriterio y restricciones por rol (RT-01, RF-08).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Página de tickets obtenida exitosamente.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PagedResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado. Token requerido.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public ResponseEntity<PagedResponseDto<TicketResponseDto>> getTicketsPaged(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer priorityId,
            @RequestParam(required = false) Integer statusId,
            @RequestParam(required = false) Integer technicianId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam(required = false) String query,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ticketService.searchTicketsPaged(categoryId, priorityId, statusId, technicianId, dateFrom, dateTo, query, pageable));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Consultar detalle de un ticket",
            description = "Retorna el detalle completo de una incidencia validando los permisos de acceso del usuario autenticado.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Detalle del ticket obtenido exitosamente.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TicketResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado. Token requerido.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado. No tiene permisos para consultar este ticket.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ticket no encontrado.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public ResponseEntity<TicketResponseDto> getTicketById(@PathVariable Integer id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Asignación manual de técnico a un ticket (RF-03, CA-001)",
            description = "Operación exclusiva del rol ADMIN. Asigna un técnico de soporte a la incidencia, actualiza automáticamente el estado a ASIGNADA y registra el evento en el historial de auditoría.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Ticket asignado exitosamente.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TicketResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos o el usuario especificado no es un técnico válido.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado. Token requerido.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado. Solo un usuario con rol ADMIN puede asignar técnicos.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ticket o técnico no encontrado.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public ResponseEntity<TicketResponseDto> assignTicket(
            @PathVariable Integer id,
            @Valid @RequestBody TicketAssignRequestDto request
    ) {
        return ResponseEntity.ok(ticketService.assignTicket(id, request));
    }

    @PutMapping("/{id}/status")
    @Operation(
            summary = "Transición formal de estado de un ticket (RF-04, RN-02)",
            description = "Avanza el estado del ticket validando la máquina de estados estricta y los roles requeridos para cada transición (ABIERTA -> ASIGNADA -> EN_PROCESO -> RESUELTA -> CERRADA).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Estado actualizado exitosamente.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TicketResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Transición inválida o salto de estado no permitido según el ciclo de vida.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado. Token requerido.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado. El usuario no cuenta con el rol o relación requerida para esta transición.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ticket o estado destino no encontrado.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public ResponseEntity<TicketResponseDto> updateTicketStatus(
            @PathVariable Integer id,
            @Valid @RequestBody TicketStatusUpdateRequestDto request
    ) {
        return ResponseEntity.ok(ticketService.updateTicketStatus(id, request));
    }

    @GetMapping("/{id}/history")
    @Operation(
            summary = "Consultar trazabilidad e histórico de cambios de estado del ticket",
            description = "Retorna la bitácora cronológica completa de cambios de estado, fecha y usuario que realizó cada transición (RF-05).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Historial obtenido correctamente.",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TicketHistoryResponseDto.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado. Token requerido.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado. No posee permisos sobre este ticket.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ticket no encontrado.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public ResponseEntity<List<TicketHistoryResponseDto>> getTicketHistory(@PathVariable Integer id) {
        return ResponseEntity.ok(ticketService.getTicketHistory(id));
    }
}
