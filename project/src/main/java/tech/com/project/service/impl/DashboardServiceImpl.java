package tech.com.project.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.com.project.dto.response.dashboard.*;
import tech.com.project.model.Ticket;
import tech.com.project.model.UserAccount;
import tech.com.project.repository.TicketAssignmentRepository;
import tech.com.project.repository.TicketRepository;
import tech.com.project.repository.UserAccountRepository;
import tech.com.project.service.DashboardService;

import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final TicketRepository ticketRepository;
    private final UserAccountRepository userAccountRepository;
    private final TicketAssignmentRepository ticketAssignmentRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardMetricsResponseDto getDashboardMetrics() {
        long totalTickets = ticketRepository.count();

        // 1. Distribución de Estados
        Map<String, Long> statusMap = new HashMap<>();
        List<StatusMetricDto> statusDistribution = new ArrayList<>();
        List<Object[]> statusCounts = ticketRepository.countTicketsGroupedByStatus();

        for (Object[] row : statusCounts) {
            Integer statusId = (Integer) row[0];
            String name = (String) row[1];
            Long count = ((Number) row[2]).longValue();
            statusMap.put(name, count);

            double percentage = totalTickets > 0 ? Math.round((count * 100.0 / totalTickets) * 10.0) / 10.0 : 0.0;
            statusDistribution.add(StatusMetricDto.builder()
                    .statusId(statusId)
                    .statusName(name)
                    .count(count)
                    .percentage(percentage)
                    .build());
        }

        long openTickets = statusMap.getOrDefault("ABIERTA", 0L);
        long assignedTickets = statusMap.getOrDefault("ASIGNADA", 0L);
        long inProgressTickets = statusMap.getOrDefault("EN_PROCESO", 0L);
        long resolvedTickets = statusMap.getOrDefault("RESUELTA", 0L);
        long closedTickets = statusMap.getOrDefault("CERRADA", 0L);

        // 2. Distribución de Categorías
        List<CategoryMetricDto> categoryDistribution = new ArrayList<>();
        List<Object[]> categoryCounts = ticketRepository.countTicketsGroupedByCategory();
        for (Object[] row : categoryCounts) {
            Integer categoryId = (Integer) row[0];
            String categoryName = (String) row[1];
            Long count = ((Number) row[2]).longValue();
            double percentage = totalTickets > 0 ? Math.round((count * 100.0 / totalTickets) * 10.0) / 10.0 : 0.0;

            categoryDistribution.add(CategoryMetricDto.builder()
                    .categoryId(categoryId)
                    .categoryName(categoryName)
                    .count(count)
                    .percentage(percentage)
                    .build());
        }

        // 3. Distribución de Prioridades
        List<PriorityMetricDto> priorityDistribution = new ArrayList<>();
        List<Object[]> priorityCounts = ticketRepository.countTicketsGroupedByPriority();
        for (Object[] row : priorityCounts) {
            Integer priorityId = (Integer) row[0];
            String priorityName = (String) row[1];
            Long count = ((Number) row[2]).longValue();
            double percentage = totalTickets > 0 ? Math.round((count * 100.0 / totalTickets) * 10.0) / 10.0 : 0.0;

            priorityDistribution.add(PriorityMetricDto.builder()
                    .priorityId(priorityId)
                    .priorityName(priorityName)
                    .count(count)
                    .percentage(percentage)
                    .build());
        }

        // 4. SLA y Tiempos de Resolución (MTTR)
        Long compliantCount = ticketRepository.countResolvedWithinSla();
        Long breachedCount = ticketRepository.countBreachedSla();
        long evaluatedSlaTotal = (compliantCount != null ? compliantCount : 0L) + (breachedCount != null ? breachedCount : 0L);
        double slaPercentage = evaluatedSlaTotal > 0
                ? Math.round(((compliantCount != null ? compliantCount : 0L) * 100.0 / evaluatedSlaTotal) * 10.0) / 10.0
                : 100.0;

        List<Ticket> resolvedOrClosed = ticketRepository.findResolvedOrClosedTickets();
        double avgResolutionHours = 0.0;
        if (!resolvedOrClosed.isEmpty()) {
            long totalMinutes = 0;
            int countWithTime = 0;
            for (Ticket t : resolvedOrClosed) {
                if (t.getCreatedAt() != null && t.getUpdatedAt() != null) {
                    long minutes = Duration.between(t.getCreatedAt(), t.getUpdatedAt()).toMinutes();
                    totalMinutes += Math.max(0, minutes);
                    countWithTime++;
                }
            }
            if (countWithTime > 0) {
                avgResolutionHours = Math.round((((double) totalMinutes / countWithTime) / 60.0) * 10.0) / 10.0;
            }
        }

        // 5. Carga de Trabajo de Técnicos (Technician Workload - Consulta Agregada Directa sin N+1)
        Map<Integer, long[]> workloadMap = new HashMap<>();
        List<Object[]> workloadCounts = ticketRepository.countTechnicianWorkloads();
        for (Object[] row : workloadCounts) {
            Integer techId = (Integer) row[0];
            long active = ((Number) row[1]).longValue();
            long resolved = ((Number) row[2]).longValue();
            workloadMap.put(techId, new long[]{active, resolved});
        }

        List<TechnicianWorkloadDto> technicianWorkloads = new ArrayList<>();
        List<UserAccount> technicians = userAccountRepository.findActiveUsersByRoleName("TECHNICIAN");

        for (UserAccount tech : technicians) {
            long[] counts = workloadMap.getOrDefault(tech.getId(), new long[]{0L, 0L});

            technicianWorkloads.add(TechnicianWorkloadDto.builder()
                    .technicianId(tech.getId())
                    .technicianName(tech.getFirstName() + " " + tech.getLastName())
                    .email(tech.getEmail())
                    .activeTickets(counts[0])
                    .resolvedTickets(counts[1])
                    .build());
        }

        return DashboardMetricsResponseDto.builder()
                .totalTickets(totalTickets)
                .openTickets(openTickets)
                .assignedTickets(assignedTickets)
                .inProgressTickets(inProgressTickets)
                .resolvedTickets(resolvedTickets)
                .closedTickets(closedTickets)
                .slaCompliantCount(compliantCount != null ? compliantCount : 0L)
                .slaBreachedCount(breachedCount != null ? breachedCount : 0L)
                .slaCompliancePercentage(slaPercentage)
                .averageResolutionTimeHours(avgResolutionHours)
                .categoryDistribution(categoryDistribution)
                .priorityDistribution(priorityDistribution)
                .statusDistribution(statusDistribution)
                .technicianWorkloads(technicianWorkloads)
                .build();
    }
}
