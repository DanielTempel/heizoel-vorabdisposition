package heizoel.backend.confirmation.application.service;

import heizoel.backend.confirmation.application.port.out.OrderSnapshotService;
import heizoel.backend.confirmation.application.model.OrderSnapshotData;
import heizoel.backend.confirmation.domain.model.OrderSnapshot;
import heizoel.backend.confirmation.domain.model.ConfirmationStatus;
import heizoel.backend.confirmation.adapter.out.persistence.OrderSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class OrderSnapshotServiceImpl implements OrderSnapshotService {

    private final OrderSnapshotRepository orderSnapshotRepository;

    @Override
    public Optional<OrderSnapshot> findByExternalOrderId(String externalOrderId) {
        return orderSnapshotRepository.findByExternalOrderId(externalOrderId);
    }

    @Override
    public OrderSnapshot create(OrderSnapshotData data) {
        OrderSnapshot orderSnapshot = new OrderSnapshot();

        orderSnapshot.setExternalOrderId(data.externalOrderId());
        orderSnapshot.setCustomerName(data.customerName());
        orderSnapshot.setCustomerEmail(data.customerEmail());
        orderSnapshot.setCustomerPhoneNumber(data.customerPhoneNumber());
        orderSnapshot.setDeliveryAddress(data.deliveryAddress());
        orderSnapshot.setProduct(data.product());
        orderSnapshot.setQuantityLiters(data.quantityLiters());
        orderSnapshot.setConfirmationStatus(ConfirmationStatus.SENT);
        orderSnapshot.setPriceDisplayText(data.priceDisplayText());

        return orderSnapshotRepository.save(orderSnapshot);
    }

    @Override
    public OrderSnapshot update(OrderSnapshot orderSnapshot, OrderSnapshotData data) {
        orderSnapshot.setCustomerName(data.customerName());
        orderSnapshot.setCustomerEmail(data.customerEmail());
        orderSnapshot.setCustomerPhoneNumber(data.customerPhoneNumber());
        orderSnapshot.setDeliveryAddress(data.deliveryAddress());
        orderSnapshot.setProduct(data.product());
        orderSnapshot.setQuantityLiters(data.quantityLiters());
        orderSnapshot.setConfirmationStatus(ConfirmationStatus.SENT);
        orderSnapshot.setPriceDisplayText(data.priceDisplayText());

        return orderSnapshotRepository.save(orderSnapshot);
    }

    @Override
    public boolean hasSameData(OrderSnapshot orderSnapshot, OrderSnapshotData data) {
        return Objects.equals(orderSnapshot.getCustomerName(), data.customerName())
                && Objects.equals(orderSnapshot.getCustomerEmail(), data.customerEmail())
                && Objects.equals(orderSnapshot.getCustomerPhoneNumber(), data.customerPhoneNumber())
                && Objects.equals(orderSnapshot.getDeliveryAddress(), data.deliveryAddress())
                && Objects.equals(orderSnapshot.getProduct(), data.product())
                && Objects.equals(orderSnapshot.getQuantityLiters(), data.quantityLiters())
                && Objects.equals(orderSnapshot.getPriceDisplayText(), data.priceDisplayText());

    }

    @Override
    public OrderSnapshot updateStatus(OrderSnapshot orderSnapshot, ConfirmationStatus status) {
        orderSnapshot.setConfirmationStatus(status);
        return orderSnapshotRepository.save(orderSnapshot);
    }

}

