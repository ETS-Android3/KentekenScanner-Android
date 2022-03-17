package com.MennoSpijker.kentekenscanner.Factory

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import com.MennoSpijker.kentekenscanner.R
import com.MennoSpijker.kentekenscanner.Util.FileHandling
import com.MennoSpijker.kentekenscanner.View.KentekenHandler
import com.MennoSpijker.kentekenscanner.View.MainActivity
import com.google.firebase.analytics.FirebaseAnalytics
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class KentekenDataFactory {
    private var array = JSONArray()
    private var kenteken: String = ""

    private var context: MainActivity? = null

    private var kentekenHandler: KentekenHandler? = null
    private var resultView: ScrollView? = null
    private var notificationFactory: NotificationFactory? = null

    fun emptyArray() {
        array = JSONArray()
        array.put(JSONObject())
    }

    fun addParams(
        context: MainActivity,
        resultView: ScrollView,
        kenteken: String,
        Khandler: KentekenHandler
    ) {
        if (this.kenteken != kenteken) {
            this.context = context
            this.resultView = resultView
            this.kenteken = kenteken
            kentekenHandler = Khandler
            kentekenHandler!!.saveRecentKenteken(kenteken)
            notificationFactory = NotificationFactory(context)
        }
    }

    fun fillArray(kentekenDataFromAPI: String, progressBar: ProgressBar) {
        progressBar.progress = 50
        try {
            val APIResult = JSONArray(kentekenDataFromAPI).getJSONObject(0)
            if (APIResult == null) {
                showErrorMessage()
                progressBar.visibility = View.GONE
                return
            }
            if (kentekenDataFromAPI.length > 3 && APIResult.length() > 0) {
                progressBar.progress = 77
                try {
                    val array1 = JSONArray(kentekenDataFromAPI)
                    val `object` = array1.getJSONObject(0)
                    val iterator = `object`.keys()
                    while (iterator.hasNext()) {
                        progressBar.incrementProgressBy(1)
                        val key = iterator.next() as String
                        val value = `object`.getString(key)
                        array.getJSONObject(0).put(key, value)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                fillResultView()
                progressBar.visibility = View.GONE
            } else {
                if (array.getJSONObject(0).length() == 0) {
                    val lin = LinearLayout(context)
                    lin.orientation = LinearLayout.VERTICAL
                    val line = TextView(context)
                    line.setText(R.string.no_result)
                    line.setTextColor(Color.RED)
                    line.textAlignment = View.TEXT_ALIGNMENT_CENTER
                    resultView!!.removeAllViews()
                    resultView!!.addView(line)
                    progressBar.visibility = View.GONE
                }
                progressBar.visibility = View.GONE
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            progressBar.visibility = View.GONE
        }
    }

    fun showErrorMessage() {
        val lin = LinearLayout(context)
        lin.orientation = LinearLayout.VERTICAL
        val line = TextView(context)
        line.setText(R.string.no_result)
        line.setTextColor(Color.RED)
        line.textAlignment = View.TEXT_ALIGNMENT_CENTER
        resultView!!.removeAllViews()
        resultView!!.addView(line)
    }

    fun createSaveButton(): Button {
        val save = Button(context)
        save.setText(R.string.save)
        save.setOnClickListener { v: View? ->
            kentekenHandler!!.saveFavoriteKenteken(kenteken)
            kentekenHandler!!.openSaved()
        }
        val size = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        size.weight = 1f
        save.layoutParams = size
        return save
    }

    fun createRemoveButton(): Button {
        val remove = Button(context)
        remove.setText(R.string.delete)
        remove.setOnClickListener { v: View? ->
            try {
                kentekenHandler!!.deleteFavoriteKenteken(kenteken)
                kentekenHandler!!.openSaved()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        val size = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        size.weight = 1f
        remove.layoutParams = size
        return remove
    }

    fun createNotificationButton(): Button {
        val notify = Button(context)
        notify.setText(R.string.notify)
        notify.setOnClickListener { v: View? ->
            try {
                val notificationText =
                    "Pas op! De APK van jouw voertuig met het kenteken " + KentekenHandler.formatLicenseplate(
                        kenteken
                    ) + " vervalt over 30 dagen. (Heb je de APK al verlengd? Dan kun je dit bericht negeren!)"
                val bundle = Bundle()
                bundle.putString(
                    FirebaseAnalytics.Param.ITEM_NAME,
                    KentekenHandler.formatLicenseplate(kenteken)
                )
                context?.firebaseAnalytics?.logEvent("notification_created", bundle)
                Log.d(TAG, "createNotificationButton: " + array.getJSONObject(0))
                notificationFactory!!.planNotification(
                    context!!.getString(R.string.APK_ALERT),
                    notificationText,
                    kenteken,
                    notificationFactory!!.calculateNotifcationTime(
                        array.getJSONObject(0).getString("vervaldatum_apk")
                    )
                )
                fillResultView()
                Toast.makeText(context, R.string.notifcationActivated, Toast.LENGTH_SHORT).show()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        val size = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        size.weight = 1f
        notify.layoutParams = size
        return notify
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun fillResultView() {
        try {
            resultView!!.removeAllViews()
            resultView!!.visibility = View.VISIBLE
            val linearLayout = LinearLayout(context)
            linearLayout.orientation = LinearLayout.VERTICAL
            val linearLayoutHorizontal = LinearLayout(context)
            linearLayoutHorizontal.orientation = LinearLayout.HORIZONTAL
            var button: Button? = null
            val kentekens = context?.let { FileHandling(it).savedKentekens }
            if (kentekens != null) {
                if (kentekens.length() != 0) {
                    var inArray = false
                    for (i in 0 until kentekens.getJSONArray("cars").length()) {
                        if (kentekens.getJSONArray("cars").getString(i) == kenteken) {
                            inArray = true
                        }
                    }
                    button = if (inArray) {
                        createRemoveButton()
                    } else {
                        createSaveButton()
                    }
                } else {
                    button = createSaveButton()
                }
            }
            if (button != null) {
                linearLayoutHorizontal.addView(button)
            }
            // TODO add notify button
            if (!context?.let { FileHandling(it).doesNotificationExist(kenteken) }!!) {
                linearLayoutHorizontal.addView(createNotificationButton())
            }
            linearLayout.addView(linearLayoutHorizontal)
            val `object` = array.getJSONObject(0)
            val iterator = `object`.keys()
            while (iterator.hasNext()) {
                val key = iterator.next() as String
                if (!key.contains("api")) {
                    val Filtered = key.replace("_", " ")
                    var value = `object`.getString(key)
                    val line = TextView(context)
                    val line2 = TextView(context)
                    val line3 = View(context)
                    if (key == "kenteken") {
                        value = KentekenHandler.formatLicenseplate(value)
                    }
                    if (key == "vervaldatum_apk") {
                        try {
                            val date = SimpleDateFormat("dd-MM-yy", Locale.GERMANY).parse(value)
                            if (date != null && date.before(Date())) {
                                line2.background =
                                    context!!.resources.getDrawable(R.drawable.border_error_item)
                            } else {
                                line2.setBackgroundColor(Color.parseColor("#ffffff"))
                            }
                        } catch (PE: ParseException) {
                            PE.printStackTrace()
                        }
                    }
                    line.text = Filtered
                    line2.text = value
                    line.setTextColor(Color.BLACK)
                    line2.setTextColor(Color.BLACK)
                    line.setTextColor(Color.parseColor("#222222"))
                    line2.setTextColor(Color.parseColor("#222222"))
                    line.visibility = View.VISIBLE
                    line2.visibility = View.VISIBLE
                    line.setPadding(10, 25, 10, 0)
                    line2.setPadding(10, 10, 10, 25)
                    line.textSize = 17f
                    line2.textSize = 15f
                    line.setTypeface(null, Typeface.BOLD)
                    line2.setTypeface(null, Typeface.ITALIC)
                    line.width = 100
                    line2.width = 100
                    line3.setBackgroundColor(Color.parseColor("#aaaaaa"))
                    line3.minimumHeight = 1
                    try {
                        linearLayout.addView(line)
                        linearLayout.addView(line2)
                        linearLayout.addView(line3)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        e.message
                    }
                }
            }
            resultView!!.addView(linearLayout)
        } catch (je: JSONException) {
            je.printStackTrace()
            je.message
            showErrorMessage()
        }
    }

    companion object {
        private const val TAG = "KentekenDataFactory"
    }

    init {
        array.put(JSONObject())
    }
}