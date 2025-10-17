package com.example.calendar.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.example.calendar.data.CalendarEvent
import com.example.calendar.data.Task
import com.example.calendar.ui.agenda.AgendaRoute
import com.example.calendar.ui.theme.CalendarTheme

@Composable
fun CalendarApp(
    viewModel: AgendaViewModel,
    onEventClick: (CalendarEvent) -> Unit = {},
    onTaskClick: (Task) -> Unit = {}
) {
    CalendarTheme {
        val permissionController = rememberNotificationPermissionController()
        val promptTracker = rememberNotificationPromptTracker()
        var dismissed by rememberSaveable { mutableStateOf(false) }

        LaunchedEffect(permissionController.shouldShowPrompt) {
            if (!permissionController.shouldShowPrompt) {
                dismissed = false
            }
        }

        LaunchedEffect(permissionController.isGranted) {
            if (permissionController.isGranted) {
                promptTracker.clearSuppression()
            }
        }

        val notificationCard: (@Composable () -> Unit)? = if (
            permissionController.shouldShowPrompt &&
            !dismissed &&
            promptTracker.shouldShow()
        ) {
            {
                NotificationPermissionCard(
                    controller = permissionController,
                    onDismiss = {
                        dismissed = true
                        promptTracker.recordDismiss()
                    },
                    onRequestPermission = {
                        promptTracker.recordInteraction()
                        permissionController.requestPermission()
                    },
                    onOpenSettings = {
                        promptTracker.recordInteraction()
                        permissionController.openSettings()
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        } else {
            null
        }

        AgendaRoute(
            viewModel = viewModel,
            onEventClick = onEventClick,
            onTaskClick = onTaskClick,
            notificationPermissionCard = notificationCard
        )
    }
}

@VisibleForTesting
@Composable
internal fun NotificationPermissionCard(
    controller: NotificationPermissionController,
    onDismiss: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "알림 권한을 허용해 주세요",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (controller.canRequest) {
                    "리마인더를 켜 두면 일정이나 할 일을 추가할 때 시작 전에 알림을 받을 수 있어요."
                } else {
                    "앱 설정에서 알림을 허용하면 일정 전에 리마인더를 받을 수 있어요."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = "나중에")
                }
                Spacer(modifier = Modifier.weight(1f))
                val actionLabel = if (controller.canRequest) "알림 허용" else "설정 열기"
                Button(
                    onClick = {
                        if (controller.canRequest) {
                            onRequestPermission()
                        } else {
                            onOpenSettings()
                        }
                    }
                ) {
                    Text(text = actionLabel)
                }
            }
        }
    }
}

@Composable
private fun rememberNotificationPermissionController(): NotificationPermissionController {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    var hasRequested by rememberSaveable { mutableStateOf(false) }
    var isGranted by remember { mutableStateOf(hasNotificationPermission(context)) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasRequested = true
        isGranted = granted || !isSupported
    }

    DisposableEffect(lifecycleOwner, context, isSupported) {
        if (!isSupported) {
            return@DisposableEffect onDispose {}
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGranted = hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(context, isSupported) {
        if (isSupported) {
            isGranted = hasNotificationPermission(context)
        }
    }

    val activity = remember(context) { context.findActivity() }
    val canRequest = isSupported && !isGranted && (!hasRequested || activity?.let {
        ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.POST_NOTIFICATIONS)
    } == true)

    return NotificationPermissionController(
        isSupported = isSupported,
        isGranted = isGranted,
        canRequest = canRequest,
        requestPermission = {
            if (isSupported && !isGranted) {
                hasRequested = true
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        openSettings = {
            if (!isGranted && isSupported) {
                openNotificationSettings(context)
            }
        }
    )
}

@VisibleForTesting
internal data class NotificationPermissionController(
    val isSupported: Boolean,
    val isGranted: Boolean,
    val canRequest: Boolean,
    val requestPermission: () -> Unit,
    val openSettings: () -> Unit
) {
    val shouldShowPrompt: Boolean
        get() = isSupported && !isGranted
}

private fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return true
    }
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
