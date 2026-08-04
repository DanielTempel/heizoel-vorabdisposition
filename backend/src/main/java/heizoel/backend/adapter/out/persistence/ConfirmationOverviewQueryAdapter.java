package heizoel.backend.adapter.out.persistence;


import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import heizoel.backend.domain.QConfirmationRequest;
import heizoel.backend.domain.QOrder;
import heizoel.backend.domain.ConfirmationStatus;
import heizoel.backend.application.model.overview.ConfirmationOverviewItem;
import heizoel.backend.application.port.out.persistence.ConfirmationOverviewFilter;
import heizoel.backend.application.port.out.persistence.ConfirmationOverviewQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ConfirmationOverviewQueryAdapter implements ConfirmationOverviewQueryPort {

    private static final List<ConfirmationStatus> PROBLEM_STATUSES = List.of(
            ConfirmationStatus.REJECTED,
            ConfirmationStatus.NO_RESPONSE
    );

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ConfirmationOverviewItem> findOverview(
            ConfirmationOverviewFilter filter,
            Pageable pageable
    ) {
        QOrder order = QOrder.order;
        QConfirmationRequest confirmationRequest = QConfirmationRequest.confirmationRequest;

        BooleanBuilder where = buildWhere(
                filter,
                order,
                confirmationRequest
        );

        List<ConfirmationOverviewItem> content = queryFactory
                .select(Projections.constructor(
                        ConfirmationOverviewItem.class,
                        order.externalOrderId,
                        order.customerName,
                        confirmationRequest.deliverySlot.date,
                        confirmationRequest.deliverySlot.start,
                        confirmationRequest.deliverySlot.end,
                        confirmationRequest.communicationChannel,
                        order.confirmationStatus,
                        confirmationRequest.expiresAt
                ))
                .from(order)
                .join(confirmationRequest)
                .on(confirmationRequest.order.eq(order))
                .where(where)
                .orderBy(
                        confirmationRequest.deliverySlot.date.asc(),
                        confirmationRequest.deliverySlot.start.asc(),
                        order.externalOrderId.asc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(order.id.count())
                .from(order)
                .join(confirmationRequest)
                .on(confirmationRequest.order.eq(order))
                .where(where)
                .fetchOne();

        return new PageImpl<>(
                content,
                pageable,
                total != null ? total : 0L
        );
    }

    private BooleanBuilder buildWhere(
            ConfirmationOverviewFilter filter,
            QOrder order,
            QConfirmationRequest confirmationRequest
    ) {
        BooleanBuilder where = new BooleanBuilder();

        where.and(order.company.id.eq(filter.companyId()));

        where.and(latestConfirmationRequestOnly(
                order,
                confirmationRequest
        ));

        applyDashboardScope(
                where,
                filter,
                order,
                confirmationRequest
        );

        applyStatusFilter(
                where,
                filter,
                order
        );

        applySearchFilter(
                where,
                filter,
                order
        );

        return where;
    }

    private BooleanBuilder latestConfirmationRequestOnly(
            QOrder order,
            QConfirmationRequest confirmationRequest
    ) {
        QConfirmationRequest latestConfirmationRequest =
                new QConfirmationRequest("latestConfirmationRequest");

        return new BooleanBuilder().and(
                confirmationRequest.id.eq(
                        JPAExpressions
                                .select(latestConfirmationRequest.id.max())
                                .from(latestConfirmationRequest)
                                .where(latestConfirmationRequest.order.eq(order))
                )
        );
    }

    private void applyDashboardScope(
            BooleanBuilder where,
            ConfirmationOverviewFilter filter,
            QOrder order,
            QConfirmationRequest confirmationRequest
    ) {
        if (filter.deliveryDate() != null) {
            where.and(confirmationRequest.deliverySlot.date.eq(filter.deliveryDate()));
            return;
        }

        where.and(
                confirmationRequest.deliverySlot.date.goe(filter.today())
                        .or(order.confirmationStatus.in(PROBLEM_STATUSES)
        ));
    }

    private void applyStatusFilter(
            BooleanBuilder where,
            ConfirmationOverviewFilter filter,
            QOrder order
    ) {
        if (filter.status() == null) {
            return;
        }

        where.and(order.confirmationStatus.eq(filter.status()));
    }

    private void applySearchFilter(
            BooleanBuilder where,
            ConfirmationOverviewFilter filter,
            QOrder order
    ) {
        if (filter.search() == null) {
            return;
        }

        String search = filter.search().toLowerCase();

        where.and(
                order.externalOrderId.lower().contains(search)
                        .or(order.customerName.lower().contains(search))
        );
    }
}
