package heizoel.backend.dispo.application.domain_service;

import heizoel.backend.dispo.application.interfaces.OrderSnapshotService;
import heizoel.backend.dispo.application.model.OrderSnapshotData;
import heizoel.backend.dispo.domain.entity.OrderSnapshot;
import heizoel.backend.dispo.domain.ConfirmationStatus;
import heizoel.backend.dispo.domain.repository.OrderSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
        orderSnapshot.setDeliveryAddress(data.deliveryAddress());
        orderSnapshot.setProduct(data.product());
        orderSnapshot.setQuantityLiters(data.quantityLiters());
        orderSnapshot.setConfirmationStatus(ConfirmationStatus.SENT);

        return orderSnapshotRepository.save(orderSnapshot);
    }

    @Override
    public OrderSnapshot update(OrderSnapshot orderSnapshot, OrderSnapshotData data) {
        orderSnapshot.setCustomerName(data.customerName());
        orderSnapshot.setCustomerEmail(data.customerEmail());
        orderSnapshot.setDeliveryAddress(data.deliveryAddress());
        orderSnapshot.setProduct(data.product());
        orderSnapshot.setQuantityLiters(data.quantityLiters());
        orderSnapshot.setConfirmationStatus(ConfirmationStatus.SENT);

        return orderSnapshotRepository.save(orderSnapshot);
    }

    @Override
    public boolean hasSameData(OrderSnapshot orderSnapshot, OrderSnapshotData data) {
        return orderSnapshot.getCustomerName().equals(data.customerName())
                && orderSnapshot.getCustomerEmail().equals(data.customerEmail())
                && orderSnapshot.getDeliveryAddress().equals(data.deliveryAddress())
                && orderSnapshot.getProduct().equals(data.product())
                && orderSnapshot.getQuantityLiters().equals(data.quantityLiters());
    }

    @Override
    public OrderSnapshot updateStatus(OrderSnapshot orderSnapshot, ConfirmationStatus status) {
        orderSnapshot.setConfirmationStatus(status);
        return orderSnapshotRepository.save(orderSnapshot);
    }

}
