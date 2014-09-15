package fi.vm.sade.omatsivut.auditlog

import java.util.concurrent.ArrayBlockingQueue

import fi.vm.sade.omatsivut.config.{OmatSivutSpringContext, SpringContextComponent, AppConfig}
import AppConfig.AppConfig
import fi.vm.sade.omatsivut.util.Logging
import org.slf4j.LoggerFactory

trait AuditLoggerComponent {
  val auditLogger: AuditLogger

  class AuditLoggerFacade(runningLogger: RunnableLogger) extends AuditLogger {
    protected val auditLog4jLogger = LoggerFactory.getLogger("audit")

    def log(event: AuditEvent) {
      runningLogger.log(event)
      auditLog4jLogger.info(event.toLogMessage)
    }
  }

  class RunnableLogger(val appConfig: AppConfig) extends Runnable with Logging {
    private val queue = new ArrayBlockingQueue[AuditEvent](2000)
    private def auditLogger = appConfig.springContext.auditLogger

    def log(event: AuditEvent) {
      try {
        queue.add(event)
      } catch {
        case e: IllegalStateException => event.toLogMessage
      }
    }

    override def run() {
      while(true) {
        val event = queue.take()
        withErrorLogging {
          auditLogger.log(event.toTapahtuma)
        }("Could not write Logout to auditlog, message was: " + event.toLogMessage)
      }
    }
  }
}

trait AuditLogger extends Logging {
  def log(event: AuditEvent)
}

