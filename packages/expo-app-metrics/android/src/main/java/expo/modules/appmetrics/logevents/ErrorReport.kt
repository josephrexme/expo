package expo.modules.appmetrics.logevents

import expo.modules.appmetrics.storage.LogRecord
import expo.modules.appmetrics.utils.JsonAny
import expo.modules.appmetrics.utils.TimeUtils
import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import expo.modules.kotlin.types.Enumerable
import expo.modules.kotlin.types.OptimizedRecord

/**
 * An unhandled JavaScript error forwarded from the JS-side `global.ErrorUtils` handler. Recorded as
 * an `exception` log event following OpenTelemetry's exception conventions.
 */
@OptimizedRecord
data class ErrorReport(
  @Field val source: ErrorSource = ErrorSource.GLOBAL,
  @Field val type: String? = null,
  @Field val message: String = "",
  @Field val stacktrace: String? = null,
  @Field val isFatal: Boolean = false
) : Record {
  /** Builds the `exception` log event for the live (non-fatal) path. */
  fun toLogRecord(sessionId: String): LogRecord =
    makeErrorLogRecord(
      sessionId = sessionId,
      source = source.rawValue,
      type = type,
      message = message,
      stacktrace = stacktrace,
      isFatal = isFatal
    )

  /**
   * Snapshots this report for durable on-disk storage, capturing the session it belongs to and the
   * time it happened, both resolved now since by drain time the main session has rotated.
   */
  fun toPendingError(sessionId: String): PendingErrorStore.PendingError =
    PendingErrorStore.PendingError(
      source = source.rawValue,
      type = type,
      message = message,
      stacktrace = stacktrace,
      sessionId = sessionId,
      timestamp = TimeUtils.getCurrentTimestampInISOFormat()
    )
}

/** How the error was captured. A closed set so the `expo.error.source` attribute stays consistent. */
enum class ErrorSource(val rawValue: String) : Enumerable {
  GLOBAL("global")
}

/**
 * Builds the `exception` log event. Following OpenTelemetry's exception-in-logs convention, the error
 * rides as `exception.*` attributes (the event name is `exception` because this captures errors from a
 * handler, not a specific operation). `expo.error.*` carries the bits OTel has no field for: the
 * capture source and whether the error was fatal. Fatal errors log at `fatal` severity, the rest at
 * `error`. Shared by the live path and the next-launch ingest of pending fatal errors.
 */
fun makeErrorLogRecord(
  sessionId: String,
  source: String,
  type: String?,
  message: String,
  stacktrace: String?,
  isFatal: Boolean,
  timestamp: String = TimeUtils.getCurrentTimestampInISOFormat()
): LogRecord {
  val attributes = buildMap<String, Any?> {
    put("expo.error.source", source)
    put("expo.error.is_fatal", isFatal)
    type?.let { put("exception.type", it) }
    put("exception.message", message)
    stacktrace?.let { put("exception.stacktrace", it) }
  }
  return LogRecord(
    sessionId = sessionId,
    timestamp = timestamp,
    name = "exception",
    severity = (if (isFatal) Severity.FATAL else Severity.ERROR).rawValue,
    attributes = JsonAny.encodeMapToJsonString(attributes)
  )
}
