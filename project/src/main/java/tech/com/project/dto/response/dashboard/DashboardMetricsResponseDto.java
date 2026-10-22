package tech.com.project.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardMetricsResponseDto {

    private Long totalTickets;
    private Long openTickets;
    private Long assignedTickets;
    private Long inProgressTickets;
    private Long resolvedTickets;
    private Long closedTickets;

    // SLA
    private Long slaCompliantCount;
    private Long slaBreachedCount;
    private Double slaCompliancePercentage;

    // MTTR (Mean Time to Resolution)
    private Double averageResolutionTimeHours;

    // Distributions
    private List<CategoryMetricDto> categoryDistribution;
    private List<PriorityMetricDto> priorityDistribution;
    private List<StatusMetricDto> statusDistribution;
    private List<TechnicianWorkloadDto> technicianWorkloads;
}
