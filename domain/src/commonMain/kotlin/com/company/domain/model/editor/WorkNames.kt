package com.company.domain.model.editor

/**
 * Name of a new work (D53): [base], or "[base] N" with the smallest free N from 2 when a work already has the name.
 *
 * Only the names "[base]" and "[base] N" count, with N an integer from 2. A number that does not fit in an Int does
 * not count. Thus the number is at most the number of works plus 1, and a large number that the user typed has no
 * effect.
 *
 * @param base Name without a number, for example "새 작업".
 * @param names Names of the works now.
 */
fun nextWorkName(base: String, names: Collection<String>): String {
    val prefix = "$base "
    val used = names.mapNotNullTo(mutableSetOf()) { name ->
        when {
            name == base -> 1
            name.startsWith(prefix) -> name.removePrefix(prefix).workNumber()
            else -> null
        }
    }
    val number = generateSequence(1) { it + 1 }.first { it !in used }
    return if (number == 1) base else "$base $number"
}

/** Most characters in a work name that the user types (D57). */
const val MAX_WORK_NAME_LENGTH = 30

/**
 * Work name from the input of the rename popup (D57). A line break becomes a space, the spaces at the ends go away,
 * and the name keeps at most [MAX_WORK_NAME_LENGTH] characters.
 *
 * @param input Text that the user typed.
 * @return The name, or null when nothing is left.
 */
fun workNameOf(input: String): String? =
    input.replace('\n', ' ').replace('\r', ' ').trim().take(MAX_WORK_NAME_LENGTH).trimEnd().ifEmpty { null }

/** Number of an automatic work name: digits 0 to 9 without a leading zero, from 2 to [Int.MAX_VALUE]. Otherwise null. */
private fun String.workNumber(): Int? {
    if (isEmpty() || startsWith('0') || any { it !in '0'..'9' }) return null
    return toIntOrNull()?.takeIf { it >= 2 }
}
