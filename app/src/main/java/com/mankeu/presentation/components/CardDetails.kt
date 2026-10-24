package com.mankeu.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.mankeu.R

@Composable
fun CardDetails(
    stateKey: String,
    showLabel: String,
    hideLabel: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var open by rememberSaveable(stateKey) { mutableStateOf(false) }
    val description = stringResource(
        if (open) R.string.card_expanded else R.string.card_collapsed,
    )
    Column(modifier = modifier.fillMaxWidth()) {
        TextButton(
            onClick = { open = !open },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .semantics { stateDescription = description },
        ) {
            Text(if (open) hideLabel else showLabel)
        }
        if (open) content()
    }
}
