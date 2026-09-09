package com.example.ui.util

import android.content.Context
import androidx.annotation.StringRes

/**
 * Lightweight UI message descriptor containing a string resource ID and optional format arguments.
 * Allows ViewModels to emit user-facing messages without holding references to Android Context.
 */
data class UiMessage(
    @StringRes val resId: Int,
    val formatArgs: List<Any> = emptyList()
) {
    constructor(@StringRes resId: Int, vararg formatArgs: Any) : this(resId, formatArgs.toList())

    fun asString(context: Context): String {
        return if (formatArgs.isEmpty()) {
            context.getString(resId)
        } else {
            val resolvedArgs = formatArgs.map { arg ->
                if (arg is UiMessage) arg.asString(context) else arg
            }.toTypedArray()
            context.getString(resId, *resolvedArgs)
        }
    }
}
