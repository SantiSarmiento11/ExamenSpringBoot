package tech.com.project.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO de solicitud para registrar un nuevo comentario en un ticket")
public class CommentCreateRequestDto {

    @NotBlank(message = "El contenido del comentario es obligatorio")
    @Size(min = 1, max = 2000, message = "El contenido del comentario debe tener entre 1 y 2000 caracteres")
    @Schema(description = "Texto del comentario", example = "Se procedió a reinstalar el controlador de red y se restableció la conexión.")
    private String content;
}
