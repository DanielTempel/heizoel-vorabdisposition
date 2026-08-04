package heizoel.backend.application.port.in.overview;

import heizoel.backend.application.model.overview.TourOverviewPage;

public interface GetTourOverviewUseCase {

    TourOverviewPage getTours(GetTourOverviewQuery query);
}