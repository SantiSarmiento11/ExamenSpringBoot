package tech.com.project.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.com.project.model.Ticket;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Integer>, JpaSpecificationExecutor<Ticket> {

    Optional<Ticket> findByCode(String code);

    boolean existsByCode(String code);

    Optional<Ticket> findTopByOrderByIdDesc();

    @Override
    @EntityGraph(attributePaths = {"category", "priority", "status", "requester"})
    Optional<Ticket> findById(Integer id);

    @Override
    @EntityGraph(attributePaths = {"category", "priority", "status", "requester"})
    List<Ticket> findAll(Specification<Ticket> spec, Sort sort);

    @Override
    @EntityGraph(attributePaths = {"category", "priority", "status", "requester"})
    Page<Ticket> findAll(Specification<Ticket> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "priority", "status", "requester"})
    List<Ticket> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"category", "priority", "status", "requester"})
    List<Ticket> findByRequesterIdOrderByCreatedAtDesc(Integer requesterId);

    @EntityGraph(attributePaths = {"category", "priority", "status", "requester"})
    @Query("SELECT DISTINCT t FROM Ticket t JOIN t.assignments a WHERE a.technician.id = :technicianId AND a.unassignedAt IS NULL ORDER BY t.createdAt DESC")
    List<Ticket> findActiveByTechnicianIdOrderByCreatedAtDesc(@Param("technicianId") Integer technicianId);

    @EntityGraph(attributePaths = {"category", "priority", "status", "requester"})
    @Query("SELECT DISTINCT t FROM Ticket t LEFT JOIN t.assignments a WHERE (a.technician.id = :userId AND a.unassignedAt IS NULL) OR t.requester.id = :userId ORDER BY t.createdAt DESC")
    List<Ticket> findActiveByTechnicianIdOrRequesterId(@Param("userId") Integer userId);

    @Query("SELECT DISTINCT t FROM Ticket t JOIN t.assignments a WHERE a.technician.id = :technicianId ORDER BY t.createdAt DESC")
    List<Ticket> findAllByTechnicianIdOrderByCreatedAtDesc(@Param("technicianId") Integer technicianId);

    @Query("SELECT t.status.id, t.status.name, COUNT(t) FROM Ticket t GROUP BY t.status.id, t.status.name")
    List<Object[]> countTicketsGroupedByStatus();

    @Query("SELECT t.category.id, t.category.name, COUNT(t) FROM Ticket t GROUP BY t.category.id, t.category.name")
    List<Object[]> countTicketsGroupedByCategory();

    @Query("SELECT t.priority.id, t.priority.name, COUNT(t) FROM Ticket t GROUP BY t.priority.id, t.priority.name")
    List<Object[]> countTicketsGroupedByPriority();

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status.name IN ('RESUELTA', 'CERRADA') AND t.updatedAt <= t.dueDate")
    Long countResolvedWithinSla();

    @Query("SELECT COUNT(t) FROM Ticket t WHERE (t.status.name IN ('RESUELTA', 'CERRADA') AND t.updatedAt > t.dueDate) OR (t.status.name NOT IN ('RESUELTA', 'CERRADA') AND t.dueDate < CURRENT_TIMESTAMP)")
    Long countBreachedSla();

    @Query("SELECT t FROM Ticket t WHERE t.status.name IN ('RESUELTA', 'CERRADA')")
    List<Ticket> findResolvedOrClosedTickets();

    @Query("SELECT a.technician.id, " +
           "SUM(CASE WHEN t.status.name NOT IN ('RESUELTA', 'CERRADA') AND a.unassignedAt IS NULL THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN t.status.name IN ('RESUELTA', 'CERRADA') THEN 1 ELSE 0 END) " +
           "FROM TicketAssignment a JOIN a.ticket t " +
           "GROUP BY a.technician.id")
    List<Object[]> countTechnicianWorkloads();
}
