package heizoel.backend.customer.application;


import heizoel.backend.customer.application.interfaces.CustomerResponseService;
import heizoel.backend.customer.domain.entity.CustomerResponse;
import heizoel.backend.customer.domain.repository.CustomerResponseRepository;
import heizoel.backend.dispo.domain.entity.ConfirmationRequest;
import heizoel.backend.customer.domain.CustomerResponseType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CustomerResponseServiceImpl implements CustomerResponseService {

    private final CustomerResponseRepository customerResponseRepository;

    @Override
    public boolean existsFor(ConfirmationRequest confirmationRequest) {
        return customerResponseRepository.existsByConfirmationRequest(confirmationRequest);
    }

    @Override
    public CustomerResponse create(
            ConfirmationRequest confirmationRequest,
            CustomerResponseType responseType,
            String customerComment
    ) {
        CustomerResponse customerResponse = new CustomerResponse();
        customerResponse.setConfirmationRequest(confirmationRequest);
        customerResponse.setResponseType(responseType);
        customerResponse.setComment(customerComment);
        customerResponse.setReceivedAt(Instant.now());

        return customerResponseRepository.save(customerResponse);
    }

}
