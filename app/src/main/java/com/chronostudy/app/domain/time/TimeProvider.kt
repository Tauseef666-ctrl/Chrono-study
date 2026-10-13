package com.chronostudy.app.domain.time

/**
 * Abstraction over the two clocks the app depends on.
 *
 * - [wallClockMillis] is the user-visible clock (UTC epoch millis). It is used only for
 *   calendar grouping and display. It can jump when the user changes the device time or when a
 *   time-zone/DST change occurs.
 * - [elapsedRealtimeMillis] is a monotonic clock that keeps advancing while the device is on,
 *   including during sleep. It is used to measure how much *active focus time* elapsed so that
 *   changing the wall clock can never make a countdown jump, be double counted, or go negative.
 */
interface TimeProvider {
    fun wallClockMillis(): Long
    fun elapsedRealtimeMillis(): Long
    fun timeZoneOffsetMillis(): Int
}

class SystemTimeProvider : TimeProvider {
    override fun wallClockMillis(): Long = System.currentTimeMillis()
    override fun elapsedRealtimeMillis(): Long = android.os.SystemClock.elapsedRealtime()
    override fun timeZoneOffsetMillis(): Int =
        java.util.TimeZone.getDefault().getOffset(wallClockMillis())
}
