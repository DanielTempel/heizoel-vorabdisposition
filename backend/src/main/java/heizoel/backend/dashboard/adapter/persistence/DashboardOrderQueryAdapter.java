package heizoel.backend.dashboard.adapter.persistence;


import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import heizoel.backend.confirmation.domain.model.QConfirmationRequest;
import heizoel.backend.confirmation.domain.model.QOrderSnapshot;
import heizoel.backend.confirmation.domain.model.enumeration.ConfirmationStatus;
import heizoel.backend.dashboard.application.port.in.orders.DashboardOrderRaw;
import heizoel.backend.dashboard.application.port.out.persistence.DashboardOrderFilter;
import heizoel.backend.dashboard.application.port.out.persistence.DashboardOrderQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class DashboardOrderQueryAdapter implements DashboardOrderQueryPort {

    private static final List<ConfirmationStatus> PROBLEM_STATUSES = List.of(
            ConfirmationStatus.REJECTED,
            ConfirmationStatus.NO_RESPONSE
    );

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<DashboardOrderRaw> findDashboardOrders(
            DashboardOrderFilter filter,
            Pageable pageable
    ) {
        QOrderSnapshot orderSnapshot = QOrderSnapshot.orderSnapshot;
        QConfirmationRequest confirmationRequest = QConfirmationRequest.confirmationRequest;

        BooleanBuilder where = buildWhere(
                filter,
                orderSnapshot,
                confirmationRequest
        );

        List<DashboardOrderRaw> content = queryFactory
                .select(Projections.constructor(
                        DashboardOrderRaw.class,
                        orderSnapshot.externalOrderId,
                        orderSnapshot.customerName,
                        confirmationRequest.deliveryDate,
                        confirmationRequest.deliveryWindowStart,
                        confirmationRequest.deliveryWindowEnd,
                        confirmationRequest.communicationChannel,
                        orderSnapshot.confirmationStatus,
                        confirmationRequest.expiresAt
                ))
                .from(orderSnapshot)
                .join(confirmationRequest)
                .on(confirmationRequest.orderSnapshot.eq(orderSnapshot))
                .where(where)
                .orderBy(
                        confirmationRequest.deliveryDate.asc(),
                        confirmationRequest.deliveryWindowStart.asc(),
                        orderSnapshot.externalOrderId.asc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(orderSnapshot.id.count())
                .from(orderSnapshot)
                .join(confirmationRequest)
                .on(confirmationRequest.orderSnapshot.eq(orderSnapshot))
                .where(where)
                .fetchOne();

        return new PageImpl<>(
                content,
                pageable,
                total != null ? total : 0L
        );
    }

    private BooleanBuilder buildWhere(
            DashboardOrderFilter filter,
            QOrderSnapshot orderSnapshot,
            QConfirmationRequest confirmationRequest
    ) {
        BooleanBuilder where = new BooleanBuilder();

        where.and(orderSnapshot.company.id.eq(filter.companyId()));

        where.and(latestConfirmationRequestOnly(
                orderSnapshot,
                confirmationRequest
        ));

        applyDashboardScope(
                where,
                filter,
                orderSnapshot,
                confirmationRequest
        );

        applyStatusFilter(
                where,
                filter,
                orderSnapshot
        );

        applySearchFilter(
                where,
                filter,
                orderSnapshot
        );

        return where;
    }

    private BooleanBuilder latestConfirmationRequestOnly(
            QOrderSnapshot orderSnapshot,
            QConfirmationRequest confirmationRequest
    ) {
        QConfirmationRequest latestConfirmationRequest =
                new QConfirmationRequest("latestConfirmationRequest");

        return new BooleanBuilder().and(
                confirmationRequest.id.eq(
                        JPAExpressions
                                .select(latestConfirmationRequest.id.max())
                                .from(latestConfirmationRequest)
                                .where(latestConfirmationRequest.orderSnapshot.eq(orderSnapshot))
                )
        );
    }

    private void applyDashboardScope(
            BooleanBuilder where,
            DashboardOrderFilter filter,
            QOrderSnapshot orderSnapshot,
            QConfirmationRequest confirmationRequest
    ) {
        if (filter.deliveryDate() != null) {
            where.and(confirmationRequest.deliveryDate.eq(filter.deliveryDate()));
            return;
        }

        where.and(
                confirmationRequest.deliveryDate.goe(filter.today())
                        .or(orderSnapshot.confirmationStatus.in(PROBLEM_STATUSES)
        ));
    }

    private void applyStatusFilter(
            BooleanBuilder where,
            DashboardOrderFilter filter,
            QOrderSnapshot orderSnapshot
    ) {
        if (filter.status() == null) {
            return;
        }

        where.and(orderSnapshot.confirmationStatus.eq(filter.status()));
    }

    private void applySearchFilter(
            BooleanBuilder where,
            DashboardOrderFilter filter,
            QOrderSnapshot orderSnapshot
    ) {
        if (filter.search() == null) {
            return;
        }

        String search = filter.search().toLowerCase();

        where.and(
                orderSnapshot.externalOrderId.lower().contains(search)
                        .or(orderSnapshot.customerName.lower().contains(search))
        );
    }
}
