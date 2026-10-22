package tech.com.project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.com.project.model.TicketAssignment;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketAssignmentRepository extends JpaRepository<TicketAssignment, Integer> {

    List<TicketAssignment> findByTicketIdOrderByAssignedAtDesc(Integer ticketId);

    Optional<TicketAssignment> findFirstByTicketIdAndUnassignedAtIsNullOrderByAssignedAtDesc(Integer ticketId);

    List<TicketAssignment> findByTechnicianIdAndUnassignedAtIsNull(Integer technicianId);

    boolean existsByTicketIdAndTechnicianIdAndUnassignedAtIsNull(Integer ticketId, Integer technicianId);

    @Query("SELECT a FROM TicketAssignment a JOIN FETCH a.technician WHERE a.ticket.id IN :ticketIds AND a.unassignedAt IS NULL")
    List<TicketAssignment> findActiveAssignmentsByTicketIds(@Param("ticketIds") List<Integer> ticketIds);
}
