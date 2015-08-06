package fi.vm.sade.omatsivut.auditlog

import fi.vm.sade.hakemuseditori.auditlog.{AuditContext}
import fi.vm.sade.omatsivut.security.AuthenticationInfo

case class Login(authInfo: AuthenticationInfo, target: String = "Session") {
  def toTapahtuma(context: AuditContext) = context.systemName + target + toLogMessage + System.currentTimeMillis()

  def toLogMessage = "Käyttäjä kirjautui sisään: " + authInfo.toString
}
