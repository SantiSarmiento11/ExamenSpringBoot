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
@Schema(description = "Respuesta de autenticación exitosa con Token JWT y detalles del usuario")
public class LoginResponseDto {

    @Schema(description = "Token de autenticación JWT firmado", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "Tipo de token para el encabezado Authorization", example = "Bearer")
    @Builder.Default
    private String type = "Bearer";

    @Schema(description = "Identificador único del usuario", example = "1")
    private Integer id;

    @Schema(description = "Correo electrónico del usuario autenticado", example = "admin@technova.com")
    private String email;

    @Schema(description = "Nombre de pila del usuario", example = "Admin")
    private String firstName;

    @Schema(description = "Apellido del usuario", example = "TechNova")
    private String lastName;

    @Schema(description = "Roles asignados al usuario", example = "[\"ADMIN\"]")
    private List<String> roles;

    @Schema(description = "Permisos específicos otorgados al usuario", example = "[\"TICKET_VIEW_ALL\", \"TICKET_ASSIGN\"]")
    private List<String> permissions;

    @Schema(description = "Tiempo de expiración del token en milisegundos a partir de la emisión", example = "86400000")
    private Long expiresIn;
}
