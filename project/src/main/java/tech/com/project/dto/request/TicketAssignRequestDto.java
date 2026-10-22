package tech.com.project.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para la asignación manual de un técnico a un ticket")
public class TicketAssignRequestDto {

    @NotNull(message = "El ID del técnico es obligatorio")
    @Schema(description = "Identificador del usuario con rol TECHNICIAN a asignar", example = "2")
    private Integer technicianId;
}
