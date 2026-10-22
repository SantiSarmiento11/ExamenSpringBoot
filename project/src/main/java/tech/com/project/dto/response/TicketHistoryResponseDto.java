package tech.com.project.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO de respuesta con el registro histórico de un cambio de estado en un ticket")
public class TicketHistoryResponseDto {

    @Schema(description = "ID del registro de historial", example = "1")
    private Integer id;

    @Schema(description = "ID del ticket", example = "10")
    private Integer ticketId;

    @Schema(description = "Código del ticket", example = "TK-0001")
    private String ticketCode;

    @Schema(description = "ID del estado previo (null si es creación)", example = "1")
    private Integer previousStatusId;

    @Schema(description = "Nombre del estado previo", example = "ABIERTA")
    private String previousStatusName;

    @Schema(description = "ID del nuevo estado", example = "2")
    private Integer newStatusId;

    @Schema(description = "Nombre del nuevo estado", example = "ASIGNADA")
    private String newStatusName;

    @Schema(description = "ID del usuario que ejecutó la transición", example = "1")
    private Integer changedById;

    @Schema(description = "Nombre completo del usuario que ejecutó el cambio", example = "Admin Principal")
    private String changedByName;

    @Schema(description = "Correo del usuario que ejecutó el cambio", example = "admin@technova.com")
    private String changedByEmail;

    @Schema(description = "Fecha y hora exacta del cambio")
    private LocalDateTime changedAt;
}
