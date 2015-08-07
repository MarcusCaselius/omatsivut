package fi.vm.sade.omatsivut.servlet.session

import fi.vm.sade.auditlog.{LogMessage, Audit}
import fi.vm.sade.hakemuseditori.auditlog.AuditLogger
import fi.vm.sade.omatsivut.auditlog.Login
import fi.vm.sade.omatsivut.security.AuthenticationContext
import fi.vm.sade.omatsivut.security.AuthenticationInfoParser._
import fi.vm.sade.omatsivut.servlet.OmatSivutServletBase

trait SecuredSessionServletContainer {
  val auditLogger: AuditLogger
  val audit: Audit

  class SecuredSessionServlet(val authenticationContext: AuthenticationContext) extends OmatSivutServletBase with ShibbolethPaths {
    get("/initsession") {
      val info = getAuthenticationInfo(request)
      info.personOid match {
        case (Some(oid)) => {
          audit.log(new LogMessage(oid, info.toString))
          response.redirect(redirectUri)
        }
        case _ => redirectToShibbolethLogin(response, authenticationContext.ssoContextPath)
      }
    }

    private def redirectUri: String = {
      request.getContextPath + paramOption("redirect").getOrElse("/index.html")
    }
  }
}