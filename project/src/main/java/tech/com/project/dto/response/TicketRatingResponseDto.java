package tech.com.project.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO para la respuesta con datos de la calificación CSAT registrada.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketRatingResponseDto {

    private Integer id;
    private Integer ticketId;
    private Integer puntuacion;
    private String comentario;
    private LocalDateTime fechaCalificacion;
    private Integer requesterId;
    private String requesterName;
    private Integer technicianId;
    private String technicianName;
}
