package com.MennoSpijker.kentekenscanner

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import java.lang.Exception

/**
 * Created by Menno Spijker on 08/12/2017.
 */
class ConnectionDetector(private val _context: Context) {
    val isConnectingToInternet: Boolean
        get() {
            try {
                val connectivity =
                    _context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                if (connectivity != null) {
                    val info = connectivity.activeNetworkInfo
                    if (info != null) {
                        if (info.state == NetworkInfo.State.CONNECTED) {
                            return true
                        }
                    }
                }
            } catch (E: Exception) {
                E.printStackTrace()
            }
            return false
        }
}