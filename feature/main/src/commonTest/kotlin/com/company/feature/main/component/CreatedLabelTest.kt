package com.company.feature.main.component

import kotlin.test.Test
import kotlin.test.assertEquals

class CreatedLabelTest {

    private val hour = 60L * 60 * 1000
    private val day = 24 * hour
    private val utc: (Long) -> Long = { 0L }
    private val korea: (Long) -> Long = { 9 * hour }

    /** 2023-11-14 22:13:20 UTC, which is 2023-11-15 07:13:20 in Korea. */
    private val now = 1_700_000_000_000L

    @Test
    fun sameLocalDay_andFutureTime_areToday() {
        assertEquals(CreatedDay.Today, createdDay(now - hour, now, utc))
        assertEquals(CreatedDay.Today, createdDay(now + day, now, utc))
    }

    @Test
    fun localMidnight_decidesYesterday() {
        // 8시간 전은 UTC로는 같은 날이지만, 한국 시간으로는 전날 밤 11시다.
        val created = now - 8 * hour
        assertEquals(CreatedDay.Today, createdDay(created, now, utc))
        assertEquals(CreatedDay.Yesterday, createdDay(created, now, korea))
    }

    @Test
    fun twoToSixDays_areDaysAgo_andSevenDaysIsTheDate() {
        assertEquals(CreatedDay.DaysAgo(2), createdDay(now - 2 * day, now, utc))
        assertEquals(CreatedDay.DaysAgo(6), createdDay(now - 6 * day, now, utc))
        assertEquals(CreatedDay.Date(month = 11, day = 7), createdDay(now - 7 * day, now, utc))
    }

    @Test
    fun date_usesTheLocalCalendar() {
        val later = now + 30 * day
        assertEquals(CreatedDay.Date(month = 11, day = 14), createdDay(now, later, utc))
        assertEquals(CreatedDay.Date(month = 11, day = 15), createdDay(now, later, korea))
        // 2024-02-29 00:00 UTC: 윤년의 2월 29일.
        assertEquals(CreatedDay.Date(month = 2, day = 29), createdDay(1_709_164_800_000L, later + 365 * day, utc))
        // 2023-12-31 23:00 UTC는 한국 시간으로 2024년 1월 1일이다.
        val newYearEve = 1_704_063_600_000L
        assertEquals(CreatedDay.Date(month = 12, day = 31), createdDay(newYearEve, later + 365 * day, utc))
        assertEquals(CreatedDay.Date(month = 1, day = 1), createdDay(newYearEve, later + 365 * day, korea))
    }
}
