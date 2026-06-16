package expo.modules.widgets

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val ACTION_APPWIDGET_UPDATE = "android.appwidget.action.APPWIDGET_UPDATE"
private const val WIDGET_NAME_METADATA = "expo.modules.widgets.NAME"

internal object WidgetsUpdater {
  private val updateScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  fun reload(context: Context, name: String) {
    updateScope.launch {
      updateReceivers(context) { receiver ->
        receiver.widgetName == name
      }
    }
  }

  fun reloadAll(context: Context) {
    updateScope.launch {
      updateReceivers(context) { true }
    }
  }

  private suspend fun updateReceivers(
    context: Context,
    shouldUpdate: (WidgetReceiver) -> Boolean
  ) {
    val appContext = context.applicationContext
    val manager = GlanceAppWidgetManager(appContext)

    widgetReceivers(appContext)
      .filter(shouldUpdate)
      .forEach { receiver ->
        manager.getGlanceIds(receiver.glanceAppWidget::class.java)
          .forEach { glanceId ->
            receiver.glanceAppWidget.update(appContext, glanceId)
          }
      }
  }

  private fun widgetReceivers(context: Context): List<WidgetReceiver> {
    val packageManager = context.packageManager
    return packageManager
      .queryWidgetReceivers(Intent(ACTION_APPWIDGET_UPDATE).setPackage(context.packageName))
      .mapNotNull { resolveInfo ->
        val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
        if (activityInfo.packageName != context.packageName) {
          return@mapNotNull null
        }

        val componentName = ComponentName(activityInfo.packageName, activityInfo.name)
        val widgetName = activityInfo.metaData?.getString(WIDGET_NAME_METADATA) ?: return@mapNotNull null
        val receiver = instantiateReceiver(componentName.className) ?: return@mapNotNull null

        WidgetReceiver(widgetName, receiver.glanceAppWidget)
      }
  }

  private fun PackageManager.queryWidgetReceivers(intent: Intent): List<ResolveInfo> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      queryBroadcastReceivers(
        intent,
        PackageManager.ResolveInfoFlags.of(PackageManager.GET_META_DATA.toLong())
      )
    } else {
      @Suppress("DEPRECATION")
      queryBroadcastReceivers(intent, PackageManager.GET_META_DATA)
    }
  }

  private fun instantiateReceiver(className: String): ExpoWidgetsAppWidgetProvider? {
    return runCatching {
      Class.forName(className)
        .getDeclaredConstructor()
        .newInstance() as? ExpoWidgetsAppWidgetProvider
    }.getOrNull()
  }

  private data class WidgetReceiver(
    val widgetName: String,
    val glanceAppWidget: GlanceAppWidget
  )
}
