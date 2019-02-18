package com.scottrosenquist.chronowear

import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationManager
import android.support.wearable.complications.ComplicationProviderService
import android.support.wearable.complications.ComplicationText

class DayOfTheWeekComplication : ComplicationProviderService() {

    override fun onComplicationUpdate(complicationId: Int, complicationType: Int, complicationManager: ComplicationManager) {

        val complicationText = ComplicationText.TimeFormatBuilder().apply {
            setStyle(ComplicationText.FORMAT_STYLE_UPPER_CASE)
            when (complicationType) {
                ComplicationData.TYPE_SHORT_TEXT -> setFormat("EEE")
                ComplicationData.TYPE_LONG_TEXT -> setFormat("EEEE")
            }
        }.build()

        val complicationData = ComplicationData.Builder(complicationType).apply {
            when (complicationType) {
                ComplicationData.TYPE_SHORT_TEXT -> setShortText(complicationText)
                ComplicationData.TYPE_LONG_TEXT -> setLongText(complicationText)
            }
        }.build()

        complicationManager.run { when {
                complicationData == null -> noUpdateRequired(complicationId)
                else -> updateComplicationData(complicationId, complicationData)
        } }

    }

}