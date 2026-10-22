package tech.com.project.dto.response;

import lombok.*;

/**
 * DTO con el resumen de métricas CSAT por técnico.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicianCsatDto {

    private Integer technicianId;
    private String technicianName;
    private Double promedio;
    private Long totalCalificaciones;
}
