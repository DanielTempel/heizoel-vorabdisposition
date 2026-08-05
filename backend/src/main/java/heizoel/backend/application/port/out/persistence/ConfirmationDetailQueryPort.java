package heizoel.backend.application.port.out.persistence;

import heizoel.backend.application.model.overview.ConfirmationDetail;

import java.util.Optional;

public interface ConfirmationDetailQueryPort {

    Optional<ConfirmationDetail> findDetail(
            Long companyId,
            String externalOrderId
    );
}
