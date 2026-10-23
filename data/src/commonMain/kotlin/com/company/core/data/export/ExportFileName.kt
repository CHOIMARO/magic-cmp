package com.company.core.data.export

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Characters that a file name cannot have on Android or iOS. */
private val InvalidFileNameCharacters = Regex("""[\\/:*?"<>|\s]+""")

/**
 * Makes the file name of a saved video, for example `magic_바닷가_1760000000000.mp4`.
 *
 * The time part keeps two saves of the same project apart.
 *
 * @param projectName Name of the project.
 */
@OptIn(ExperimentalTime::class)
internal fun exportFileName(projectName: String): String {
    val name = projectName.replace(InvalidFileNameCharacters, "_").trim('_').take(MAX_NAME_LENGTH).ifEmpty { "video" }
    return "magic_${name}_${Clock.System.now().toEpochMilliseconds()}.mp4"
}

/** Longest project name part of a file name. */
private const val MAX_NAME_LENGTH = 40
