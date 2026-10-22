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
@Schema(description = "Nivel de prioridad de un ticket con acuerdo de nivel de servicio (SLA)")
public class PriorityResponseDto {

    @Schema(description = "Identificador único de la prioridad", example = "2")
    private Integer id;

    @Schema(description = "Nombre de la prioridad", example = "MEDIA")
    private String name;

    @Schema(description = "Descripción y tiempo máximo de respuesta SLA", example = "Incidencias estándar que afectan parcialmente operaciones (SLA: 8 horas)")
    private String description;

    @Schema(description = "Nivel numérico de severidad", example = "2")
    private Integer level;
}
