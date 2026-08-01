package com.sharedhouse.android.platform.notifications

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sharedhouse.android.R

@Composable
fun NotificationPermissionExplainer(
    modifier: Modifier = Modifier,
    onStatusChanged: (NotificationPermissionStatus) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshKey by remember { mutableIntStateOf(0) }
    val status = remember(refreshKey) { SharedHouseNotifications.permissionStatus(context) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        refreshKey += 1
        onStatusChanged(SharedHouseNotifications.permissionStatus(context))
    }
    DisposableEffect(lifecycleOwner, onStatusChanged) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshKey += 1
                onStatusChanged(SharedHouseNotifications.permissionStatus(context))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val isGranted = status == NotificationPermissionStatus.GRANTED
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (isGranted) {
                        Icons.Outlined.NotificationsActive
                    } else {
                        Icons.Outlined.NotificationsOff
                    },
                    contentDescription = null,
                )
                Text(
                    text = stringResource(
                        if (isGranted) {
                            R.string.notification_permission_enabled
                        } else {
                            R.string.notification_permission_title
                        },
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                text = stringResource(
                    if (isGranted) {
                        R.string.notification_permission_enabled_description
                    } else {
                        R.string.notification_permission_description
                    },
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (status == NotificationPermissionStatus.NOT_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Button(
                    onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                ) {
                    Text(stringResource(R.string.notification_permission_action))
                }
            } else {
                OutlinedButton(onClick = { SharedHouseNotifications.openSystemSettings(context) }) {
                    Text(stringResource(R.string.notification_open_system_settings))
                }
            }
        }
    }
}
