package heizoel.backend.application.service.overview;

import heizoel.backend.application.exception.OrderNotFoundException;
import heizoel.backend.application.model.overview.ConfirmationDetail;
import heizoel.backend.application.port.in.overview.*;
import heizoel.backend.application.port.out.persistence.ConfirmationDetailQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetConfirmationDetailService implements GetConfirmationDetailUseCase {


    private final ConfirmationDetailQueryPort confirmationDetailQueryPort;

    @Override
    @Transactional(readOnly = true)
    public ConfirmationDetail getOrderDetail(
            GetConfirmationDetailQuery query
    ) {
        return confirmationDetailQueryPort
                .findDetail(
                        query.companyContext().companyId(),
                        query.externalOrderId()
                )
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order was not found."
                ));
    }
}