package tech.com.project.repository.specification;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import tech.com.project.model.Ticket;
import tech.com.project.model.TicketAssignment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TicketSpecification {

    public static Specification<Ticket> withFilters(
            Integer categoryId,
            Integer priorityId,
            Integer statusId,
            Integer requesterId,
            Integer technicianId,
            Integer technicianOrRequesterId,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            String searchQuery
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            if (priorityId != null) {
                predicates.add(cb.equal(root.get("priority").get("id"), priorityId));
            }

            if (statusId != null) {
                predicates.add(cb.equal(root.get("status").get("id"), statusId));
            }

            if (requesterId != null) {
                predicates.add(cb.equal(root.get("requester").get("id"), requesterId));
            }

            if (technicianId != null) {
                Subquery<Integer> subquery = query.subquery(Integer.class);
                Root<TicketAssignment> assignmentRoot = subquery.from(TicketAssignment.class);
                subquery.select(assignmentRoot.get("ticket").get("id"))
                        .where(
                                cb.equal(assignmentRoot.get("technician").get("id"), technicianId),
                                cb.isNull(assignmentRoot.get("unassignedAt"))
                        );
                predicates.add(root.get("id").in(subquery));
            }

            if (technicianOrRequesterId != null) {
                Subquery<Integer> subquery = query.subquery(Integer.class);
                Root<TicketAssignment> assignmentRoot = subquery.from(TicketAssignment.class);
                subquery.select(assignmentRoot.get("ticket").get("id"))
                        .where(
                                cb.equal(assignmentRoot.get("technician").get("id"), technicianOrRequesterId),
                                cb.isNull(assignmentRoot.get("unassignedAt"))
                        );
                Predicate isAssigned = root.get("id").in(subquery);
                Predicate isRequester = cb.equal(root.get("requester").get("id"), technicianOrRequesterId);
                predicates.add(cb.or(isAssigned, isRequester));
            }

            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
            }

            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo));
            }

            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                String pattern = "%" + searchQuery.trim().toLowerCase() + "%";
                Predicate codeMatch = cb.like(cb.lower(root.get("code")), pattern);
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(codeMatch, titleMatch, descMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
