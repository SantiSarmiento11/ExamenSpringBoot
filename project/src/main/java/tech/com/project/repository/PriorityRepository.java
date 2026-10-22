package tech.com.project.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.com.project.model.Priority;

import java.util.List;
import java.util.Optional;

@Repository
public interface PriorityRepository extends JpaRepository<Priority, Integer> {
    Optional<Priority> findByName(String name);
    boolean existsByName(String name);
    List<Priority> findAllByOrderByLevelAsc();
}
