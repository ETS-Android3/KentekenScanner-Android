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

import com.MennoSpijker.kentekenscanner.Camera.GraphicOverlay
import com.google.android.gms.vision.text.TextBlock
import com.MennoSpijker.kentekenscanner.Camera.GraphicOverlay.Graphic
import android.graphics.RectF
import android.graphics.Canvas
import android.graphics.Color
import com.MennoSpijker.kentekenscanner.OcrGraphic
import android.graphics.Paint
import android.util.Log

/**
 * Graphic instance for rendering TextBlock position, size, and ID within an associated graphic
 * overlay view.
 */
class OcrGraphic internal constructor(overlay: GraphicOverlay<*>?, text: TextBlock?) :
    Graphic(overlay) {
    var id = 0
    val textBlock: TextBlock?

    /**
     * Checks whether a point is within the bounding box of this graphic.
     * The provided point should be relative to this graphic's containing overlay.
     *
     * @param x An x parameter in the relative context of the canvas.
     * @param y A y parameter in the relative context of the canvas.
     * @return True if the provided point is contained within this graphic's bounding box.
     */
    override fun contains(x: Float, y: Float): Boolean {
        val text = textBlock ?: return false
        val rect = RectF(text.boundingBox)
        rect.left = translateX(rect.left)
        rect.top = translateY(rect.top)
        rect.right = translateX(rect.right)
        rect.bottom = translateY(rect.bottom)
        return rect.left < x && rect.right > x && rect.top < y && rect.bottom > y
    }

    /**
     * Draws the text block annotations for position, size, and raw value on the supplied canvas.
     */
    override fun draw(canvas: Canvas) {
        // TODO: Draw the text onto the canvas.
        if (textBlock == null) {
            return
        }

        // Draws the bounding box around the TextBlock.
        val rect = RectF(textBlock.boundingBox)
        rect.left = translateX(rect.left)
        rect.top = translateY(rect.top)
        rect.right = translateX(rect.right)
        rect.bottom = translateY(rect.bottom)
        canvas.drawRect(rect, sRectPaint!!)

        // Break the text into multiple lines and draw each one according to its own bounding box.
        val textComponents = textBlock.components
        for (currentText in textComponents) {
            val left = translateX(currentText.boundingBox.left.toFloat())
            val bottom = translateY(currentText.boundingBox.bottom.toFloat())
            canvas.drawText(currentText.value, left, bottom, sTextPaint!!)
        }
    }

    companion object {
        private const val TAG = "OCRGRAPHIC"
        private const val TEXT_COLOR = Color.WHITE
        private const val BORDER_COLOR = Color.BLACK
        private var sRectPaint: Paint? = null
        private var sTextPaint: Paint? = null
    }

    init {
        Log.w(TAG, "running ocrGraphic()")
        textBlock = text
        if (sRectPaint == null) {
            sRectPaint = Paint()
            sRectPaint!!.color = BORDER_COLOR
            sRectPaint!!.style = Paint.Style.STROKE
            sRectPaint!!.strokeWidth = 4.0f
        }
        if (sTextPaint == null) {
            sTextPaint = Paint()
            sTextPaint!!.color = TEXT_COLOR
            sTextPaint!!.textSize = 54.0f
        }
        // Redraw the overlay, as this graphic has been added.
        postInvalidate()
    }
}