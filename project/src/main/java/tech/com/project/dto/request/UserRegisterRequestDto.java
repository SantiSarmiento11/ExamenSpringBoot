package tech.com.project.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para registrar un nuevo usuario en TechNova (solo ADMIN)")
public class UserRegisterRequestDto {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    @Schema(description = "Nombre(s) del usuario", example = "Carlos")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
    @Schema(description = "Apellido(s) del usuario", example = "Mendoza")
    private String lastName;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    @Size(max = 150, message = "El correo no puede exceder 150 caracteres")
    @Schema(description = "Correo electrónico institucional único", example = "carlos.mendoza@technova.com")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, max = 50, message = "La contraseña debe tener entre 6 y 50 caracteres")
    @Schema(description = "Contraseña de acceso", example = "segura123")
    private String password;

    @NotBlank(message = "El rol es obligatorio (TECHNICIAN o USER)")
    @Schema(description = "Rol a asignar: TECHNICIAN o USER", example = "TECHNICIAN")
    private String role;
}
