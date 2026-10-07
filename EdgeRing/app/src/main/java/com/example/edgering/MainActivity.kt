package com.example.edgering

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.edgering.overlay.TriggerSide
import com.example.edgering.service.EdgeService
import com.example.edgering.service.EdgeState
import com.example.edgering.ui.settings.SettingsScreen
import com.example.edgering.ui.settings.SettingsUiState
import com.example.edgering.ui.theme.EdgeRingTheme

class MainActivity : ComponentActivity() {

    /** Bumped whenever permissions may have changed, so the UI re-reads them. */
    private val permissionTick = mutableIntStateOf(0)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionTick.intValue++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EdgeRingTheme {
                val tick = permissionTick.intValue
                val running by EdgeState.running.collectAsState()
                val latency by EdgeState.lastLatencyMs.collectAsState()
                var sideName by rememberSaveable { mutableStateOf(TriggerSide.RIGHT.name) }
                val side = TriggerSide.fromName(sideName)

                SettingsScreen(
                    state = SettingsUiState(
                        versionName = BuildConfig.VERSION_NAME,
                        overlayGranted = tick >= 0 && Settings.canDrawOverlays(this),
                        notificationsNeeded = tick >= 0 && notificationsNeeded(),
                        running = running,
                        side = side,
                        lastLatencyMs = latency,
                    ),
                    onToggleService = { enable -> toggleService(enable, side) },
                    onSideChange = { sideName = it.name },
                    onGrantOverlay = ::openOverlaySettings,
                    onGrantNotifications = ::requestNotificationPermission,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionTick.intValue++
    }

    private fun notificationsNeeded(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openOverlaySettings() {
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.toast_settings_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    private fun toggleService(enable: Boolean, side: TriggerSide) {
        if (!enable) {
            EdgeService.stop(this)
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.toast_overlay_permission_missing, Toast.LENGTH_LONG).show()
            return
        }
        if (!EdgeService.start(this, side)) {
            Toast.makeText(this, R.string.toast_service_failed, Toast.LENGTH_LONG).show()
        }
    }
}
