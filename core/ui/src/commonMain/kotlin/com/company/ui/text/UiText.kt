package com.company.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * Text that a ViewModel gives to the UI, for example the text of a toast or a dialog.
 *
 * A ViewModel does not read string resources. It keeps the resource and its arguments, and the UI makes the string
 * in the current language with [asString] or [resolve].
 */
@Immutable
sealed interface UiText {
    /**
     * String resource with format arguments.
     *
     * @property id String resource.
     * @property args Format arguments for `%1$s` and `%1$d`. A [UiText] argument becomes its string first.
     */
    data class Resource(val id: StringResource, val args: List<Any> = emptyList()) : UiText

    /**
     * Text that is not in the resources, for example a name that the user typed.
     *
     * @property value The text.
     */
    data class Raw(val value: String) : UiText
}

/**
 * Makes a [UiText.Resource].
 *
 * @param id String resource.
 * @param args Format arguments for `%1$s` and `%1$d`.
 */
fun uiText(id: StringResource, vararg args: Any): UiText = UiText.Resource(id, args.toList())

/** Returns the string of the text in the current language. Use it in a composable. */
@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Raw -> value
    is UiText.Resource -> stringResource(id, *args.map { if (it is UiText) it.asString() else it }.toTypedArray())
}

/** Returns the string of the text in the current language. Use it in a coroutine, for example to show a toast. */
suspend fun UiText.resolve(): String = when (this) {
    is UiText.Raw -> value
    is UiText.Resource -> getString(id, *args.map { if (it is UiText) it.resolve() else it }.toTypedArray())
}

/**
 * Reads strings outside of the UI, for example the default names that a ViewModel writes into a project.
 *
 * The app gives [ComposeStringResolver]. A test can give a fake.
 */
fun interface StringResolver {
    /**
     * Returns the string of the text in the current language.
     *
     * @param text Text to read.
     */
    suspend fun resolve(text: UiText): String
}

/** [StringResolver] that reads the Compose resources. */
object ComposeStringResolver : StringResolver {
    override suspend fun resolve(text: UiText): String = text.resolve()
}
