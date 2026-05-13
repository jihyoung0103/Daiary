package com.smu.daiary.feature.notification

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.smu.daiary.MainActivity
import com.smu.daiary.R

class DiaryNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("daiary_settings", Context.MODE_PRIVATE)

        // 부팅 완료 시 → 다음 알림 재예약만 수행
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (prefs.getBoolean("notification_enabled", true)) {
                scheduleNotification(
                    context,
                    prefs.getInt("notification_hour", 21),
                    prefs.getInt("notification_minute", 0)
                )
            }
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val tapPendingIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = androidx.core.app.NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_body))
            .setContentIntent(tapPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, notification)

        // 다음 날 동일 시각으로 재예약
        if (prefs.getBoolean("notification_enabled", true)) {
            scheduleNotification(
                context,
                prefs.getInt("notification_hour", 21),
                prefs.getInt("notification_minute", 0)
            )
        }
    }
}
