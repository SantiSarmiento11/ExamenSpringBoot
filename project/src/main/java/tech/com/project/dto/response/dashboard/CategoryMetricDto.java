package tech.com.project.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryMetricDto {
    private Integer categoryId;
    private String categoryName;
    private Long count;
    private Double percentage;
}
