package cl.duoc.barriodigital.requests.repository;

import cl.duoc.barriodigital.requests.model.RequestAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestAuditLogRepository extends JpaRepository<RequestAuditLog, Long> {
    List<RequestAuditLog> findByRequestIdOrderByTimestampDesc(Long requestId);
    List<RequestAuditLog> findByTrackingNumberOrderByTimestampDesc(String trackingNumber);
    List<RequestAuditLog> findAllByOrderByTimestampDesc();
}
