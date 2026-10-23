package com.company.core.domain.model.editor

// =====================================================================
// Decorations of character ranges (F03)
// =====================================================================

/**
 * Finds the decoration of each character.
 *
 * @param length Number of characters in the text.
 * @return One item for each character. The item is null when the character has no decoration.
 */
fun List<DecorationRange>.perCharacter(length: Int): List<Decoration?> {
    val result = arrayOfNulls<Decoration>(length)
    forEach { range ->
        for (index in range.start.coerceAtLeast(0) until range.end.coerceAtMost(length)) result[index] = range.decoration
    }
    return result.toList()
}

/**
 * Makes ranges from the decorations of each character. Neighbor characters with the same decoration become one range.
 * Characters without a decoration are not in a range.
 */
fun List<Decoration?>.toDecorationRanges(): List<DecorationRange> {
    val ranges = mutableListOf<DecorationRange>()
    var start = 0
    for (index in 1..size) {
        if (index == size || this[index] != this[start]) {
            val decoration = this[start]
            if (decoration != null && !decoration.isEmpty) ranges += DecorationRange(start, index, decoration)
            start = index
        }
    }
    return ranges
}

/**
 * Changes the decoration of the characters from [start] to [end].
 *
 * @param length Number of characters in the text.
 * @param start Index of the first character to change.
 * @param end Index after the last character to change.
 * @param change Makes the new decoration from the old decoration. An empty decoration is removed.
 * @return The new ranges.
 */
fun List<DecorationRange>.decorate(length: Int, start: Int, end: Int, change: (Decoration) -> Decoration): List<DecorationRange> {
    val characters = perCharacter(length).toMutableList()
    for (index in start.coerceAtLeast(0) until end.coerceAtMost(length)) {
        characters[index] = change(characters[index] ?: Decoration()).takeUnless { it.isEmpty }
    }
    return characters.toDecorationRanges()
}

/**
 * Moves the decorations after a change of the text.
 *
 * Characters before and after the changed part keep their decorations. New characters get the decoration of the
 * character before them. The decorations of removed characters go away.
 *
 * @param oldText Text before the change.
 * @param newText Text after the change.
 * @return The ranges for [newText].
 */
fun List<DecorationRange>.afterTextChange(oldText: String, newText: String): List<DecorationRange> {
    if (oldText == newText) return this
    if (isEmpty()) return emptyList()
    val old = perCharacter(oldText.length)
    val shorter = minOf(oldText.length, newText.length)
    var prefix = 0
    while (prefix < shorter && oldText[prefix] == newText[prefix]) prefix++
    var suffix = 0
    while (suffix < shorter - prefix && oldText[oldText.length - 1 - suffix] == newText[newText.length - 1 - suffix]) suffix++
    val inserted = newText.length - suffix - prefix
    val fill = if (prefix > 0) old[prefix - 1] else null
    val characters = old.take(prefix) + List(inserted.coerceAtLeast(0)) { fill } + old.drop(oldText.length - suffix)
    return characters.take(newText.length).toDecorationRanges()
}

/**
 * Keeps the decorations of the characters from [start] to [end], and moves them so that [start] becomes 0.
 *
 * @param start Index of the first character to keep.
 * @param end Index after the last character to keep.
 */
fun List<DecorationRange>.slice(start: Int, end: Int): List<DecorationRange> = mapNotNull { range ->
    val from = maxOf(range.start, start)
    val to = minOf(range.end, end)
    if (from < to) DecorationRange(from - start, to - start, range.decoration) else null
}

/**
 * Removes the spaces at the start and the end of a text, and moves the decorations with the text.
 *
 * @param text Text with the decorations.
 * @return The text without the spaces, and its decorations.
 */
fun List<DecorationRange>.trimWith(text: String): Pair<String, List<DecorationRange>> {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return trimmed to emptyList()
    val lead = text.length - text.trimStart().length
    return trimmed to slice(lead, lead + trimmed.length)
}

/**
 * Collects the values of one decoration property from [start] to [end].
 *
 * @param length Number of characters in the text.
 * @param start Index of the first character.
 * @param end Index after the last character.
 * @param value Reads the property. A character without a decoration gives the value of an empty decoration.
 * @return The different values. Two or more values mean that the range is mixed.
 */
fun <T> List<DecorationRange>.valuesIn(length: Int, start: Int, end: Int, value: (Decoration) -> T): Set<T> {
    val characters = perCharacter(length)
    return (start.coerceAtLeast(0) until end.coerceAtMost(length)).mapTo(mutableSetOf()) { value(characters[it] ?: Decoration()) }
}
