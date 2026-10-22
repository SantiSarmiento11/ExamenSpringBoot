package tech.com.project.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicianWorkloadDto {
    private Integer technicianId;
    private String technicianName;
    private String email;
    private Long activeTickets;
    private Long resolvedTickets;
}
