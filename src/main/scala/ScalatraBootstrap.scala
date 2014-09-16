import javax.servlet.ServletContext

import fi.vm.sade.omatsivut.config.AppConfig.AppConfig
import fi.vm.sade.omatsivut.config.{AppConfig, OmatSivutSpringContext, ScalatraPaths}
import fi.vm.sade.omatsivut.servlet._
import fi.vm.sade.omatsivut.servlet.session.{LoginServlet, SessionServlet}
import fi.vm.sade.omatsivut.servlet.testing.FakeShibbolethServlet
import org.scalatra._

class ScalatraBootstrap extends LifeCycle {
  val config: AppConfig = AppConfig.fromSystemProperty
  OmatSivutSpringContext.check

  override def init(context: ServletContext) {
    config.componentRegistry.start

    context.mount(config.componentRegistry.newApplicationsServlet, ScalatraPaths.applications)
    context.mount(new TranslationServlet, "/translations")
    context.mount(config.componentRegistry.newKoulutusServlet, ScalatraPaths.koulutusinformaatio)
    context.mount(config.componentRegistry.newSwaggerServlet, "/swagger/*")
    context.mount(config.componentRegistry.newSecuredSessionServlet, "/secure")
    context.mount(new SessionServlet(config), "/session")
    context.mount(new RaamitServlet(config), "/raamit")
    context.mount(new LoginServlet(config), "/login")
    context.mount(config.componentRegistry.newLogoutServlet, "/logout")
    context.mount(config.componentRegistry.newTestHelperServlet, "/util")
    context.mount(new FakeShibbolethServlet(config), "/Shibboleth.sso")
  }

  override def destroy(context: ServletContext) = {
    config.componentRegistry.stop
    super.destroy(context)
  }
}
