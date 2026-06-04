package heizoel.backend.location.persistence;

import heizoel.backend.location.application.dto.TrackingTokenBinding;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ConfirmationRequestLookupRepository {

    private final JdbcTemplate jdbcTemplate;

    public Optional<TrackingTokenBinding> findLatestByExternalOrderId(String externalOrderId) {
        return jdbcTemplate.query("""
                        SELECT os.external_order_id, cr.token
                        FROM order_snapshot os
                        JOIN confirmation_request cr ON cr.order_snapshot_id = os.id
                        WHERE os.external_order_id = ?
                        ORDER BY cr.id DESC
                        LIMIT 1
                        """,
                resultSet -> resultSet.next()
                        ? Optional.of(new TrackingTokenBinding(
                        resultSet.getString("external_order_id"),
                        resultSet.getString("token")
                ))
                        : Optional.empty(),
                externalOrderId
        );
    }
}
