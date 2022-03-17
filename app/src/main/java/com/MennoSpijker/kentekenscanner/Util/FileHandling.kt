package com.MennoSpijker.kentekenscanner.Util

import android.content.Context
import android.util.Log
import java.lang.StringBuilder
import com.MennoSpijker.kentekenscanner.Util.FileHandling
import org.json.JSONObject
import org.json.JSONArray
import org.json.JSONException
import java.io.*
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class FileHandling(context: Context) {
    private val storageDir: String
    fun readFile(filename: String): String? {
        val fis: FileInputStream
        var n: Int
        val fileContent = StringBuilder()
        val file = File(storageDir + filename)
        println("FILENAME: $file")
        if (file.exists()) {
            Log.d(TAG, "readFile: File exists.")
            try {
                fis = FileInputStream(file)
                val buffer = ByteArray(1024)
                try {
                    while (fis.read(buffer).also { n = it } != -1) {
                        fileContent.append(String(buffer, 0, n))
                    }
                } catch (IE: IOException) {
                    IE.printStackTrace()
                }
            } catch (FNF: FileNotFoundException) {
                FNF.printStackTrace()
            }
            Log.d(TAG, "readFile() returned: $fileContent")
            return fileContent.toString()
        } else {
            Log.d(TAG, "readFile: File doesn't exist, try to create")
            try {
                file.createNewFile()
                writeToFile(filename, JSONObject())
                return readFile(filename)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return null
    }

    fun writeToFileOnDate(
        filename: String,
        newKenteken: String,
        newKentekenDate: String,
        otherKentekens: JSONObject
    ) {
        val mainObject = JSONObject()
        val file = File(storageDir + filename)
        if (file.exists()) {
            try {
                val fileOutputStream = FileOutputStream(file)

                // defining previous saved kentekens
                println("other kentekens: $otherKentekens")
                val previousSavedData = JSONArray().put(otherKentekens)
                val amountOfDates = previousSavedData.getJSONObject(0).length()

                // Making sure no kentekens are double saved.
                var proceed = 1
                var dateChecked = false
                if (amountOfDates > 0) {
                    val iterator = otherKentekens.keys()
                    while (iterator.hasNext()) {
                        val key = iterator.next()
                        val values = JSONArray(otherKentekens.getString(key))
                        println("main object at begin:$mainObject")
                        println(values)
                        println(values.length())
                        if (key == newKentekenDate) {
                            dateChecked = true
                            for (i in 0 until values.length()) {
                                val value = values.getString(i)
                                val currentDateKentekens = JSONArray()
                                val currentDateKentekensNew = JSONArray()
                                val s = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                                val currentDate = s.parse(key).toString()

                                // retrieve dates saved
                                val kentekensSavedOnDate = otherKentekens.getJSONArray(key)
                                val amountOfKentekens = kentekensSavedOnDate.length()

                                // retrieve kentekens saved on certain date
                                for (j in 0 until amountOfKentekens) {
                                    val previousKenteken = kentekensSavedOnDate.getString(j)
                                    if (previousKenteken == newKenteken) {
                                        proceed = 0
                                    }
                                    currentDateKentekens.put(previousKenteken)
                                }
                                if (proceed == 1) {
                                    currentDateKentekensNew.put(newKenteken)
                                }
                                for (x in 0 until currentDateKentekens.length()) {
                                    currentDateKentekensNew.put(currentDateKentekens.getString(x))
                                }
                                mainObject.put(key, currentDateKentekensNew)
                            }
                        } else {
                            if (!dateChecked) {
                                mainObject.put(newKentekenDate, JSONArray().put(newKenteken))
                            }
                            mainObject.put(key, otherKentekens.getJSONArray(key))
                        }
                    }
                } else {
                    println("No dates saved.")
                    mainObject.put(newKentekenDate, JSONArray().put(newKenteken))
                }
                println("before write: $mainObject")
                fileOutputStream.write(mainObject.toString().toByteArray())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                println(file.createNewFile())
                writeToFileOnDate(filename, newKenteken, newKentekenDate, otherKentekens)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun writeToFile(filename: String, newKenteken: String, otherKentekens: JSONObject) {
        val mainObject = JSONObject()
        val file = File(storageDir + filename)
        if (file.exists()) {
            try {
                val fileOutputStream = FileOutputStream(file)

                // Making sure no kentekens are double saved.
                var proceed = 1
                val dateChecked = false
                val key = "cars"
                if (otherKentekens.length() > 0) {
                    val values = JSONArray(otherKentekens.getString(key))
                    if (values.length() == 0) {
                        println("add new one")
                    }
                    for (i in 0 until values.length()) {
                        val value = values.getString(i)
                        val currentDateKentekens = JSONArray()
                        val currentDateKentekensNew = JSONArray()

                        // retrieve dates saved
                        val kentekensSavedOnDate = otherKentekens.getJSONArray(key)
                        val amountOfKentekens = kentekensSavedOnDate.length()

                        // retrieve kentekens saved on certain date
                        for (j in 0 until amountOfKentekens) {
                            val previousKenteken = kentekensSavedOnDate.getString(j)
                            if (previousKenteken == newKenteken) {
                                proceed = 0
                            }
                            currentDateKentekens.put(previousKenteken)
                        }
                        if (proceed == 1) {
                            currentDateKentekensNew.put(newKenteken)
                        }
                        for (x in 0 until currentDateKentekens.length()) {
                            currentDateKentekensNew.put(currentDateKentekens.getString(x))
                        }
                        mainObject.put(key, currentDateKentekensNew)
                    }
                } else {
                    mainObject.put(key, JSONArray().put(newKenteken))
                }
                println("before write: $mainObject")
                fileOutputStream.write(mainObject.toString().toByteArray())
            } catch (e: JSONException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            try {
                println(file.createNewFile())
                writeToFile(filename, newKenteken, otherKentekens)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun emptyFile(filename: String) {
        val empty = ""
        val file = File(storageDir + filename)
        if (file.exists()) {
            try {
                val fOut = FileOutputStream(file)
                try {
                    fOut.write(empty.toByteArray())
                    fOut.close()
                } catch (IE: IOException) {
                    IE.printStackTrace()
                }
            } catch (FNFE: FileNotFoundException) {
                FNFE.printStackTrace()
            }
        }
    }

    //return kentekens;
    val savedKentekens: JSONObject
        get() {
            val kentekens = ArrayList<JSONArray>()
            val fileContent = readFile(SavedKentekensFile)
            var mainObject = JSONObject()
            try {
                mainObject = JSONObject(fileContent)
            } catch (e: JSONException) {
                println("error empty mainObject")
            }

            //return kentekens;
            return mainObject
        }

    //return notifications;
    val pendingNotifications: JSONObject
        get() {
            val notifications = ArrayList<JSONArray>()
            val fileContent = readFile(NotificationsFile)
            var mainObject = JSONObject()
            try {
                mainObject = JSONObject(fileContent)
            } catch (e: JSONException) {
                println("error empty mainObject")
            }
            Log.d(TAG, "getPendingNotifications: $mainObject")

            //return notifications;
            return mainObject
        }

    fun saveNotificationsToFile(notifications: JSONArray?) {
        val mainObject = JSONObject()
        val file = File(storageDir + NotificationsFile)
        if (file.exists()) {
            try {
                val fileOutputStream = FileOutputStream(file)
                val key = "notifications"
                mainObject.put(key, notifications)
                fileOutputStream.write(mainObject.toString().toByteArray())
            } catch (e: JSONException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            // generate file and re-call this method
            try {
                println(file.createNewFile())
                saveNotificationsToFile(notifications)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    //return kentekens;
    val recentKenteken: JSONObject
        get() {
            val kentekens = ArrayList<JSONArray>()
            val fileContent = readFile(RecentKentekensFile)
            var mainObject = JSONObject()
            try {
                mainObject = JSONObject(fileContent)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            //return kentekens;
            return mainObject
        }

    fun writeToFile(savedKentekensFile: String, otherKentekens: JSONObject) {
        val mainObject = JSONObject()
        val file = File(storageDir + savedKentekensFile)
        if (file.exists()) {
            try {
                val fileOutputStream = FileOutputStream(file)
                val key = "cars"
                if (otherKentekens.length() > 0) {
                    val values = JSONArray(otherKentekens.getString(key))
                    mainObject.put(key, otherKentekens.getJSONArray("cars"))
                }
                println("before write: $mainObject")
                fileOutputStream.write(mainObject.toString().toByteArray())
            } catch (e: JSONException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            try {
                println(file.createNewFile())
                writeToFile(savedKentekensFile, otherKentekens)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun addNotificationToFile(notificationObject: JSONObject) {
        try {
            val notifications = pendingNotifications
            if (notifications.length() > 0) {
                val notificationsArray = notifications.getJSONArray("notifications")
                if (!doesNotificationExist(notificationObject.getString("kenteken"))) {
                    notificationsArray.put(notificationObject)
                    saveNotificationsToFile(notificationsArray)
                } else {
                    Log.d(
                        TAG,
                        "addNotificationToFile: Notification with this kenteken already saved."
                    )
                }
                Log.d(TAG, "addNotificationToFile: $notifications")
            } else {
                saveNotificationsToFile(JSONArray().put(notificationObject))
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun doesNotificationExist(kenteken: String): Boolean {
        var doesExistInArray = false
        val notifications = pendingNotifications
        Log.d(TAG, "doesNotificationExist: $notifications")
        Log.d(TAG, "doesNotificationExist: " + notifications.length())
        if (notifications.length() == 0) {
            return false
        }
        try {
            val notificationsArray = notifications.getJSONArray("notifications")
            for (i in 0 until notificationsArray.length()) {
                if (notificationsArray.getJSONObject(i).getString("kenteken") == kenteken) {
                    doesExistInArray = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return doesExistInArray
    }

    fun cleanUpNotificationList() {
        val pendingNotifications = pendingNotifications
        if (pendingNotifications.length() == 0) {
            return
        }
        try {
            val notificationsArray = pendingNotifications.getJSONArray("notifications")
            var changedArray = false
            for (i in 0 until notificationsArray.length()) {
                val notificationDate =
                    notificationsArray.getJSONObject(i).getString("notificationDateNoFormat")
                val date = SimpleDateFormat("dd-MM-yy", Locale.GERMANY).parse(notificationDate)
                if (date.before(Date())) {
                    Log.d(TAG, "cleanUpNotificationList: Date passed")
                    notificationsArray.remove(i)
                    changedArray = true
                } else {
                    Log.d(TAG, "cleanUpNotificationList: Date not passed")
                }
                if (changedArray) {
                    saveNotificationsToFile(notificationsArray)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
    }

    companion object {
        private const val TAG = "PERMISSION"
        private const val RecentKentekensFile = "recent.json"
        private const val NotificationsFile = "notifications.json"
        private const val SavedKentekensFile = "favorites.json"
    }

    init {
        storageDir = context.filesDir.toString() + "/"
    }
}