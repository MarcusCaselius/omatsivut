package fi.vm.sade.omatsivut

import fi.vm.sade.omatsivut.config.{AppConfig, ComponentRegistry}
import fi.vm.sade.omatsivut.security.{FakeAuthentication, ShibbolethCookie}
import fi.vm.sade.omatsivut.servlet.OmatSivutSwagger
import fi.vm.sade.omatsivut.util.PortChecker
import org.scalatra.test.HttpComponentsClient
import org.specs2.mutable.Specification
import org.specs2.specification.{Fragments, Step}

trait ScalatraTestSupport extends Specification with HttpComponentsClient {
  implicit val swagger = new OmatSivutSwagger
  lazy val appConfig = AppConfigSetup.create
  lazy val componentRegistry = new ComponentRegistry(appConfig)

  def baseUrl = "http://localhost:" + SharedJetty.port + "/omatsivut"

  def authGet[A](uri: String)(f: => A)(implicit personOid: PersonOid): A = {
    get(uri, headers = authHeaders(personOid.oid))(f)
  }

  def authPost[A](uri: String, body: Array[Byte])(f: => A)(implicit personOid: PersonOid): A = {
    post(uri, body, headers = authHeaders(personOid.oid))(f)
  }

  def authPut[A](uri: String, body: Array[Byte])(f: => A)(implicit personOid: PersonOid): A = {
    put(uri, body, headers = authHeaders(personOid.oid))(f)
  }

  def authHeaders[A](oid: String): Map[String, String] = {
    val shibbolethCookie: ShibbolethCookie = ShibbolethCookie("_shibsession_test", "test")
    Map("Cookie" -> (FakeAuthentication.oidCookie + "=" + oid + "; " + shibbolethCookie))
  }

  override def map(fs: => Fragments) = Step(SharedJetty.start) ^ super.map(fs)
}

object SharedJetty {
  private lazy val jettyLauncher = new JettyLauncher(PortChecker.findFreeLocalPort, Some("it"))

  def port = jettyLauncher.port

  def start {
    jettyLauncher.start
  }
}

object AppConfigSetup {
  lazy val create = AppConfig.fromSystemProperty
}

case class PersonOid(oid: String)
