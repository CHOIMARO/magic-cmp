package com.company.feature.main.component

import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localTimeZone

internal actual fun utcOffsetMillis(epochMillis: Long): Long {
    val date = NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0)
    return NSTimeZone.localTimeZone.secondsFromGMTForDate(date) * 1000L
}
