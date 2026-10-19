package cl.duoc.barriodigital.catalog.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "procedure_types")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcedureType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 100)
    private String department;

    @Column(nullable = false)
    private Integer dailyQuota;

    @Column(nullable = false)
    private Integer availableQuota;

    @Column(length = 500)
    private String requirements;

    @Column(nullable = false)
    private Boolean active;
}
