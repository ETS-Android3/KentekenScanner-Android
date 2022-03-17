package com.MennoSpijker.kentekenscanner.java

import androidx.work.WorkerParameters
import androidx.work.Worker
import androidx.work.ListenableWorker
import android.content.Intent
import com.MennoSpijker.kentekenscanner.View.MainActivity
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import com.MennoSpijker.kentekenscanner.Factory.NotificationFactory
import com.MennoSpijker.kentekenscanner.R
import androidx.core.app.NotificationManagerCompat

class CustomNotification(private val context: Context, workerParams: WorkerParameters) : Worker(
    context, workerParams
) {
    override fun doWork(): Result {
        createNotification()
        return Result.success()
    }

    private fun createNotification() {
        val title = inputData.getString("title")
        val text = inputData.getString("text")
        val kenteken = inputData.getString("kenteken")
        val uuid = inputData.getInt("uuid", 1)

        // Create an explicit intent for an Activity in your app
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra("kenteken", kenteken)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        @SuppressLint("UnspecifiedImmutableFlag") val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, 0
        )
        val builder = NotificationCompat.Builder(context, NotificationFactory.CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_180)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(text)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        val notificationManager = NotificationManagerCompat.from(
            context
        )
        notificationManager.notify(uuid, builder.build())
    }
}