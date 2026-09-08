package heizoel.backend.application.service.overview;

import heizoel.backend.application.exception.OrderNotFoundException;
import heizoel.backend.application.model.overview.ConfirmationDetail;
import heizoel.backend.application.port.in.overview.*;
import heizoel.backend.application.port.out.persistence.ConfirmationDetailQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
                .orElseThrow(() -> {
                    log.warn(
                            "Rejecting confirmation detail lookup: reason=ORDER_NOT_FOUND, companyId={}, externalOrderId={}",
                            query.companyContext().companyId(),
                            query.externalOrderId()
                    );
                    return new OrderNotFoundException(
                            "Order was not found."
                    );
                });
    }
}
