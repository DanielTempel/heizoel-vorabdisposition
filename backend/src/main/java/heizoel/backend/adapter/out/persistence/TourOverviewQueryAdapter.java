package heizoel.backend.adapter.out.persistence;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import heizoel.backend.application.model.overview.OrderOverviewItem;
import heizoel.backend.application.model.overview.TourOverviewItem;
import heizoel.backend.application.port.out.persistence.TourNumberFilter;
import heizoel.backend.application.port.out.persistence.TourOverviewFilter;
import heizoel.backend.application.port.out.persistence.TourOverviewQueryPort;
import heizoel.backend.domain.QConfirmationRequest;
import heizoel.backend.domain.QOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class TourOverviewQueryAdapter implements TourOverviewQueryPort {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<TourOverviewItem> findTours(
            TourOverviewFilter filter,
            Pageable pageable
    ) {
        QOrder order = QOrder.order;
        QConfirmationRequest confirmationRequest = QConfirmationRequest.confirmationRequest;

        BooleanBuilder tourWhere = buildWhere(
                filter,
                order,
                confirmationRequest
        );

        List<Tuple> tourRows = queryFactory
                .select(
                        order.tour.tourNumber,
                        order.tour.vehicleLicensePlate,
                        confirmationRequest.deliverySlot.date
                )
                .from(order)
                .join(confirmationRequest)
                .on(confirmationRequest.order.eq(order))
                .where(tourWhere)
                .groupBy(
                        order.tour.tourNumber,
                        order.tour.vehicleLicensePlate,
                        confirmationRequest.deliverySlot.date
                )
                .orderBy(
                        confirmationRequest.deliverySlot.date.asc(),
                        order.tour.tourNumber.asc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(order.tour.tourNumber.countDistinct())
                .from(order)
                .join(confirmationRequest)
                .on(confirmationRequest.order.eq(order))
                .where(tourWhere)
                .fetchOne();

        if (tourRows.isEmpty()) {
            return new PageImpl<>(
                    List.of(),
                    pageable,
                    total != null ? total : 0L
            );
        }

        Map<String, TourAccumulator> toursByNumber =
                createTourAccumulators(
                        tourRows,
                        order,
                        confirmationRequest
                );

        loadOrders(
                filter,
                toursByNumber,
                order,
                confirmationRequest
        );

        List<TourOverviewItem> content = toursByNumber
                .values()
                .stream()
                .map(TourAccumulator::toOverviewItem)
                .toList();

        return new PageImpl<>(
                content,
                pageable,
                total != null ? total : 0L
        );
    }

    @Override
    public List<String> findTourNumbers(
            TourNumberFilter filter
    ) {
        QOrder order = QOrder.order;
        QConfirmationRequest confirmationRequest = QConfirmationRequest.confirmationRequest;

        BooleanBuilder where = new BooleanBuilder();

        where.and(order.company.id.eq(filter.companyId()));

        where.and(latestConfirmationRequestOnly(
                        order,
                        confirmationRequest
                )
        );

        applyDateFilter(
                where,
                filter.dateFrom(),
                filter.dateTo(),
                confirmationRequest
        );

        if (filter.search() != null) {
            String search = filter.search().toLowerCase(Locale.ROOT);

            where.and(
                    order.tour.tourNumber
                            .lower()
                            .contains(search)
            );
        }

        List<Tuple> rows = queryFactory
                .select(
                        order.tour.tourNumber,
                        confirmationRequest.deliverySlot.date
                )
                .from(order)
                .join(confirmationRequest)
                .on(confirmationRequest.order.eq(order))
                .where(where)
                .groupBy(
                        order.tour.tourNumber,
                        confirmationRequest.deliverySlot.date
                )
                .orderBy(
                        confirmationRequest.deliverySlot.date.asc(),
                        order.tour.tourNumber.asc()
                )
                .fetch();

        return rows.stream()
                .map(row -> row.get(order.tour.tourNumber))
                .toList();
    }



    private Map<String, TourAccumulator> createTourAccumulators(
            List<Tuple> tourRows,
            QOrder order,
            QConfirmationRequest confirmationRequest
    ) {
        Map<String, TourAccumulator> result = new LinkedHashMap<>();

        for (Tuple row : tourRows) {
            String tourNumber = row.get(order.tour.tourNumber);
            String vehicleLicensePlate = row.get(order.tour.vehicleLicensePlate);
            LocalDate deliveryDate = row.get(confirmationRequest.deliverySlot.date);

            result.put(
                    tourNumber,
                    new TourAccumulator(
                            tourNumber,
                            vehicleLicensePlate,
                            deliveryDate,
                            new ArrayList<>()
                    )
            );
        }

        return result;
    }

    private void loadOrders(
            TourOverviewFilter filter,
            Map<String, TourAccumulator> toursByNumber,
            QOrder order,
            QConfirmationRequest confirmationRequest
    ) {
        List<String> tourNumbers = List.copyOf(toursByNumber.keySet());

        BooleanBuilder orderWhere = buildWhere(
                filter,
                order,
                confirmationRequest
        );

        orderWhere.and(order.tour.tourNumber.in(tourNumbers));

        ConstructorExpression<OrderOverviewItem> orderProjection =
                Projections.constructor(
                        OrderOverviewItem.class,
                        order.externalOrderId,
                        order.customerName,
                        order.deliveryAddress,
                        confirmationRequest.deliverySlot.start,
                        confirmationRequest.deliverySlot.end,
                        confirmationRequest.communicationChannel,
                        order.confirmationStatus,
                        confirmationRequest.expiresAt
                );

        List<Tuple> orderRows = queryFactory
                .select(
                        order.tour.tourNumber,
                        orderProjection
                )
                .from(order)
                .join(confirmationRequest)
                .on(confirmationRequest.order.eq(order))
                .where(orderWhere)
                .orderBy(
                        confirmationRequest.deliverySlot.date.asc(),
                        order.tour.tourNumber.asc(),
                        confirmationRequest.deliverySlot.start.asc(),
                        confirmationRequest.deliverySlot.end.asc(),
                        order.externalOrderId.asc()
                )
                .fetch();

        for (Tuple row : orderRows) {
            String tourNumber = row.get(order.tour.tourNumber);
            OrderOverviewItem orderItem = row.get(orderProjection);
            TourAccumulator tour = toursByNumber.get(tourNumber);

            if (tour != null && orderItem != null) {
                tour.orders().add(orderItem);
            }
        }
    }

    private BooleanBuilder buildWhere(
            TourOverviewFilter filter,
            QOrder order,
            QConfirmationRequest confirmationRequest
    ) {
        BooleanBuilder where = new BooleanBuilder();

        where.and(order.company.id.eq(filter.companyId()));

        where.and(latestConfirmationRequestOnly(
                        order,
                        confirmationRequest
                )
        );

        applyTourFilter(where, filter, order);

        applyDateFilter(
                where,
                filter.dateFrom(),
                filter.dateTo(),
                confirmationRequest
        );

        applyStatusFilter(where, filter, order);

        applySearchFilter(where, filter, order);

        return where;
    }

    private BooleanExpression latestConfirmationRequestOnly(
            QOrder order,
            QConfirmationRequest confirmationRequest
    ) {
        QConfirmationRequest latestRequest =
                new QConfirmationRequest("latestRequest");

        return confirmationRequest.id.eq(
                JPAExpressions
                        .select(latestRequest.id.max())
                        .from(latestRequest)
                        .where(
                                latestRequest.order.eq(order)
                        )
        );
    }

    private void applyDateFilter(
            BooleanBuilder where,
            LocalDate dateFrom,
            LocalDate dateTo,
            QConfirmationRequest confirmationRequest
    ) {
        if (dateFrom != null) {
            where.and(confirmationRequest.deliverySlot.date.goe(dateFrom));
        }

        if (dateTo != null) {
            where.and(confirmationRequest.deliverySlot.date.loe(dateTo));
        }
    }

    private void applyStatusFilter(
            BooleanBuilder where,
            TourOverviewFilter filter,
            QOrder order
    ) {
        if (filter.statuses().isEmpty()) {
            return;
        }
        where.and(order.confirmationStatus.in(filter.statuses()));
    }

    private void applySearchFilter(
            BooleanBuilder where,
            TourOverviewFilter filter,
            QOrder order
    ) {
        if (filter.search() == null) {
            return;
        }

        String search = filter.search().toLowerCase(Locale.ROOT);

        where.and(order.tour.tourNumber
                        .lower()
                        .contains(search)
                        .or(order.tour.vehicleLicensePlate
                                .lower()
                                .contains(search))
                        .or(order.externalOrderId
                                .lower()
                                .contains(search))
                        .or(order.customerName
                                .lower()
                                .contains(search))
                        .or(order.deliveryAddress
                                .lower()
                                .contains(search))
        );
    }

    private void applyTourFilter(
            BooleanBuilder where,
            TourOverviewFilter filter,
            QOrder order
    ) {
        if (filter.tourNumbers().isEmpty()) {
            return;
        }

        where.and(
                order.tour.tourNumber.in(
                        filter.tourNumbers()
                )
        );
    }

    private record TourAccumulator(
            String tourNumber,
            String vehicleLicensePlate,
            LocalDate deliveryDate,
            List<OrderOverviewItem> orders
    ) {

        private TourOverviewItem toOverviewItem() {
            return new TourOverviewItem(
                    tourNumber,
                    vehicleLicensePlate,
                    deliveryDate,
                    List.copyOf(orders)
            );
        }
    }
}
