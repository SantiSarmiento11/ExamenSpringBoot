package tech.com.project.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para la creación de un nuevo ticket de soporte")
public class TicketCreateRequestDto {

    @NotBlank(message = "El título del ticket es obligatorio")
    @Size(max = 200, message = "El título no puede exceder los 200 caracteres")
    @Schema(description = "Título descriptivo del incidente", example = "Falla de conectividad en equipo de contabilidad")
    private String title;

    @NotBlank(message = "La descripción del ticket es obligatoria")
    @Schema(description = "Descripción detallada del incidente reportado", example = "El equipo no obtiene dirección IP por cable ni por Wi-Fi.")
    private String description;

    @NotNull(message = "El ID de la categoría es obligatorio")
    @Schema(description = "Identificador de la categoría asignada al ticket", example = "3")
    private Integer categoryId;

    @NotNull(message = "El ID de la prioridad es obligatorio")
    @Schema(description = "Identificador del nivel de prioridad inicial", example = "2")
    private Integer priorityId;
}
