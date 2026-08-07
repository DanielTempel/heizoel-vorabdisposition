package heizoel.backend.adapter.out.persistence;

import heizoel.backend.domain.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findByCompanyIdAndExternalOrderId(
            Long companyId,
            String externalOrderId
    );


}
