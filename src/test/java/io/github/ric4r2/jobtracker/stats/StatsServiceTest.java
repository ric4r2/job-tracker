package io.github.ric4r2.jobtracker.stats;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class StatsServiceTest {

    @ParameterizedTest(name = "{0}/{1} = {2}")
    @CsvSource({
            "0, 0, 0.0",
            "1, 3, 0.333",
            "2, 3, 0.667",
            "5, 5, 1.0",
    })
    void ratioIsRoundedAndSafeForZero(long part, long whole, double expected) {
        assertThat(StatsService.ratio(part, whole)).isEqualTo(expected);
    }
}
