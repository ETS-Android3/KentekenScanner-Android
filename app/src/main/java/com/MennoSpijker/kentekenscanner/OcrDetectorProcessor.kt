/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.MennoSpijker.kentekenscanner

import android.content.Context
import com.MennoSpijker.kentekenscanner.View.KentekenHandler.Companion.kentekenValid
import com.MennoSpijker.kentekenscanner.Camera.GraphicOverlay
import com.MennoSpijker.kentekenscanner.OcrGraphic
import com.MennoSpijker.kentekenscanner.OcrCaptureActivity
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.Detector.Detections
import android.util.SparseArray
import com.MennoSpijker.kentekenscanner.View.KentekenHandler
import com.MennoSpijker.kentekenscanner.OcrDetectorProcessor
import android.content.Intent
import com.google.android.gms.common.api.CommonStatusCodes
import android.media.AudioManager
import android.media.MediaPlayer
import com.MennoSpijker.kentekenscanner.R
import android.os.Vibrator
import android.os.Build
import android.os.VibrationEffect
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import android.view.View
import java.lang.Exception
import java.util.*

/**
 * A very simple Processor which receives detected TextBlocks and adds them to the overlay
 * as OcrGraphics.
 */
internal class OcrDetectorProcessor(
    private val mGraphicOverlay: GraphicOverlay<OcrGraphic>,
    private val ocrCaptureActivity: OcrCaptureActivity
) : Detector.Processor<TextBlock?> {
    private var snackbarOpened = false
    override fun receiveDetections(detections: Detections<TextBlock?>) {
        mGraphicOverlay.clear()
        val items = detections.detectedItems
        for (i in 0 until items.size()) {
            val item = items.valueAt(i)
            if (item != null && item.value != null) {
                if (kentekenValid(item.value)) {
                    if (DIRECTSEARCH) {
                        val data = Intent()
                        data.putExtra(OcrCaptureActivity.TextBlockObject, item.value)
                        ocrCaptureActivity.setResult(CommonStatusCodes.SUCCESS, data)
                        val audio =
                            ocrCaptureActivity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        if (audio.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                            try {
                                val rand = Random()
                                var mPlayer: MediaPlayer
                                val r = rand.nextInt(3)
                                Log.println(Log.INFO, TAG, Integer.toString(r))
                                mPlayer = MediaPlayer.create(ocrCaptureActivity, R.raw.beep)
                                mPlayer.start()
                                val vi =
                                    ocrCaptureActivity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    vi.vibrate(VibrationEffect.EFFECT_CLICK.toLong())
                                } else {
                                    vi.vibrate(700)
                                }
                            } catch (IE: Exception) {
                                Log.println(Log.ERROR, TAG, "no sound could be played.")
                                IE.printStackTrace()
                            }
                        }
                        ocrCaptureActivity.finish()
                    } else {
                        if (!snackbarOpened) {
                            val snackbar = Snackbar.make(
                                mGraphicOverlay, item.value,
                                Snackbar.LENGTH_LONG
                            )
                            snackbar.setAction("Dit kenteken opzoeken") {
                                snackbar.dismiss()
                                val data = Intent()
                                data.putExtra(OcrCaptureActivity.TextBlockObject, item.value)
                                ocrCaptureActivity.setResult(CommonStatusCodes.SUCCESS, data)
                                val audio =
                                    ocrCaptureActivity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                if (audio.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                                    try {
                                        val rand = Random()
                                        val mPlayer: MediaPlayer
                                        val r = rand.nextInt(3)
                                        Log.println(Log.INFO, TAG, Integer.toString(r))
                                        mPlayer = MediaPlayer.create(ocrCaptureActivity, R.raw.beep)
                                        mPlayer.start()
                                        val vi =
                                            ocrCaptureActivity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            vi.vibrate(VibrationEffect.EFFECT_CLICK.toLong())
                                        } else {
                                            vi.vibrate(700)
                                        }
                                    } catch (IE: Exception) {
                                        Log.println(Log.ERROR, TAG, "no sound could be played.")
                                        IE.printStackTrace()
                                    }
                                }
                                ocrCaptureActivity.finish()
                            }
                            snackbar.addCallback(object : Snackbar.Callback() {
                                override fun onDismissed(snackbar: Snackbar, event: Int) {
                                    snackbarOpened = false
                                }

                                override fun onShown(snackbar: Snackbar) {
                                    snackbarOpened = true
                                }
                            })
                            snackbar.show()
                        }
                        try {
                            val graphic = OcrGraphic(mGraphicOverlay, item)
                            mGraphicOverlay.add(graphic)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            e.message
                        }
                    }
                }
            }
        }
    }

    override fun release() {
        mGraphicOverlay.clear()
    }

    companion object {
        private const val DIRECTSEARCH = true
        private const val TAG = "OcrDetectorProcessor"
    }
}