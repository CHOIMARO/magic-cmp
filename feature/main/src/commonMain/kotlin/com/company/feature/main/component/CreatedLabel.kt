package com.company.feature.main.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.company.feature.main.resources.Res
import com.company.feature.main.resources.created_date
import com.company.feature.main.resources.created_days_ago
import com.company.feature.main.resources.created_today
import com.company.feature.main.resources.created_yesterday
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.jetbrains.compose.resources.stringResource

/** One day in milliseconds. */
private const val DAY_MILLIS = 24L * 60 * 60 * 1000

/** Number of days before the label changes from "N일 전" to the date. */
private const val DAYS_AGO_LIMIT = 7

/**
 * Returns the offset of the device time zone from UTC at a time, in milliseconds. The offset includes summer time.
 *
 * @param epochMillis Time in milliseconds since 1970-01-01 UTC.
 */
internal expect fun utcOffsetMillis(epochMillis: Long): Long

/** Day of a creation time on the local calendar, relative to today. */
internal sealed interface CreatedDay {
    /** Today : The same day as today. A time in the future is also today. */
    data object Today : CreatedDay

    /** Yesterday : One day before today. */
    data object Yesterday : CreatedDay

    /** DaysAgo(days) : 2 to 6 days before today. */
    data class DaysAgo(val days: Int) : CreatedDay

    /** Date(month, day) : 7 or more days before today. [month] is 1 to 12. */
    data class Date(val month: Int, val day: Int) : CreatedDay
}

/**
 * Finds the local calendar day of [createdAt], relative to the day of [now].
 *
 * @param createdAt Creation time in milliseconds since 1970-01-01 UTC.
 * @param now Current time in milliseconds since 1970-01-01 UTC.
 * @param offsetOf Time zone offset from UTC at a time. Tests give a fixed offset.
 */
internal fun createdDay(createdAt: Long, now: Long, offsetOf: (Long) -> Long = ::utcOffsetMillis): CreatedDay {
    val createdEpochDay = (createdAt + offsetOf(createdAt)).floorDiv(DAY_MILLIS)
    val todayEpochDay = (now + offsetOf(now)).floorDiv(DAY_MILLIS)
    val days = todayEpochDay - createdEpochDay
    return when {
        days <= 0 -> CreatedDay.Today
        days == 1L -> CreatedDay.Yesterday
        days < DAYS_AGO_LIMIT -> CreatedDay.DaysAgo(days.toInt())
        else -> civilDate(createdEpochDay)
    }
}

/**
 * Converts a day count since 1970-01-01 to the month and the day on the Gregorian calendar.
 *
 * The algorithm is "civil_from_days" of Howard Hinnant. It works for all days without a date library.
 */
private fun civilDate(epochDay: Long): CreatedDay.Date {
    val shifted = epochDay + 719_468
    val era = shifted.floorDiv(146_097L)
    val dayOfEra = shifted - era * 146_097
    val yearOfEra = (dayOfEra - dayOfEra / 1_460 + dayOfEra / 36_524 - dayOfEra / 146_096) / 365
    val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
    val monthIndex = (5 * dayOfYear + 2) / 153
    val day = dayOfYear - (153 * monthIndex + 2) / 5 + 1
    val month = if (monthIndex < 10) monthIndex + 3 else monthIndex - 9
    return CreatedDay.Date(month = month.toInt(), day = day.toInt())
}

/** Current time in milliseconds since 1970-01-01 UTC. It stays the same while the caller stays on the screen. */
@OptIn(ExperimentalTime::class)
@Composable
internal fun rememberNow(): Long = remember { Clock.System.now().toEpochMilliseconds() }

/**
 * Returns the label of a creation time: "오늘", "어제", "N일 전", or "M월 D일".
 *
 * @param createdAt Creation time in milliseconds since 1970-01-01 UTC.
 * @param now Current time in milliseconds since 1970-01-01 UTC.
 */
@Composable
internal fun createdLabel(createdAt: Long, now: Long): String = when (val day = createdDay(createdAt, now)) {
    CreatedDay.Today -> stringResource(Res.string.created_today)
    CreatedDay.Yesterday -> stringResource(Res.string.created_yesterday)
    is CreatedDay.DaysAgo -> stringResource(Res.string.created_days_ago, day.days)
    is CreatedDay.Date -> stringResource(Res.string.created_date, day.month, day.day)
}
