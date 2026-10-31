package com.company.feature.main.component

import java.util.TimeZone

internal actual fun utcOffsetMillis(epochMillis: Long): Long = TimeZone.getDefault().getOffset(epochMillis).toLong()
