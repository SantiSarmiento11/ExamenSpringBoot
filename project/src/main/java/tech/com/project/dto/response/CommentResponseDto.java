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
@Schema(description = "DTO de respuesta con el detalle de un comentario registrado")
public class CommentResponseDto {

    @Schema(description = "ID único del comentario", example = "1")
    private Integer id;

    @Schema(description = "ID del ticket asociado", example = "10")
    private Integer ticketId;

    @Schema(description = "Código del ticket", example = "TK-0001")
    private String ticketCode;

    @Schema(description = "ID del usuario autor del comentario", example = "2")
    private Integer authorId;

    @Schema(description = "Nombre completo del autor", example = "Juan Soporte")
    private String authorName;

    @Schema(description = "Correo del autor", example = "tecnico@technova.com")
    private String authorEmail;

    @Schema(description = "Roles del autor", example = "ROLE_TECHNICIAN")
    private String authorRoles;

    @Schema(description = "Contenido del comentario", example = "Se procedió a reinstalar el controlador de red.")
    private String content;

    @Schema(description = "Fecha y hora de creación del comentario")
    private LocalDateTime createdAt;
}
