package heizoel.backend.application.port.out.persistence;

import heizoel.backend.application.model.overview.ConfirmationOverviewItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface ConfirmationOverviewQueryPort {

    Page<ConfirmationOverviewItem> findOverview(
            ConfirmationOverviewFilter filter,
            Pageable pageable
    );


}
