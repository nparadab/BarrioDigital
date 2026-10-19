package cl.duoc.barriodigital.requests.repository;

import cl.duoc.barriodigital.requests.model.Request;
import cl.duoc.barriodigital.requests.model.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
    Optional<Request> findByTrackingNumber(String trackingNumber);
    List<Request> findByCitizenDni(String citizenDni);
    List<Request> findByCitizenEmail(String citizenEmail);
    List<Request> findByStatus(RequestStatus status);
}
