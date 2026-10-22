package tech.com.project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.com.project.model.TicketHistory;

import java.util.List;

@Repository
public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Integer> {

    @Query("SELECT h FROM TicketHistory h LEFT JOIN FETCH h.previousStatus LEFT JOIN FETCH h.newStatus LEFT JOIN FETCH h.changedBy WHERE h.ticket.id = :ticketId ORDER BY h.changedAt ASC")
    List<TicketHistory> findByTicketIdOrderByChangedAtAsc(@Param("ticketId") Integer ticketId);

    List<TicketHistory> findByTicketIdOrderByChangedAtDesc(Integer ticketId);
}
