package tech.com.project.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "status_transition")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_status_id", nullable = false)
    private TicketStatus fromStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_status_id", nullable = false)
    private TicketStatus toStatus;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
