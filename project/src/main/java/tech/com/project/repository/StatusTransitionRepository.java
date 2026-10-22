package tech.com.project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.com.project.model.StatusTransition;
import tech.com.project.model.TicketStatus;

import java.util.Optional;

@Repository
public interface StatusTransitionRepository extends JpaRepository<StatusTransition, Integer> {
    boolean existsByFromStatusAndToStatus(TicketStatus fromStatus, TicketStatus toStatus);
    boolean existsByFromStatusAndToStatusAndIsActiveTrue(TicketStatus fromStatus, TicketStatus toStatus);
    Optional<StatusTransition> findByFromStatusAndToStatus(TicketStatus fromStatus, TicketStatus toStatus);
}
