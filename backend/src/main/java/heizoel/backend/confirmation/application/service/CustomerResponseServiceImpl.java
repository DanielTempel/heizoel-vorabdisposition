package heizoel.backend.confirmation.application.service;


import heizoel.backend.confirmation.application.port.out.CustomerResponseService;
import heizoel.backend.confirmation.domain.model.CustomerResponse;
import heizoel.backend.confirmation.adapter.out.persistence.CustomerResponseRepository;
import heizoel.backend.confirmation.domain.model.ConfirmationRequest;
import heizoel.backend.confirmation.domain.model.CustomerResponseType;
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

