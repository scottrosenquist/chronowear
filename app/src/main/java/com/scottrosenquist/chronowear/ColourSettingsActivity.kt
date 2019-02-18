package com.scottrosenquist.chronowear

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.preference.*
import android.app.Activity
import android.graphics.drawable.Drawable

var selectedHexString: String = "none"

class ColourSettingsActivity : PreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        selectedHexString = intent.extras.getString("selectedHexString")

        fragmentManager.beginTransaction().replace(android.R.id.content, ColourSettingsFragment()).commit()
    }

    class ColourSettingsFragment : PreferenceFragment() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.colour_settings)

            val rootPreference = preferenceScreen

            addIcon(rootPreference)
        }

        override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen?, preference: Preference?): Boolean {
            val requestData = Intent().run {
                putExtra("hexString", preference?.key)
                putExtra("name", preference?.title)
            }
            activity.run {
                setResult(Activity.RESULT_OK, requestData)
                finish()
            }

            return super.onPreferenceTreeClick(preferenceScreen, preference)
        }

        fun addIcon(preference: Preference) {
            if (preference is PreferenceGroup) {
                for (index in 0 until preference.preferenceCount) {
                    addIcon(preference.getPreference(index))
                }
            } else {
                preference.icon = createColourSettingsIcon(preference)
            }
        }

        fun createColourSettingsIcon(preference: Preference): Drawable {
            val layeredIcon = context.getDrawable(R.drawable.ic_config_background).mutate() as LayerDrawable
            val nestedIcon = context.getDrawable(if (preference.key == selectedHexString) R.drawable.ic_circle_check else R.drawable.ic_circle).mutate()
            nestedIcon.setTint(Color.parseColor(preference.key))
            layeredIcon.setDrawableByLayerId(R.id.nested_icon, nestedIcon)
            return layeredIcon
        }
    }
}
