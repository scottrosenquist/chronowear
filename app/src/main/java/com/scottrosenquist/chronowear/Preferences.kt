package com.scottrosenquist.chronowear

import android.content.Context
import android.graphics.Color
import android.preference.PreferenceManager
import kotlin.reflect.KProperty

class Preferences(val context: Context) {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    var ambientFullMute by BooleanPreference(R.string.ambient_full_mute_preference, false)

    val accent = ColourPreference(R.string.accent_colour_preference, "#F44336", "Red")
    val background = ColourPreference(R.string.background_colour_preference, "#000000", "Black")

    val colourPreferencesArray = arrayOf(accent, background)

    val colours = colourPreferencesArray.associateBy( {it.stringId}, {it})

    inner class BooleanPreference(val stringId: Int, val defaultBoolean: Boolean) {
        operator fun getValue(preferences: Preferences, property: KProperty<*>): Boolean {
            return sharedPreferences.getBoolean(context.getString(stringId), defaultBoolean)
        }

        operator fun setValue(preferences: Preferences, property: KProperty<*>, value: Boolean) {
            sharedPreferences.edit().putBoolean(context.getString(stringId), value).apply()
        }
    }
    
    inner class ColourPreference(val stringId: Int, val defaultHexString: String, val defaultColourName: String) {
        val colour by ColourValue()
        var hexString by ColourHexString()
        var name by ColourName()

        inner class ColourValue() {
            operator fun getValue(colourPreference: Preferences.ColourPreference, property: KProperty<*>): Int {
                return Color.parseColor(preferences.sharedPreferences.getString(context.getString(stringId), defaultHexString))
            }
        }
        
        inner class ColourHexString() {
            operator fun getValue(colourPreference: Preferences.ColourPreference, property: KProperty<*>): String {
                return sharedPreferences.getString(context.getString(stringId), defaultHexString)
            }

            operator fun setValue(colourPreference: ColourPreference, property: KProperty<*>, value: String) {
                sharedPreferences.edit().putString(context.getString(stringId), value).apply()
            }

        }
        
        inner class ColourName() {
            operator fun getValue(colourPreference: ColourPreference, property: KProperty<*>): String {
                return sharedPreferences.getString(context.getString(stringId) + "_summary", defaultColourName)
            }

            operator fun setValue(colourPreference: ColourPreference, property: KProperty<*>, value: String) {
                sharedPreferences.edit().putString(context.getString(stringId) + "_summary", value).apply()
            }
        }
    }
}
