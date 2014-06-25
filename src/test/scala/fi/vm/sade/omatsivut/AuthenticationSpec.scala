package fi.vm.sade.omatsivut

import org.joda.time.DateTime

class AuthenticationSpec extends TestSupport {
  "GET /applications" should {
    "return 401 if not authenticated" in {
      get("/applications") {
        status must_== 401
      }
    }

    "return 401 if cookie has timed out" in {
      authGet("/applications", "1.2.246.562.24.14229104472") {
        status must_== 401
      }
    }

    "delete cookie if cookie has timed out" in {
      authGet("/applications", "1.2.246.562.24.14229104472") {
        val expires = response.getHeader("Set-Cookie").split(";").toList.find(_.startsWith("Expires="))
        expires.get must_== "Expires=Thu, 01-Jan-1970 00:00:00 GMT"
      }
    }
  }

  addServlet(new OHPServlet()(new OHPSwagger) {
    override val cookieTimeoutMinutes = 0
  }, "/*")

}