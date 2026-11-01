package com.company.feature.main.component

import kotlin.test.Test
import kotlin.test.assertEquals

class StorageSizeLabelTest {

    @Test
    fun underOneMegabyte_hasItsOwnLabel() {
        assertEquals(StorageSize.UnderOneMegabyte, storageSize(0))
        assertEquals(StorageSize.UnderOneMegabyte, storageSize(999_999))
    }

    @Test
    fun megabytes_areRoundedToWholeNumbers() {
        assertEquals(StorageSize.Megabytes(1), storageSize(1_000_000))
        assertEquals(StorageSize.Megabytes(320), storageSize(320_499_999))
        assertEquals(StorageSize.Megabytes(321), storageSize(320_500_000))
        assertEquals(StorageSize.Megabytes(999), storageSize(999_499_999))
    }

    @Test
    fun fromOneThousandMegabytes_theLabelShowsGigabytesWithOneDecimal() {
        // 999.5MB는 1000MB로 반올림되므로 GB로 보여 준다.
        assertEquals(StorageSize.Gigabytes(10), storageSize(999_500_000))
        assertEquals(StorageSize.Gigabytes(12), storageSize(1_249_999_999))
        assertEquals(StorageSize.Gigabytes(13), storageSize(1_250_000_000))
    }
}
