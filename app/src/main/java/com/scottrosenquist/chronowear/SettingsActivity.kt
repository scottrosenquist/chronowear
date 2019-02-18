package com.scottrosenquist.chronowear

import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.preference.*
import android.support.wearable.complications.ComplicationHelperActivity
import android.support.wearable.complications.ComplicationProviderInfo
import android.support.wearable.complications.ProviderChooserIntent.EXTRA_PROVIDER_INFO
import android.support.wearable.complications.ProviderInfoRetriever
import java.util.concurrent.Executors

class SettingsActivity : PreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()
    }

    class SettingsFragment : PreferenceFragment() {

        private lateinit var providerInfoRetriever: ProviderInfoRetriever

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            addPreferencesFromResource(R.xml.settings)

            val rootPreference = preferenceScreen
            initializePreference(rootPreference)

            initializeComplications()
        }

        override fun onDestroy() {
            super.onDestroy()

            providerInfoRetriever.release()
        }

        fun initializePreference(preference: Preference) {
            if (preference.isColourPreference()) {
                val colourPreference = preferences.colours[preference.getPreferenceId()]
                if (colourPreference != null) {
                    preference.summary = colourPreference.name
                    preference.icon = createColourIcon(colourPreference.hexString)
                }
            } else {
                preference.icon?.setTint(Color.WHITE)
            }
            addPreferenceIconBackground(preference)

            if (preference is PreferenceGroup) {
                for (index in 0 until preference.preferenceCount) {
                    initializePreference(preference.getPreference(index))
                }
            }
        }

        fun initializeComplications() {
            providerInfoRetriever = ProviderInfoRetriever(context, Executors.newCachedThreadPool())
            providerInfoRetriever.init()

            val onProviderInfoRetriever = object: ProviderInfoRetriever.OnProviderInfoReceivedCallback() {
                override fun onProviderInfoReceived(watchFaceComplicationId: Int, info: ComplicationProviderInfo?) {
                    setComplicationSummary(watchFaceComplicationId, info)
                }
            }

            providerInfoRetriever.retrieveProviderInfo(onProviderInfoRetriever, ComponentName(context, Chronowear::class.java), *COMPLICATION_IDS)
        }

        fun createColourIcon(hexString: String): Drawable {
            val icon = context.getDrawable(R.drawable.ic_circle).mutate()
            icon.setTint(Color.parseColor(hexString))
            return icon
        }

        fun addPreferenceIconBackground(preference: Preference) {
            if (preference.icon != null) preference.icon = addIconBackground(preference.icon)
        }

        fun addIconBackground(icon: Drawable?): LayerDrawable {
            val iconWithBackground = context.getDrawable(R.drawable.ic_config_background).mutate() as LayerDrawable
            iconWithBackground.setDrawableByLayerId(R.id.nested_icon, icon)
            return iconWithBackground
        }

        override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen?, preference: Preference?): Boolean {
            if (preferenceScreen?.key == "complications") {
                val complicationId = getComplicationId(preference?.key)
                if (complicationId != null) {
                    startActivityForResult(ComplicationHelperActivity.createProviderChooserHelperIntent(context, ComponentName(context, Chronowear::class.java), complicationId, *COMPLICATION_SUPPORTED_TYPES[complicationId]), complicationId)
                }

            }
            if (preference != null && preference.isColourPreference()) {
                val preferenceId = preference.getPreferenceId()
                val colourPreference = preferences.colours[preferenceId]
                if (colourPreference != null) {
                    val colourSettingsIntent = Intent(context, ColourSettingsActivity::class.java)
                    colourSettingsIntent.putExtra("selectedHexString", colourPreference.hexString)
                    startActivityForResult(colourSettingsIntent, preferenceId)
                }
            }

            return super.onPreferenceTreeClick(preferenceScreen, preference)
        }

        fun Preference.getPreferenceId() = resources.getIdentifier(this.key, "string", activity.packageName)

        fun Preference.isColourPreference() = this.extras["type"] == "colour"

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (resultCode == RESULT_OK) {
                when (requestCode) {
                    in COMPLICATION_IDS -> setComplicationSummary(requestCode, data?.extras?.get(EXTRA_PROVIDER_INFO) as? ComplicationProviderInfo)
                    else -> setColourPreference(requestCode, data)
                }
            } else {
                println("cancelled")
            }
        }

        private fun setComplicationSummary(complicationId: Int, complicationProviderInfo: ComplicationProviderInfo?) {
            val preferenceKey = getComplicationPreferenceKey(complicationId)
            if (preferenceKey != null) {
                findPreference(preferenceKey).summary = complicationProviderInfo?.providerName ?: "Empty"
                findPreference(preferenceKey).icon = addIconBackground(complicationProviderInfo?.providerIcon?.loadDrawable(context) ?: getDefaultComplicationIcon(preferenceKey))
            }
        }

        private fun setColourPreference(preferenceId: Int, data: Intent?) {
            val preference = findPreference(getString(preferenceId))
            val colourPreference = preferences.colours[preferenceId]
            val hexString = data?.getStringExtra("hexString")
            val name = data?.getStringExtra("name")

            if (colourPreference != null && hexString != null && name != null) {
                colourPreference.hexString = hexString
                colourPreference.name = name
                preference.apply {
                    summary = name
                    icon = addIconBackground(createColourIcon(hexString))
                }
                preference.summary = name
                preference.icon = addIconBackground(createColourIcon(hexString))
            }
        }

        private fun getDefaultComplicationIcon(preferenceKey: String?): Drawable? {
            val icon =  context.getDrawable(when (preferenceKey) {
                "left_complication_preference" -> R.drawable.ic_complication_left
                "top_complication_preference" -> R.drawable.ic_complication_top
                "right_complication_preference" -> R.drawable.ic_complication_right
                "bottom_complication_preference" -> R.drawable.ic_complication_bottom
                else -> R.drawable.ic_error
            }).mutate()
            icon.setTint(Color.WHITE)
            return icon
        }

        private fun getComplicationPreferenceKey(complicationId: Int?) = when (complicationId) {
            LEFT_COMPLICATION_ID -> "left_complication_preference"
            TOP_COMPLICATION_ID -> "top_complication_preference"
            RIGHT_COMPLICATION_ID -> "right_complication_preference"
            BOTTOM_COMPLICATION_ID -> "bottom_complication_preference"
            else -> null
        }

        private fun getComplicationId(preferenceKey: String?) = when (preferenceKey) {
            "left_complication_preference" -> LEFT_COMPLICATION_ID
            "top_complication_preference" -> TOP_COMPLICATION_ID
            "right_complication_preference" -> RIGHT_COMPLICATION_ID
            "bottom_complication_preference" -> BOTTOM_COMPLICATION_ID
            else -> null
        }
    }
}
