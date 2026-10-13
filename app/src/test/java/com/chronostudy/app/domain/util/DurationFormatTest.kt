package com.chronostudy.app.domain.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DurationFormatTest {

    @Test
    fun clock_formatsUnderAnHour() {
        assertThat(DurationFormat.clock(0)).isEqualTo("00:00")
        assertThat(DurationFormat.clock(65_000)).isEqualTo("01:05")
    }

    @Test
    fun clock_formatsOverAnHour() {
        assertThat(DurationFormat.clock(3_725_000)).isEqualTo("1:02:05")
    }

    @Test
    fun clock_neverNegative() {
        assertThat(DurationFormat.clock(-5_000)).isEqualTo("00:00")
    }

    @Test
    fun human_choosesUsefulUnit() {
        assertThat(DurationFormat.human(30_000)).isEqualTo("30s")
        assertThat(DurationFormat.human(5 * 60_000L)).isEqualTo("5m")
        assertThat(DurationFormat.human(90 * 60_000L)).isEqualTo("1h 30m")
        assertThat(DurationFormat.human(120 * 60_000L)).isEqualTo("2h")
    }

    @Test
    fun minutesLabel_scales() {
        assertThat(DurationFormat.minutesLabel(25)).isEqualTo("25m")
        assertThat(DurationFormat.minutesLabel(60)).isEqualTo("1h")
    }
}
