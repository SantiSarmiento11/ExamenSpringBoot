package tech.com.project.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para la transición de estado de un ticket")
public class TicketStatusUpdateRequestDto {

    @NotBlank(message = "El nombre del estado destino es obligatorio")
    @Schema(description = "Nombre del estado destino (p. ej. ASIGNADA, EN_PROCESO, RESUELTA, CERRADA)", example = "EN_PROCESO")
    private String status;
}
