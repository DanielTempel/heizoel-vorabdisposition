package heizoel.backend.application.port.in.overview;

import heizoel.backend.application.model.overview.ConfirmationDetail;

public interface GetConfirmationDetailUseCase {

    ConfirmationDetail getOrderDetail(GetDashboardOrderDetailQuery query);

}
