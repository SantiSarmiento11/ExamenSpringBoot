package tech.com.project.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriorityMetricDto {
    private Integer priorityId;
    private String priorityName;
    private Long count;
    private Double percentage;
}
