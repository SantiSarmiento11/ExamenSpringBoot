package tech.com.project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tech.com.project.model.TicketRating;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para operaciones con calificaciones de satisfacción (CSAT).
 */
@Repository
public interface TicketRatingRepository extends JpaRepository<TicketRating, Integer> {

    Optional<TicketRating> findByTicketId(Integer ticketId);

    boolean existsByTicketId(Integer ticketId);

    @Query("SELECT AVG(r.puntuacion) FROM TicketRating r")
    Double getAverageRating();

    @Query("SELECT COUNT(r) FROM TicketRating r")
    Long countTotalRatings();

    @Query("SELECT r.technician.id, r.technician.firstName, r.technician.lastName, AVG(r.puntuacion), COUNT(r) FROM TicketRating r WHERE r.technician IS NOT NULL GROUP BY r.technician.id, r.technician.firstName, r.technician.lastName")
    List<Object[]> getAverageRatingPerTechnician();
}
