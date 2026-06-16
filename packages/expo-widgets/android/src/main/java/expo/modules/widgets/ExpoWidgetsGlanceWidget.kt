package expo.modules.widgets

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize

open class ExpoWidgetsGlanceWidget(
  private val widgetName: String
) : GlanceAppWidget() {
  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val props = WidgetsLayoutRegistry.props(context, widgetName)
    val environment = getWidgetEnvironment(context)
    val node = WidgetsLayoutRegistry.layout(context, widgetName)?.let { layout ->
      evaluateLayout(context, layout, props, environment)
    } ?: createErrorNode("No layout found for $widgetName")

    provideContent {
      Box(modifier = GlanceModifier.fillMaxSize()) {
        DynamicView(node)
      }
    }
  }
}
