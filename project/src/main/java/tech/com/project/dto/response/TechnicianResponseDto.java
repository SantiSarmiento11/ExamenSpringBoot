package tech.com.project.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos de un técnico de soporte técnico disponible para asignación")
public class TechnicianResponseDto {

    @Schema(description = "Identificador único del usuario técnico", example = "2")
    private Integer id;

    @Schema(description = "Nombre de pila del técnico", example = "Carlos")
    private String firstName;

    @Schema(description = "Apellido del técnico", example = "Técnico")
    private String lastName;

    @Schema(description = "Correo electrónico institucional", example = "tecnico@technova.com")
    private String email;

    @Schema(description = "Indica si el técnico se encuentra con cuenta activa", example = "true")
    private Boolean isActive;
}
