package tech.com.project.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Envoltorio paginado estándar para listados (RT-01)")
public class PagedResponseDto<T> {

    @Schema(description = "Elementos de la página actual")
    private List<T> content;

    @Schema(description = "Número de página actual (iniciando en 0)", example = "0")
    private int page;

    @Schema(description = "Cantidad de elementos por página", example = "10")
    private int size;

    @Schema(description = "Cantidad total de elementos que coinciden con el filtro", example = "42")
    private long totalElements;

    @Schema(description = "Cantidad total de páginas disponibles", example = "5")
    private int totalPages;

    @Schema(description = "Indica si es la primera página", example = "true")
    private boolean first;

    @Schema(description = "Indica si es la última página", example = "false")
    private boolean last;

    public static <T> PagedResponseDto<T> fromPage(Page<T> page) {
        return PagedResponseDto.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
