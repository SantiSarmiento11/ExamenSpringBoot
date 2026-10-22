package tech.com.project.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.com.project.model.Comment;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {

    @Query("SELECT c FROM Comment c JOIN FETCH c.author a LEFT JOIN FETCH a.userRoles ur LEFT JOIN FETCH ur.role WHERE c.ticket.id = :ticketId ORDER BY c.createdAt ASC")
    List<Comment> findByTicketIdOrderByCreatedAtAsc(@Param("ticketId") Integer ticketId);

    Page<Comment> findByTicketIdOrderByCreatedAtAsc(Integer ticketId, Pageable pageable);
}
