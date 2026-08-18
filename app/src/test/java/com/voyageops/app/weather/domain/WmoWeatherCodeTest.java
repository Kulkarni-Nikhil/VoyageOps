package com.voyageops.app.weather.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WmoWeatherCodeTest {

    @Test
    void describesClearSky() {
        assertThat(WmoWeatherCode.describe(0)).isEqualTo("Clear sky");
    }

    @Test
    void describesRainGroups() {
        assertThat(WmoWeatherCode.describe(61)).isEqualTo("Rain");
        assertThat(WmoWeatherCode.describe(63)).isEqualTo("Rain");
    }

    @Test
    void describesThunderstorm() {
        assertThat(WmoWeatherCode.describe(95)).isEqualTo("Thunderstorm");
        assertThat(WmoWeatherCode.describe(96)).isEqualTo("Thunderstorm with hail");
    }

    @Test
    void fallsBackToUnknown() {
        assertThat(WmoWeatherCode.describe(999)).isEqualTo("Unknown");
    }
}
