package cl.duoc.barriodigital.catalog.repository;

import cl.duoc.barriodigital.catalog.model.ProcedureType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcedureTypeRepository extends JpaRepository<ProcedureType, Long> {
    Optional<ProcedureType> findByCode(String code);
}
