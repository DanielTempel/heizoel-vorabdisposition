package heizoel.backend.application.port.out.persistence;

import heizoel.backend.application.model.overview.TourOverviewItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface TourOverviewQueryPort {

    Page<TourOverviewItem> findTours(
            TourOverviewFilter filter,
            Pageable pageable
    );


}
