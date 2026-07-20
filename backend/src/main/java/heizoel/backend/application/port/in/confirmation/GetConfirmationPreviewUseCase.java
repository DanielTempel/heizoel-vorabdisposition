package heizoel.backend.application.port.in.confirmation;

public interface GetConfirmationPreviewUseCase {

    GetConfirmationPreviewResult getConfirmationPreview(String token);
}
