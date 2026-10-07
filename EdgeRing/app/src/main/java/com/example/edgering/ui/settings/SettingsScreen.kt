package com.example.edgering.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.edgering.R
import com.example.edgering.overlay.TriggerSide

data class SettingsUiState(
    val versionName: String,
    val overlayGranted: Boolean,
    val notificationsNeeded: Boolean,
    val running: Boolean,
    val side: TriggerSide,
    val lastLatencyMs: Long?,
)

/** Home/settings shell: permission status, start/stop switch (the in-app panic switch) and side choice. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onToggleService: (Boolean) -> Unit,
    onSideChange: (TriggerSide) -> Unit,
    onGrantOverlay: () -> Unit,
    onGrantNotifications: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PermissionsCard(state, onGrantOverlay, onGrantNotifications)
            TriggerCard(state, onToggleService, onSideChange)
            Text(
                text = stringResource(R.string.spike_note),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(R.string.version_label, state.versionName),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun PermissionsCard(
    state: SettingsUiState,
    onGrantOverlay: () -> Unit,
    onGrantNotifications: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.section_permissions), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.overlay_permission_title), style = MaterialTheme.typography.bodyLarge)
            if (state.overlayGranted) {
                Text(stringResource(R.string.overlay_permission_granted), style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(stringResource(R.string.overlay_permission_missing), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onGrantOverlay) { Text(stringResource(R.string.action_grant)) }
            }
            if (state.notificationsNeeded) {
                Text(stringResource(R.string.notification_permission_title), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.notification_permission_hint), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onGrantNotifications) { Text(stringResource(R.string.action_allow)) }
            }
        }
    }
}

@Composable
private fun TriggerCard(
    state: SettingsUiState,
    onToggleService: (Boolean) -> Unit,
    onSideChange: (TriggerSide) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.section_trigger), style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.trigger_enabled), style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = state.running,
                    onCheckedChange = onToggleService,
                    enabled = state.running || state.overlayGranted,
                )
            }
            SideOption(R.string.side_left, state.side == TriggerSide.LEFT) { onSideChange(TriggerSide.LEFT) }
            SideOption(R.string.side_right, state.side == TriggerSide.RIGHT) { onSideChange(TriggerSide.RIGHT) }
            Text(stringResource(R.string.trigger_hint), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = state.lastLatencyMs
                    ?.let { stringResource(R.string.latency_label, it) }
                    ?: stringResource(R.string.latency_none),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SideOption(@StringRes label: Int, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(stringResource(label), modifier = Modifier.padding(start = 12.dp))
    }
}
