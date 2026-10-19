package cl.duoc.barriodigital.requests.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String trackingNumber;

    @Column(nullable = false, length = 50)
    private String citizenDni;

    @Column(nullable = false, length = 150)
    private String citizenName;

    @Column(length = 150)
    private String citizenEmail;

    @Column(length = 30)
    private String citizenPhone;

    @Column(nullable = false)
    private Long procedureTypeId;

    @Column(nullable = false, length = 150)
    private String procedureName;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false, length = 250)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestStatus status;

    @Column(length = 100)
    private String assignedCrew;

    @Column(length = 1000)
    private String resolutionNotes;

    @Column(length = 100)
    private String createdBy;

    @Column(length = 100)
    private String admittedBy;

    @Column(length = 100)
    private String visitedBy;

    @Column(length = 100)
    private String resolvedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = RequestStatus.INGRESADO;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
