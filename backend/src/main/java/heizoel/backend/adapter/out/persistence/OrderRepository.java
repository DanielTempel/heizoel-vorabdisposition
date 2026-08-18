package heizoel.backend.adapter.out.persistence;

import heizoel.backend.domain.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findByCompanyIdAndExternalOrderId(
            Long companyId,
            String externalOrderId
    );


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select request.order
        from ConfirmationRequest request
        where request.id = :confirmationRequestId
        """)
    Optional<Order> findByConfirmationRequestIdForUpdate(
            @Param("confirmationRequestId")
            Long confirmationRequestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select request.order
        from ConfirmationRequest request
        where request.token = :token
        """)
    Optional<Order> findByConfirmationRequestTokenForUpdate(
            @Param("token") String token
    );

}
