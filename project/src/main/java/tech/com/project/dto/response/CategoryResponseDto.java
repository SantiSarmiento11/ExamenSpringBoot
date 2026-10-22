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
@Schema(description = "Categoría disponible para clasificación de incidentes")
public class CategoryResponseDto {

    @Schema(description = "Identificador único de la categoría", example = "1")
    private Integer id;

    @Schema(description = "Nombre de la categoría", example = "REDES")
    private String name;

    @Schema(description = "Descripción de la categoría", example = "Problemas de conectividad, Wi-Fi o red")
    private String description;

    @Schema(description = "Indica si la categoría se encuentra activa", example = "true")
    private Boolean isActive;
}
