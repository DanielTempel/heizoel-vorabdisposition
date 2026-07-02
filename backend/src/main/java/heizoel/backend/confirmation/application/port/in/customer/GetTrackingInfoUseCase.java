package heizoel.backend.confirmation.application.port.in.customer;

public interface GetTrackingInfoUseCase {

    TrackingInfoResult getTrackingInfo(String token);
}
