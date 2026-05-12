package heizoel.backend.customer.application.interfaces;

import heizoel.backend.customer.api.dto.CustomerConfirmationPreviewDto;

public interface CustomerConfirmationService {

    CustomerConfirmationPreviewDto getConfirmationPreview(String token);

    void confirm(String token, String customerComment);

    void reject(String token, String customerComment);
}
