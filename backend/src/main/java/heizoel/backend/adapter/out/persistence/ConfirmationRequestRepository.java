package heizoel.backend.adapter.out.persistence;

import heizoel.backend.domain.ConfirmationRequest;
import heizoel.backend.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConfirmationRequestRepository extends JpaRepository<ConfirmationRequest, Long> {

    Optional<ConfirmationRequest> findTopByOrderOrderByIdDesc(
            Order order
    );

    @Query("""
            select cr
            from ConfirmationRequest cr
            where cr.token = :token
              and cr.id = (
                  select max(latest.id)
                  from ConfirmationRequest latest
                  where latest.order = cr.order
              )
            """)
    Optional<ConfirmationRequest> findLatestByToken(
            @Param("token") String token
    );
}
