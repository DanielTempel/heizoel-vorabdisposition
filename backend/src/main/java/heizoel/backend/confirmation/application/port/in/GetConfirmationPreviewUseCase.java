package heizoel.backend.confirmation.application.port.in;

public interface GetConfirmationPreviewUseCase {

    GetConfirmationPreviewResult getConfirmationPreview(String token);
}
