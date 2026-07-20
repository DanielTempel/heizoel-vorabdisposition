package heizoel.backend.application.port.in.tracking;

public interface GetTrackingInfoUseCase {

    TrackingInfoResult getTrackingInfo(String token);
}
