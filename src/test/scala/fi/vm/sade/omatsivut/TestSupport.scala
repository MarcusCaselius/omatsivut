package fi.vm.sade.omatsivut

import org.scalatra.test.specs2.MutableScalatraSpec

trait TestSupport extends MutableScalatraSpec {

  def authGet[A](uri: String, oid : String)(f: => A): A = {
    get(uri, headers = Map("Cookie" -> ("auth=" + AuthenticationCipher.encrypt(CookieCredentials(oid).toString))))(f)
  }
}