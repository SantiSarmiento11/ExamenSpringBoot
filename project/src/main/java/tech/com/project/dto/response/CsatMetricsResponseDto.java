package tech.com.project.dto.response;

import lombok.*;

import java.util.List;

/**
 * DTO con las métricas consolidadas de satisfacción del cliente (CSAT).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsatMetricsResponseDto {

    private Double promedioGeneral;
    private Long totalCalificaciones;
    private List<TechnicianCsatDto> desglosePorTecnico;
}
