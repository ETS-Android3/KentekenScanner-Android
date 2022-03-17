package com.MennoSpijker.kentekenscanner.Factory

import kotlin.Throws
import com.MennoSpijker.kentekenscanner.Factory.NotificationFactory
import java.time.LocalDateTime
import java.time.Instant
import org.json.JSONObject
import com.MennoSpijker.kentekenscanner.Util.FileHandling
import androidx.work.OneTimeWorkRequest
import com.MennoSpijker.kentekenscanner.java.CustomNotification
import androidx.work.WorkManager
import com.MennoSpijker.kentekenscanner.R
import android.app.NotificationManager
import android.app.NotificationChannel
import android.content.Context
import android.util.Log
import androidx.work.Data
import java.lang.Exception
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NotificationFactory(context: Context) {
    private val context: Context
    @Throws(ParseException::class)
    fun getDate(dateString: String?): Date {
        return SimpleDateFormat("dd-MM-yy", Locale.GERMANY).parse(dateString)
    }

    fun calculateNotifcationTime(dateString: String?): Long {
        try {
            val date = getDate(dateString)
            Log.d(TAG, "onCreate: $date")
            val c = Calendar.getInstance()
            c.timeZone = TimeZone.getTimeZone("Europe/Amsterdam")
            c.time = date
            c.add(Calendar.DAY_OF_YEAR, -30)
            c.add(Calendar.HOUR_OF_DAY, 12)
            c.add(Calendar.MINUTE, 0)
            Log.d(TAG, "onCreate: " + System.currentTimeMillis())
            Log.d(TAG, "onCreate: " + c.timeInMillis)
            val currentTime = System.currentTimeMillis()
            val specificTimeToTrigger = c.timeInMillis
            val delayToPass = specificTimeToTrigger - currentTime
            Log.d(TAG, "onCreate: $delayToPass")
            return delayToPass
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return -1
    }

    fun planNotification(
        notificationTitle: String?,
        notificationText: String?,
        kenteken: String?,
        notificationDate: Long
    ) {
        try {
            val notificationUUID = (Math.random() * 1000).toInt() + 1000
            val data = Data.Builder()
            data.putString("title", notificationTitle)
            data.putString("text", notificationText)
            data.putInt("uuid", notificationUUID)
            data.putString("kenteken", kenteken)
            Log.d(TAG, "planNotification: $notificationDate")
            val localDateTime = Instant.ofEpochMilli(System.currentTimeMillis() + notificationDate)
                .atZone(
                    TimeZone.getTimeZone("Europe/Amsterdam")
                        .toZoneId()
                ).toLocalDateTime()
            Log.d(TAG, "planNotification: $localDateTime")
            var notficationDateString = localDateTime.dayOfMonth.toString() + " "
            notficationDateString += (localDateTime.month.toString() + " ").toLowerCase()
            notficationDateString += localDateTime.year

            // TODO add notification to file
            val notificationObject = JSONObject()
            notificationObject.put("title", notificationTitle)
            notificationObject.put("text", notificationText)
            notificationObject.put("kenteken", kenteken)
            notificationObject.put("notificationDate", notficationDateString)
            notificationObject.put(
                "notificationDateNoFormat",
                localDateTime.dayOfMonth.toString() + "-" + localDateTime.monthValue + "-" + localDateTime.year
            )
            notificationObject.put("UUID", notificationUUID)
            FileHandling(context).addNotificationToFile(notificationObject)
            val compressionWork = OneTimeWorkRequest.Builder(
                CustomNotification::class.java
            )
                .setInitialDelay(notificationDate, TimeUnit.MILLISECONDS)
                .setInputData(data.build())
                .build()
            WorkManager.getInstance().enqueue(compressionWork)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        val name: CharSequence = context.getString(R.string.channel_name)
        val description = context.getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance)
        channel.description = description
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        val notificationManager = context.getSystemService(
            NotificationManager::class.java
        )
        notificationManager.createNotificationChannel(channel)
        Log.d(TAG, "createNotificationChannel: Notification channel created...")
    }

    companion object {
        private const val TAG = "NotificationFactory"
        const val CHANNEL_ID = "KentekenScanner"
    }

    init {
        Log.d(TAG, "NotificationFactory: New notifcationFactory generated")
        this.context = context
        createNotificationChannel()
    }
}