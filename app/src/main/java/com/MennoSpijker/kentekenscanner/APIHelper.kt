package com.MennoSpijker.kentekenscanner

import android.util.Log
import com.MennoSpijker.kentekenscanner.ConnectionDetector
import java.net.URL
import com.MennoSpijker.kentekenscanner.APIHelper
import java.net.HttpURLConnection
import java.io.InputStream
import java.io.BufferedInputStream
import java.io.IOException
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.lang.StringBuilder

/**
 * Created by Menno on 08/12/2017.
 */
class APIHelper(private val connection: ConnectionDetector, private val uri: String) {
    fun run(kenteken: String): String? {
        var result: String? = null
        return if (connection.isConnectingToInternet) {
            try {
                if (kenteken != "") {
                    try {
                        val url = URL(uri)
                        Log.d(TAG, url.toString())
                        val urlConnection = url.openConnection() as HttpURLConnection
                        try {
                            val `in`: InputStream = BufferedInputStream(urlConnection.inputStream)
                            result = readStream(`in`)
                        } catch (E: IOException) {
                            E.printStackTrace()
                        } finally {
                            urlConnection.disconnect()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    result = "ERROR"
                }
            } catch (x: Exception) {
                x.printStackTrace()
            }
            result
        } else {
            "No internet connection."
        }
    }

    private fun readStream(`is`: InputStream): String {
        val reader = BufferedReader(InputStreamReader(`is`))
        val sb = StringBuilder()
        var line: String?
        try {
            while (reader.readLine().also { line = it } != null) {
                sb.append(line).append("\n")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                `is`.close()
            } catch (i: IOException) {
                i.printStackTrace()
            }
        }
        return sb.toString()
    }

    companion object {
        private const val TAG = "APIHelper"
    }
}