package tech.com.project.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Estructura estandarizada de errores devueltos por la API REST")
public class ApiErrorResponse {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Marca de tiempo del error", example = "2026-10-09T17:50:00")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Schema(description = "Código de estado HTTP", example = "401")
    private int status;

    @Schema(description = "Razón HTTP o nombre corto del error", example = "Unauthorized")
    private String error;

    @Schema(description = "Mensaje descriptivo del error para el cliente", example = "Credenciales inválidas. Verifique su correo o contraseña.")
    private String message;

    @Schema(description = "Ruta o endpoint donde se originó el error", example = "/api/auth/login")
    private String path;

    @Schema(description = "Lista detallada de validaciones fallidas si aplica")
    private List<String> validationErrors;
}
