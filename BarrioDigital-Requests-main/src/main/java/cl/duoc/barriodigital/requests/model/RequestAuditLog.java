package cl.duoc.barriodigital.requests.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "request_audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long requestId;

    @Column(nullable = false, length = 30)
    private String trackingNumber;

    @Column(length = 30)
    private String previousStatus;

    @Column(nullable = false, length = 30)
    private String newStatus;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false, length = 100)
    private String performedBy;

    @Column(length = 50)
    private String userRole;

    @Column(length = 1000)
    private String comments;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}
