package expo.modules.widgets

import android.content.Context
import androidx.core.content.edit

internal object WidgetsStorage {
  private const val PREFERENCES_NAME = "expo.modules.widgets"

  fun set(context: Context, value: String, forKey: String) {
    preferences(context).edit { putString(forKey, value) }
  }

  fun set(context: Context, value: Map<String, Any?>, forKey: String) {
    set(context, WidgetsJson.stringifyMap(value), forKey)
  }

  fun set(context: Context, value: List<Map<String, Any?>>, forKey: String) {
    set(context, WidgetsJson.stringifyList(value), forKey)
  }

  fun getString(context: Context, forKey: String): String? {
    return preferences(context).getString(forKey, null)
  }

  fun getDictionary(context: Context, forKey: String): Map<String, Any?>? {
    val value = getString(context, forKey) ?: return null
    return runCatching { WidgetsJson.parseMap(value) }.getOrNull()
  }

  fun remove(context: Context, forKey: String) {
    preferences(context).edit { remove(forKey) }
  }

  private fun preferences(context: Context) =
    context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}
