package com.mankeu.presentation.components

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import com.mankeu.presentation.LocalMankeuSemanticColors

/** Keep readable primary ink separate from the deeper filled action in dark mode. */
@Composable
fun primaryActionButtonColors(): ButtonColors {
    val colors = LocalMankeuSemanticColors.current
    return ButtonDefaults.buttonColors(
        containerColor = colors.actionContainer,
        contentColor = colors.onActionContainer,
    )
}
