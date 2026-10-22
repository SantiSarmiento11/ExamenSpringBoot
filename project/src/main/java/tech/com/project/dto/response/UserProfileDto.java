package tech.com.project.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Perfil del usuario autenticado actualmente en la sesión")
public class UserProfileDto {

    @Schema(description = "Identificador único del usuario", example = "1")
    private Integer id;

    @Schema(description = "Nombre de pila del usuario", example = "Admin")
    private String firstName;

    @Schema(description = "Apellido del usuario", example = "TechNova")
    private String lastName;

    @Schema(description = "Correo electrónico del usuario", example = "admin@technova.com")
    private String email;

    @Schema(description = "Estado de actividad de la cuenta", example = "true")
    private Boolean isActive;

    @Schema(description = "Roles asignados", example = "[\"ADMIN\"]")
    private List<String> roles;

    @Schema(description = "Permisos asociados", example = "[\"TICKET_VIEW_ALL\", \"TICKET_ASSIGN\"]")
    private List<String> permissions;
}
