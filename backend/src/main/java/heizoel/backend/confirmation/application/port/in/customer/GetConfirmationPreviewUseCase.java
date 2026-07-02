package heizoel.backend.confirmation.application.port.in.customer;

public interface GetConfirmationPreviewUseCase {

    GetConfirmationPreviewResult getConfirmationPreview(String token);
}
