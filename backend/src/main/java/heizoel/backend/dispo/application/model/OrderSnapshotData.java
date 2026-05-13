package heizoel.backend.dispo.application.model;

public record OrderSnapshotData(
        String externalOrderId,
        String customerName,
        String customerEmail,
        String customerPhoneNumber,
        String deliveryAddress,
        String product,
        Integer quantityLiters
) {
}