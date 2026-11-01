package com.company.feature.main.component

import androidx.compose.runtime.Composable
import com.company.feature.main.resources.Res
import com.company.feature.main.resources.storage_size_gb
import com.company.feature.main.resources.storage_size_mb
import com.company.feature.main.resources.storage_size_under_1mb
import org.jetbrains.compose.resources.stringResource

/** Bytes in one megabyte. The labels use decimal units, as the phone storage settings do. */
private const val MEGABYTE = 1_000_000L

/** Megabytes from which the label shows gigabytes. */
private const val GIGABYTE_FROM_MEGABYTES = 1000L

/** Rounded size of the storage that works use (D51). */
internal sealed interface StorageSize {
    /** UnderOneMegabyte : Less than 1 MB ("1MB 미만"). */
    data object UnderOneMegabyte : StorageSize

    /** Megabytes(value) : 1 to 999 MB, rounded to a whole number. */
    data class Megabytes(val value: Long) : StorageSize

    /** Gigabytes(tenths) : 1 GB or more, in tenths of a GB. 12 is 1.2 GB. */
    data class Gigabytes(val tenths: Long) : StorageSize
}

/**
 * Rounds a size for the label.
 *
 * @param bytes Size in bytes.
 */
internal fun storageSize(bytes: Long): StorageSize {
    if (bytes < MEGABYTE) return StorageSize.UnderOneMegabyte
    val megabytes = (bytes + MEGABYTE / 2) / MEGABYTE
    if (megabytes < GIGABYTE_FROM_MEGABYTES) return StorageSize.Megabytes(megabytes)
    val tenthOfGigabyte = MEGABYTE * GIGABYTE_FROM_MEGABYTES / 10
    return StorageSize.Gigabytes((bytes + tenthOfGigabyte / 2) / tenthOfGigabyte)
}

/**
 * Returns the label of a storage size: "1MB 미만", "320MB", or "1.2GB" (D51).
 *
 * @param bytes Size in bytes.
 */
@Composable
internal fun storageSizeLabel(bytes: Long): String = when (val size = storageSize(bytes)) {
    StorageSize.UnderOneMegabyte -> stringResource(Res.string.storage_size_under_1mb)
    is StorageSize.Megabytes -> stringResource(Res.string.storage_size_mb, size.value)
    is StorageSize.Gigabytes -> stringResource(Res.string.storage_size_gb, "${size.tenths / 10}.${size.tenths % 10}")
}
