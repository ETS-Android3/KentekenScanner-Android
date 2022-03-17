package com.MennoSpijker.kentekenscanner.View

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import com.MennoSpijker.kentekenscanner.ConnectionDetector
import com.MennoSpijker.kentekenscanner.FontManager
import com.MennoSpijker.kentekenscanner.OcrCaptureActivity
import com.MennoSpijker.kentekenscanner.R
import com.MennoSpijker.kentekenscanner.Util.FileHandling
import com.MennoSpijker.kentekenscanner.View.KentekenHandler.Companion.formatLicenseplate
import com.MennoSpijker.kentekenscanner.View.KentekenHandler.Companion.getSidecodeLicenseplate
import com.MennoSpijker.kentekenscanner.View.KentekenHandler.Companion.kentekenValid
import com.google.android.gms.ads.*
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.tasks.Task
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.messaging.FirebaseMessaging
import java.util.*
import java.util.function.Consumer

class MainActivity : Activity() {
    private lateinit var bundle: Bundle
    lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var showHistoryButton: Button
    private lateinit var openCameraButton: Button
    private lateinit var showFavoritesButton: Button
    private lateinit var showAlertsButton: Button
    private lateinit var resultScrollView: ScrollView
    private lateinit var Khandler: KentekenHandler

    private var buttons = ArrayList<Button>()

    override fun onCreate(savedInstanceState: Bundle?) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        val iconFont = FontManager.getTypeface(applicationContext, FontManager.FONTAWESOME)
        FontManager.markAsIconContainer(findViewById(R.id.icons_container), iconFont)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
        val kentekenTextField = findViewById<EditText>(R.id.kenteken)
        Log.d(TAG, "onCreate: $kentekenTextField")
        showHistoryButton = findViewById(R.id.showHistory)
        openCameraButton = findViewById(R.id.camera)
        showFavoritesButton = findViewById(R.id.showFavorites)
        showAlertsButton = findViewById(R.id.showAlerts)
        buttons.add(showHistoryButton)
        buttons.add(openCameraButton)
        buttons.add(showFavoritesButton)
        buttons.add(showAlertsButton)
        resultScrollView = findViewById(R.id.scroll)
        val connectionDetector = ConnectionDetector(this)
        Log.d(TAG, "onCreate: $kentekenTextField")
        Khandler = KentekenHandler(
            this@MainActivity,
            connectionDetector,
            resultScrollView,
            kentekenTextField
        )
        buttons.forEach(Consumer { button: Button? ->
            button!!.setTypeface(FontManager.getTypeface(this, FontManager.FONTAWESOME))
            button.textSize = 20f
        })
        showHistoryButton.setOnClickListener { Khandler.openRecent() }
        openCameraButton.setOnClickListener { startCameraIntent() }
        showFavoritesButton.setOnClickListener { Khandler.openSaved() }
        showAlertsButton.setOnClickListener { Khandler.openNotifications() }
    }

    override fun onResume() {
        super.onResume()
        // Must be run on main UI thread...
        ads
    }

    override fun onStart() {
        super.onStart()
        val context: Context = this

        // Run the setup ASYNC for faster first render.
        Thread {

            // Notifications cleanup
            FileHandling(context).cleanUpNotificationList()
            val kentekenTextField = findViewById<EditText>(R.id.kenteken)
            kentekenTextField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    charSequence: CharSequence,
                    i: Int,
                    i1: Int,
                    i2: Int
                ) {
                    Log.d(TAG, "beforeTextChanged: $charSequence")
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    val kenteken = kentekenTextField.text.toString()

                    // check if kenteken is 6 characters long
                    if (kenteken.length == 6) {
                        val formatedKenteken = formatLicenseplate(kenteken)
                        if (kenteken != formatedKenteken) {
                            // Set formatted text in kentekenField
                            kentekenTextField.setText(formatedKenteken)
                            // check if kenteken is valid
                            if (kentekenValid(kentekenTextField.text.toString())) {
                                // run API call
                                Khandler.run(kentekenTextField)
                            }
                        }
                    }
                }

                override fun afterTextChanged(editable: Editable) {
                    Log.d(TAG, "afterTextChanged: $editable")
                }
            })
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task: Task<String?> ->
                    if (!task.isSuccessful) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                        return@addOnCompleteListener
                    }

                    // Get new FCM registration token
                    val token = task.result

                    // Log
                    Log.d("FCM Token", token!!)
                }
            kentekenTextField.setOnKeyListener { v: View?, keyCode: Int, event: KeyEvent ->
                Log.d(TAG, "onKey: $keyCode")
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_ENTER -> {
                            Khandler.run(kentekenTextField)
                            return@setOnKeyListener true
                        }
                        KeyEvent.KEYCODE_DEL -> {
                            Log.d(TAG, "onKey: KEY EVENT")
                            var text = kentekenTextField.text.toString()
                            text = text.replace("-", "")
                            var newText = text
                            if (text.length > 0) {
                                newText = text.substring(0, text.length - 1)
                            }
                            kentekenTextField.setText(newText)
                            kentekenTextField.setSelection(kentekenTextField.text.length)
                            return@setOnKeyListener true
                        }
                        else -> {}
                    }
                }
                if (getSidecodeLicenseplate(
                        kentekenTextField.text.toString().uppercase(Locale.ROOT)
                    ) != -1 && getSidecodeLicenseplate(
                        kentekenTextField.text.toString().uppercase(Locale.ROOT)
                    ) != -2
                ) {
                    Log.d(TAG, "onKey: BINGO")
                    Khandler.run(kentekenTextField)
                    return@setOnKeyListener true
                }
                false
            }
            if (intent.getStringExtra("kenteken") != null) {
                Khandler.runCamera(intent.getStringExtra("kenteken")!!, kentekenTextField)
            }
        }.start()
    }// Code to be executed when the user is about to return

    // to the app after tapping on an ad.
    // Code to be executed when an ad opens an overlay that
    // covers the screen.
    protected val ads: Unit
        get() {
            MobileAds.initialize(this) { initializationStatus: InitializationStatus? -> }
            try {
                var advertisementView = AdView(this)
                advertisementView.adUnitId = "ca-app-pub-4928043878967484/5146910390"
                advertisementView = findViewById(R.id.ad1)
                advertisementView.adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        bundle = Bundle()
                        firebaseAnalytics.logEvent("Ad_loaded", bundle)
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        bundle = Bundle()
                        bundle.putString("Message", adError.message)
                        firebaseAnalytics.logEvent("Ad_error", bundle)
                    }

                    override fun onAdOpened() {
                        // Code to be executed when an ad opens an overlay that
                        // covers the screen.
                    }

                    override fun onAdClicked() {
                        bundle = Bundle()
                        firebaseAnalytics.logEvent("AD_CLICK", bundle)
                    }

                    override fun onAdClosed() {
                        // Code to be executed when the user is about to return
                        // to the app after tapping on an ad.
                    }
                }
                val adRequest = AdRequest.Builder().build()
                advertisementView.loadAd(adRequest)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    private fun startCameraIntent() {
        try {
            val intent = Intent(this, OcrCaptureActivity::class.java)
            intent.putExtra(OcrCaptureActivity.AutoFocus, true)
            intent.putExtra(OcrCaptureActivity.UseFlash, false)
            startActivityForResult(intent, RC_OCR_CAPTURE)
        } catch (e: Exception) {
            e.printStackTrace()
            e.message
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == RC_OCR_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                val text = data.getStringExtra(OcrCaptureActivity.TextBlockObject)
                val textfield = findViewById<EditText>(R.id.kenteken)
                Khandler.runCamera(text!!, textfield)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val RC_OCR_CAPTURE = 9003
    }
}