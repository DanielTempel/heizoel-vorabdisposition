package heizoel.backend.confirmation.application.port.in;

import heizoel.backend.confirmation.adapter.in.web.customer.dto.CustomerConfirmationPreviewDto;

public interface CustomerConfirmationService {

    CustomerConfirmationPreviewDto getConfirmationPreview(String token);

    void confirm(String token, String customerComment);

    void reject(String token, String customerComment);
}

