package com.arinahub.arinaagri.calculator.presentation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Text produced outside composition — snackbars and transfer status from [AppViewModel] — that
 * still has to obey the app's language setting.
 *
 * A ViewModel has no `Context`, so those messages used to be Indonesian literals: with the app in
 * English every confirmation still came back in Indonesian. Carrying a string id instead of a
 * finished string keeps the ViewModel free of Android resource plumbing (an `@StringRes Int` is
 * just a constant), keeps it unit-testable by id rather than by matching prose, and lets the UI
 * resolve against whatever locale [ProvideAppLanguage] currently provides.
 *
 * [Raw] exists for the one case where the text is not ours: a message thrown by the domain or
 * repository layer. Those carry their own explanation and must reach the user verbatim rather than
 * being replaced by a generic translated fallback.
 */
sealed interface UiText {
    data class Res(@param:StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    data class Raw(val text: String) : UiText

    companion object {
        fun of(@StringRes id: Int, vararg args: Any): Res = Res(id, args.toList())

        /** The thrown message when there is one, otherwise our own translated fallback. */
        fun fromError(error: Throwable, @StringRes fallback: Int): UiText =
            error.message?.takeIf(String::isNotBlank)?.let(::Raw) ?: Res(fallback)
    }
}

@Composable
fun UiText.resolve(): String = when (this) {
    is UiText.Raw -> text
    is UiText.Res -> if (args.isEmpty()) stringResource(id) else stringResource(id, *args.toTypedArray())
}
