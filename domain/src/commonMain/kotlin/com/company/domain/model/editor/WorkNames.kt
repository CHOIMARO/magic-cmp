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

/** Number of an automatic work name: digits 0 to 9 without a leading zero, from 2 to [Int.MAX_VALUE]. Otherwise null. */
private fun String.workNumber(): Int? {
    if (isEmpty() || startsWith('0') || any { it !in '0'..'9' }) return null
    return toIntOrNull()?.takeIf { it >= 2 }
}
