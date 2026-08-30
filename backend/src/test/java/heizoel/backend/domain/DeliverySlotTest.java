package heizoel.backend.domain;

import heizoel.backend.domain.exception.InvalidDeliveryWindowException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliverySlotTest {

    private static final LocalDate DELIVERY_DATE = LocalDate.of(2026, 7, 15);
    private static final LocalTime START = LocalTime.of(10, 0);
    private static final LocalTime END = LocalTime.of(12, 0);
    private static final Instant DELIVERY_START = Instant.parse("2026-07-15T08:00:00Z");

    @Test
    void rejectsMissingDeliveryDate() {
        assertThatThrownBy(() -> DeliverySlot.of(null, START, END))
                .isInstanceOf(InvalidDeliveryWindowException.class)
                .hasMessage("Delivery date is required.");
    }

    @ParameterizedTest
    @MethodSource("missingWindowBoundaries")
    void rejectsMissingWindowBoundary(LocalTime start, LocalTime end) {
        assertThatThrownBy(() -> DeliverySlot.of(DELIVERY_DATE, start, end))
                .isInstanceOf(InvalidDeliveryWindowException.class)
                .hasMessage("Delivery window start and end are required.");
    }

    @ParameterizedTest
    @MethodSource("invalidWindowOrderings")
    void rejectsWindowThatDoesNotEndAfterStart(LocalTime start, LocalTime end) {
        assertThatThrownBy(() -> DeliverySlot.of(DELIVERY_DATE, start, end))
                .isInstanceOf(InvalidDeliveryWindowException.class)
                .hasMessage("Delivery window start must be before delivery window end.");
    }

    @Test
    void startsAtUsesEuropeBerlinSummerTime() {
        DeliverySlot slot = DeliverySlot.of(DELIVERY_DATE, START, END);

        assertThat(slot.startsAt())
                .isEqualTo(DELIVERY_START);
    }

    @Test
    void validateStartsAfterAcceptsInstantBeforeDeliveryWindow() {
        DeliverySlot slot = DeliverySlot.of(DELIVERY_DATE, START, END);

        slot.validateStartsAfter(DELIVERY_START.minusNanos(1));
    }

    @Test
    void validateStartsAfterRejectsDeliveryWindowThatAlreadyStarted() {
        DeliverySlot slot = DeliverySlot.of(DELIVERY_DATE, START, END);

        assertThatThrownBy(() -> slot.validateStartsAfter(DELIVERY_START))
                .isInstanceOf(InvalidDeliveryWindowException.class)
                .hasMessage("Delivery window must start in the future.");
    }

    private static Stream<Arguments> missingWindowBoundaries() {
        return Stream.of(
                Arguments.of(null, END),
                Arguments.of(START, null)
        );
    }

    private static Stream<Arguments> invalidWindowOrderings() {
        return Stream.of(
                Arguments.of(START, START),
                Arguments.of(END, START)
        );
    }
}
