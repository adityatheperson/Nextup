package com.adith.nextup

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy

@Composable
fun WalkingSetup(status: WalkingStatus, onDismiss: () -> Unit, onSave: (String) -> Unit,
    onRemove: () -> Unit, onPrecise: () -> Unit, onBackground: () -> Unit, busy: Boolean) {
    var key by remember { mutableStateOf("") }
    val uri = LocalUriHandler.current
    // Credential entry is intentionally not saveable and is hidden in screenshots/recents.
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
        title = { Text("Walking times") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Show walks of 40 minutes or less. Updates after about 200 m of movement, with periodic checks. Android may delay updates.")
                Text("Your position and event destinations go to openrouteservice to calculate routes. Event titles stay on your phone. Tracking stops when you dismiss NextUp’s notification.")
                OutlinedTextField(value = key, onValueChange = { key = it }, singleLine = true,
                    label = { Text(if (status.configured) "Replace API key" else "openrouteservice API key") },
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), enabled = !busy)
                Text(status.message, style = MaterialTheme.typography.bodySmall)
                if (!status.precise) OutlinedButton(onPrecise, enabled = !busy) { Text("Allow precise location") }
                if (status.precise && !status.background) {
                    Text("For movement updates while NextUp is closed, choose Permissions → Location → Allow all the time in Android settings.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onBackground, enabled = !busy) { Text("Allow background location") }
                }
                Text("Estimates depend on mapped footpaths. Unclear destinations and unavailable routes are hidden.", style = MaterialTheme.typography.bodySmall)
                TextButton({ uri.openUri("https://openrouteservice.org/") }) { Text("Routes: openrouteservice · © OpenStreetMap contributors") }
                if (status.configured) TextButton(onRemove, enabled = !busy) { Text("Remove key and disable") }
            }
        },
        confirmButton = { TextButton({ onSave(key.trim()); key = "" }, enabled = !busy && key.trim().length >= 16) { Text(if (busy) "Saving…" else "Save key") } },
        dismissButton = { TextButton(onDismiss, enabled = !busy) { Text("Done") } })
}
