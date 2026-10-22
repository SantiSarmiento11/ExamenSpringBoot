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
@Schema(description = "Representación detallada de un ticket de soporte de TechNova")
public class TicketResponseDto {

    @Schema(description = "Identificador único de la incidencia", example = "1")
    private Integer id;

    @Schema(description = "Código correlativo del ticket", example = "TK-0001")
    private String code;

    @Schema(description = "Título o asunto de la incidencia", example = "Falla de conectividad en equipo de contabilidad")
    private String title;

    @Schema(description = "Descripción detallada del incidente", example = "El equipo no obtiene dirección IP por cable ni por Wi-Fi.")
    private String description;

    @Schema(description = "ID de la categoría", example = "3")
    private Integer categoryId;

    @Schema(description = "Nombre de la categoría", example = "REDES")
    private String categoryName;

    @Schema(description = "ID de la prioridad", example = "2")
    private Integer priorityId;

    @Schema(description = "Nombre de la prioridad", example = "MEDIA")
    private String priorityName;

    @Schema(description = "ID del estado actual", example = "1")
    private Integer statusId;

    @Schema(description = "Nombre del estado actual", example = "ABIERTA")
    private String statusName;

    @Schema(description = "ID del usuario solicitante", example = "3")
    private Integer requesterId;

    @Schema(description = "Nombre completo del usuario solicitante", example = "Santiago Usuario")
    private String requesterName;

    @Schema(description = "Correo electrónico del solicitante", example = "usuario@technova.com")
    private String requesterEmail;

    @Schema(description = "ID del técnico asignado actualmente (o null si está sin asignar)", example = "2")
    private Integer assignedTechnicianId;

    @Schema(description = "Nombre completo del técnico asignado", example = "Carlos Técnico")
    private String assignedTechnicianName;

    @Schema(description = "Correo del técnico asignado", example = "tecnico@technova.com")
    private String assignedTechnicianEmail;

    @Schema(description = "Fecha y hora límite de resolución según el SLA de la prioridad", example = "2026-10-11T20:00:00")
    private LocalDateTime dueDate;

    @Schema(description = "Fecha y hora de creación de la incidencia", example = "2026-10-11T12:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Fecha y hora de última actualización", example = "2026-10-11T12:00:00")
    private LocalDateTime updatedAt;
}
