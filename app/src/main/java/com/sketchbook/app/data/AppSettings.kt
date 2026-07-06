package com.sketchbook.app.data

import android.content.Context
import android.content.SharedPreferences
import com.sketchbook.app.model.BrushType
import com.sketchbook.app.model.PencilGrade
import com.sketchbook.app.model.TemplateKind

/** SharedPreferences-backed app settings, mirroring the iOS @AppStorage keys. */
class AppSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var theme: String
        get() = prefs.getString("theme", "light") ?: "light"
        set(v) = prefs.edit().putString("theme", v).apply()

    /** On Android finger drawing defaults ON (no Apple Pencil equivalence). */
    var fingerDrawing: Boolean
        get() = prefs.getBoolean("fingerDrawing", true)
        set(v) = prefs.edit().putBoolean("fingerDrawing", v).apply()

    var haptics: Boolean
        get() = prefs.getBoolean("haptics", true)
        set(v) = prefs.edit().putBoolean("haptics", v).apply()

    var defaultBrush: BrushType
        get() = runCatching {
            BrushType.valueOf(prefs.getString("defaultBrush", BrushType.PEN.name)!!)
        }.getOrDefault(BrushType.PEN)
        set(v) = prefs.edit().putString("defaultBrush", v.name).apply()

    var defaultPencilGrade: PencilGrade
        get() = runCatching {
            PencilGrade.valueOf(prefs.getString("defaultPencilGrade", PencilGrade.HB.name)!!)
        }.getOrDefault(PencilGrade.HB)
        set(v) = prefs.edit().putString("defaultPencilGrade", v.name).apply()

    var defaultEraseSize: Float
        get() = prefs.getFloat("defaultEraseSize", 24f)
        set(v) = prefs.edit().putFloat("defaultEraseSize", v).apply()

    var defaultTemplate: TemplateKind
        get() = runCatching {
            TemplateKind.valueOf(prefs.getString("defaultTemplate", TemplateKind.BLANK.name)!!)
        }.getOrDefault(TemplateKind.BLANK)
        set(v) = prefs.edit().putString("defaultTemplate", v.name).apply()

    var defaultLandscape: Boolean
        get() = prefs.getBoolean("defaultLandscape", true)
        set(v) = prefs.edit().putBoolean("defaultLandscape", v).apply()
}
