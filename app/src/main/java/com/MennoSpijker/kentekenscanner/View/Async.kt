package com.MennoSpijker.kentekenscanner.View

import android.annotation.SuppressLint
import android.os.AsyncTask
import android.util.Log
import android.widget.ProgressBar
import android.widget.ScrollView
import com.MennoSpijker.kentekenscanner.APIHelper
import com.MennoSpijker.kentekenscanner.ConnectionDetector
import com.MennoSpijker.kentekenscanner.Factory.KentekenDataFactory
import kotlin.math.log

/**
 * Created by Menno on 08/12/2017.
 */
class Async(
    mainActivity: MainActivity,
    k: String,
    r: ScrollView,
    u: String,
    c: ConnectionDetector,
    kh: KentekenHandler,
    kdf: KentekenDataFactory,
    progressBar: ProgressBar
) : AsyncTask<String, String, String>() {
    private lateinit var kenteken: String
    private lateinit var uri: String
    private lateinit var resultView: ScrollView
    private lateinit var mainActivity: MainActivity
    private lateinit var connection: ConnectionDetector
    private lateinit var resp: String
    private lateinit var progressBar: ProgressBar

    lateinit var Khandler: KentekenHandler
    lateinit var kentekenDataFactory: KentekenDataFactory

    override fun doInBackground(vararg p0: String): String {
        progressBar.progress = 60
        try {
            if (kenteken.isNotEmpty()) {
                val response = APIHelper(connection, uri)
                resp = response.run(kenteken)!!
                Log.d(TAG, "doInBackground: $kenteken")
                Log.d(TAG, "doInBackground: $resp")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return resp
    }

    override fun onPostExecute(result: String) {
        Log.d(TAG, "onPostExecute: $mainActivity, $resultView, $kenteken, $Khandler")
        kentekenDataFactory.addParams(mainActivity, resultView, kenteken, Khandler)
        Log.d(TAG, "onPostExecute: $result")
        kentekenDataFactory.fillArray(result, progressBar)
    }

    override fun onPreExecute() {}
    override fun onProgressUpdate(vararg values: String) {}

    companion object {
        private const val TAG = "Async"
    }

    init {
        try {
            this.mainActivity = mainActivity
            kenteken = k
            resultView = r
            uri = u
            connection = c
            Khandler = kh
            kentekenDataFactory = kdf
            this.progressBar = progressBar

            Log.d(TAG, "init?: $k")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}