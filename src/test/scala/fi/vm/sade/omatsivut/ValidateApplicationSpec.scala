package fi.vm.sade.omatsivut

import fi.vm.sade.omatsivut.domain.{Question, Translations, Hakemus, ValidationError}
import fi.vm.sade.omatsivut.json.JsonFormats
import fi.vm.sade.omatsivut.servlet.ApplicationsServlet
import org.json4s._
import org.json4s.native.{JsonMethods, Serialization}

class ValidateApplicationSpec extends JsonFormats with ScalatraTestSupport {
  sequential

  "POST /application/validate" should {
    "validate application" in {
      AppConfig.fromSystemProperty.withConfig {
        authGet("/applications", "1.2.246.562.24.14229104472") {
          val applications: List[Hakemus] = Serialization.read[List[Hakemus]](body)
          val hakemus = applications(0)
          authPost("/applications/validate/" + hakemus.oid, "1.2.246.562.24.14229104472", Serialization.write(hakemus)) {
            status must_== 200
            val result: JValue = JsonMethods.parse(body)
            val errors: List[ValidationError] = (result \ "errors").extract[List[ValidationError]]
            val questions: List[AnyQuestion] = (result \ "questions").extract[List[AnyQuestion]]
            errors.size must_== 3
            questions.size must_== 3
            questions.head.title must_== Translations(Map("fi" -> "Päättötodistuksen kaikkien oppiaineiden keskiarvo?"))
          }
        }
      }
    }
  }

  case class AnyQuestion(title: Translations, questionType: String) extends Question

  addServlet(new ApplicationsServlet(), "/*")
}