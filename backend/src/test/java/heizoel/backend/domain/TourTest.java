package heizoel.backend.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TourTest {

    @Test
    void shouldBeEqualWhenBothValuesAreEqual() {
        Tour first = Tour.of(
                "17",
                "WÜ-AB 123"
        );

        Tour second = Tour.of(
                "17",
                "WÜ-AB 123"
        );

        assertThat(first).isEqualTo(second);
    }

    @Test
    void shouldNotBeEqualWhenTourNumberDiffers() {
        Tour first = Tour.of(
                "17",
                "WÜ-AB 123"
        );

        Tour second = Tour.of(
                "18",
                "WÜ-AB 123"
        );

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void shouldNotBeEqualWhenVehicleLicensePlateDiffers() {
        Tour first = Tour.of(
                "17",
                "WÜ-AB 123"
        );

        Tour second = Tour.of(
                "17",
                "WÜ-CD 456"
        );

        assertThat(first).isNotEqualTo(second);
    }
}
