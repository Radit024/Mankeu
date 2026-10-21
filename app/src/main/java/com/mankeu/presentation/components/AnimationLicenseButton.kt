package com.mankeu.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mankeu.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Offline, complete license and creator notice; third-party artwork is not relicensed as GPL. */
@Composable
fun AnimationLicenseButton() {
    var open by remember { mutableStateOf(false) }
    TextButton(onClick = { open = true }) { Text(stringResource(R.string.lottie_license_title)) }
    if (open) {
        val context = LocalContext.current.applicationContext
        val unavailable = stringResource(R.string.lottie_license_unavailable)
        val notice by produceState<String?>(null, context) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    listOf("lottie-attribution.txt", "lottie-simple-license.txt").joinToString("\n\n") { file ->
                        context.assets.open("licenses/$file").bufferedReader().use { it.readText() }
                    }
                }.getOrElse { unavailable }
            }
        }
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(stringResource(R.string.lottie_license_title)) },
            text = {
                Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                    Text(notice ?: stringResource(R.string.lottie_license_loading), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { open = false }) { Text(stringResource(R.string.action_close)) }
            },
        )
    }
}
