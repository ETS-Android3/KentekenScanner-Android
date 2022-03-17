package com.MennoSpijker.kentekenscanner.java

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.MennoSpijker.kentekenscanner.java.MyFirebaseMessagingService

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
    }

    companion object {
        private const val TAG = "MyFirebaseMessagingService"
    }
}