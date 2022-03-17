package com.MennoSpijker.kentekenscanner.View

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.MennoSpijker.kentekenscanner.ConnectionDetector
import com.MennoSpijker.kentekenscanner.Factory.KentekenDataFactory
import com.MennoSpijker.kentekenscanner.R
import com.MennoSpijker.kentekenscanner.Util.FileHandling
import com.google.firebase.analytics.FirebaseAnalytics
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class KentekenHandler(
    private val context: MainActivity,
    private val connection: ConnectionDetector,
    private val result: ScrollView,
    private val kentekenHolder: TextView
) {
    private val kentekenDataFactory = KentekenDataFactory()
    private val bundle: Bundle
    var previousSearchedKenteken = ""
    fun run(textview: TextView) {
        val kenteken = textview.text.toString().toUpperCase(Locale.ROOT)
        runCamera(kenteken, textview)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun runCamera(kenteken: String, textview: TextView) {
        var kenteken = kenteken
        val progressBar = context.findViewById<ProgressBar>(R.id.progressBar)
        progressBar.progress = 10
        progressBar.visibility = View.VISIBLE
        Log.d(TAG, "runCamera: $progressBar")
        if (previousSearchedKenteken == kenteken) {
            Log.d(TAG, "runCamera: Kenteken run twice, returning...")
            return
        }
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, formatLicenseplate(kenteken))
        context.firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SEARCH, bundle)
        previousSearchedKenteken = kenteken
        progressBar.progress = 15
        try {
            kenteken = kenteken.replace("-", "")
            kenteken = kenteken.replace(" ", "")
            kenteken = kenteken.replace("\n", "")
            if (kenteken.length > 0) {
                kentekenDataFactory.emptyArray()
                textview.text = formatLicenseplate(kenteken)
                result.removeAllViews()
                val uri = "https://kenteken-scanner.nl/api/kenteken/$kenteken"
                val runner = Async(
                    context,
                    kenteken,
                    result,
                    uri,
                    connection,
                    this,
                    kentekenDataFactory,
                    progressBar
                )
                runner.execute()
            } else {
                progressBar.visibility = View.GONE
            }
        } catch (e: Exception) {
            e.printStackTrace()
            e.message
            progressBar.visibility = View.GONE
        }
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
        previousSearchedKenteken = ""
    }

    fun saveRecentKenteken(kenteken: String?) {
        val otherKentekens = FileHandling(context).recentKenteken
        val wantedFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
        val date = wantedFormat.format(Date())
        FileHandling(context).writeToFileOnDate(
            RecentKentekensFile,
            kenteken!!,
            date,
            otherKentekens
        )
    }

    fun saveFavoriteKenteken(kenteken: String?) {
        val otherKentekens = FileHandling(context).savedKentekens
        context.firebaseAnalytics.setUserProperty("kenteken", kenteken)
        FileHandling(context).writeToFile(SavedKentekensFile, kenteken!!, otherKentekens)
    }

    @Throws(JSONException::class)
    fun deleteFavoriteKenteken(kenteken: String) {
        val otherKentekens = FileHandling(context).savedKentekens
        context.firebaseAnalytics.setUserProperty("kenteken", kenteken)
        val temp = JSONObject()
        temp.put("cars", JSONArray())
        for (i in 0 until otherKentekens.getJSONArray("cars").length()) {
            if (otherKentekens.getJSONArray("cars").getString(i) != kenteken) {
                temp.getJSONArray("cars").put(otherKentekens.getJSONArray("cars").getString(i))
            }
        }
        Log.d(TAG, "deleteFavoriteKenteken: $temp")
        FileHandling(context).writeToFile(SavedKentekensFile, temp)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun openRecent() {
        kentekenHolder.text = ""
        kentekenHolder.clearFocus()
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
        result.removeAllViews()
        val scale = context.resources.displayMetrics.density
        val width = (283 * scale + 0.5f).toInt()
        val height = (75 * scale + 0.5f).toInt()
        try {
            val recents = FileHandling(context).recentKenteken
            val lin = LinearLayout(context)
            lin.orientation = LinearLayout.VERTICAL
            val iterator = recents.keys()
            while (iterator.hasNext()) {
                val key = iterator.next()
                val values = recents.getJSONArray(key)
                val dateView = TextView(context)
                dateView.text = key
                dateView.textAlignment = View.TEXT_ALIGNMENT_CENTER
                lin.addView(dateView)
                for (i in 0 until values.length()) {
                    var recent = values.getString(i)
                    recent = recent.replace("/", "")
                    val line = Button(context)
                    line.text = formatLicenseplate(recent)
                    val finalRecent = recent
                    line.textAlignment = View.TEXT_ALIGNMENT_CENTER
                    line.setOnClickListener { v: View? -> runCamera(finalRecent, kentekenHolder) }
                    line.background = context.getDrawable(R.drawable.kentekenplaat3)
                    val params = LinearLayout.LayoutParams(
                        width,
                        height
                    )
                    params.setMargins(0, 10, 0, 10)
                    params.gravity = 17
                    line.layoutParams = params
                    line.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
                    line.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                    val left = (45 * scale + 0.5f).toInt()
                    val right = (10 * scale + 0.5f).toInt()
                    val top = (0 * scale + 0.5f).toInt()
                    val bottom = (0 * scale + 0.5f).toInt()
                    line.setPadding(left, top, right, bottom)
                    lin.addView(line)
                }
            }
            result.addView(lin)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun openSaved() {
        kentekenHolder.text = ""
        kentekenHolder.clearFocus()
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
        result.removeAllViews()
        val scale = context.resources.displayMetrics.density
        val width = (283 * scale + 0.5f).toInt()
        val height = (75 * scale + 0.5f).toInt()
        try {
            val recents = FileHandling(context).savedKentekens
            val lin = LinearLayout(context)
            lin.orientation = LinearLayout.VERTICAL
            val iterator = recents.keys()
            val textView = TextView(context)
            textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
            textView.setText(R.string.eigen_auto)
            println(recents.names())
            lin.addView(textView)
            while (iterator.hasNext()) {
                val key = iterator.next()
                val values = recents.getJSONArray(key)
                for (i in 0 until values.length()) {
                    var recent = values.getString(i)
                    recent = recent.replace("/", "")
                    val line = Button(context)
                    line.text = formatLicenseplate(recent)
                    val finalRecent = recent
                    line.textAlignment = View.TEXT_ALIGNMENT_CENTER
                    line.setOnClickListener { v: View? -> runCamera(finalRecent, kentekenHolder) }
                    line.background = context.getDrawable(R.drawable.kentekenplaat3)
                    val params = LinearLayout.LayoutParams(
                        width,
                        height
                    )
                    params.setMargins(0, 10, 0, 10)
                    params.gravity = 17
                    line.layoutParams = params
                    line.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
                    line.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                    val left = (45 * scale + 0.5f).toInt()
                    val right = (10 * scale + 0.5f).toInt()
                    val top = (0 * scale + 0.5f).toInt()
                    val bottom = (0 * scale + 0.5f).toInt()
                    line.setPadding(left, top, right, bottom)
                    lin.addView(line)
                }
            }
            result.addView(lin)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun openNotifications() {
        kentekenHolder.text = ""
        kentekenHolder.clearFocus()

        // Hide keyboard
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
        result.removeAllViews()
        val scale = context.resources.displayMetrics.density
        val width = (283 * scale + 0.5f).toInt()
        val height = (75 * scale + 0.5f).toInt()
        try {
            val pendingNotifications = FileHandling(context).pendingNotifications
            val linearLayout = LinearLayout(context)
            linearLayout.orientation = LinearLayout.VERTICAL
            val iterator = pendingNotifications.keys()
            while (iterator.hasNext()) {
                val key = iterator.next()
                val values = pendingNotifications.getJSONArray(key)
                for (i in 0 until values.length()) {
                    val notification = values.getJSONObject(i)
                    val kenteken = notification.getString("kenteken")
                    Log.d(TAG, "openNotifications: $kenteken")
                    val dateView = TextView(context)
                    dateView.text = notification.getString("notificationDate")
                    dateView.textAlignment = View.TEXT_ALIGNMENT_CENTER
                    linearLayout.addView(dateView)
                    val button = Button(context)
                    button.text = formatLicenseplate(kenteken)
                    button.textAlignment = View.TEXT_ALIGNMENT_CENTER
                    button.setOnClickListener { v: View? -> runCamera(kenteken, kentekenHolder) }
                    button.background = context.getDrawable(R.drawable.kentekenplaat3)
                    val params = LinearLayout.LayoutParams(
                        width,
                        height
                    )
                    params.setMargins(0, 10, 0, 10)
                    params.gravity = 17
                    button.layoutParams = params
                    button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
                    button.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                    val left = (45 * scale + 0.5f).toInt()
                    val right = (10 * scale + 0.5f).toInt()
                    val top = (0 * scale + 0.5f).toInt()
                    val bottom = (0 * scale + 0.5f).toInt()
                    button.setPadding(left, top, right, bottom)
                    linearLayout.addView(button)
                }
            }
            result.addView(linearLayout)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val RecentKentekensFile = "recent.json"
        private const val SavedKentekensFile = "favorites.json"
        private const val TAG = "KentekenHandler"
        @JvmStatic
        fun formatLicenseplate(alicenseplate: String): String {
            try {
                val sidecode = getSidecodeLicenseplate(alicenseplate)
                val licenseplate = alicenseplate.replace("-", "").toUpperCase(Locale.ROOT)
                if (sidecode == -2) {
                    return alicenseplate
                }
                if (sidecode <= 6) {
                    println("sidecode 6")
                    return licenseplate.substring(0, 2) + '-' + licenseplate.substring(
                        2,
                        4
                    ) + '-' + licenseplate.substring(4, 6)
                }
                if (sidecode == 7 || sidecode == 9) {
                    println("sidecode 7")
                    return licenseplate.substring(0, 2) + '-' + licenseplate.substring(
                        2,
                        5
                    ) + '-' + licenseplate[5]
                }
                if (sidecode == 8 || sidecode == 10) {
                    println("sidecode 8")
                    return licenseplate.substring(0, 1) + '-' + licenseplate.substring(
                        1,
                        4
                    ) + '-' + licenseplate.substring(4, 6)
                }
                if (sidecode == 11 || sidecode == 14) {
                    println("sidecode 11")
                    return licenseplate.substring(0, 3) + '-' + licenseplate.substring(
                        3,
                        5
                    ) + '-' + licenseplate[5]
                }
                if (sidecode == 12 || sidecode == 13) {
                    println("sidecode 12")
                    return licenseplate.substring(0, 1) + '-' + licenseplate.substring(
                        1,
                        3
                    ) + '-' + licenseplate.substring(3, 6)
                }
                return alicenseplate
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return alicenseplate
        }

        @JvmStatic
        fun getSidecodeLicenseplate(licenseplate: String): Int {
            var licenseplate = licenseplate
            val patterns = arrayOfNulls<String>(14)
            licenseplate = licenseplate.replace("-", "").toUpperCase(Locale.ROOT)
            patterns[0] = "^[a-zA-Z]{2}[0-9]{2}[0-9]{2}$" // 1 XX-99-99
            patterns[1] = "^[0-9]{2}[0-9]{2}[a-zA-Z]{2}$" // 2 99-99-XX
            patterns[2] = "^[0-9]{2}[a-zA-Z]{2}[0-9]{2}$" // 3 99-XX-99
            patterns[3] = "^[a-zA-Z]{2}[0-9]{2}[a-zA-Z]{2}$" // 4 XX-99-XX
            patterns[4] = "^[a-zA-Z]{2}[a-zA-Z]{2}[0-9]{2}$" // 5 XX-XX-99
            patterns[5] = "^[0-9]{2}[a-zA-Z]{2}[a-zA-Z]{2}$" // 6 99-XX-XX
            patterns[6] = "^[0-9]{2}[a-zA-Z]{3}[0-9]{1}$" // 7 99-XXX-9
            patterns[7] = "^[0-9]{1}[a-zA-Z]{3}[0-9]{2}$" // 8 9-XXX-99
            patterns[8] = "^[a-zA-Z]{2}[0-9]{3}[a-zA-Z]{1}$" // 9 XX-999-X
            patterns[9] = "^[a-zA-Z]{1}[0-9]{3}[a-zA-Z]{2}$" // 10 X-999-XX
            patterns[10] = "^[a-zA-Z]{3}[0-9]{2}[a-zA-Z]{1}$" // 11 XXX-99-X
            patterns[11] = "^[a-zA-Z]{1}[0-9]{2}[a-zA-Z]{3}$" // 12 X-99-XXX
            patterns[12] = "^[0-9]{1}[a-zA-Z]{2}[0-9]{3}$" // 13 9-XX-999
            patterns[13] = "^[0-9]{3}[a-zA-Z]{2}[0-9]{1}$" // 14 999-XX-9

            //except licenseplates for diplomats
            val diplomat: Regex = Regex("^CD[ABFJNST][0-9]{1,3}$") //for example: CDB1 of CDJ45
            for (i in patterns.indices) {
                if (licenseplate.matches(Regex(patterns[i]!!))) {
                    return i + 1
                }
            }
            return if (licenseplate.matches(diplomat)) {
                -1
            } else -2
        }

        @JvmStatic
        fun kentekenValid(s: String): Boolean {
            return getSidecodeLicenseplate(s) >= -1
        }
    }

    init {
        bundle = Bundle()
        Log.d(TAG, "KentekenHandler: " + kentekenHolder)
        Log.d(TAG, "KentekenHandler: $kentekenHolder")
    }
}