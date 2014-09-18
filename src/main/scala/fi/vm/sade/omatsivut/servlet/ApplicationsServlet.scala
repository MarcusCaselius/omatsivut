package fi.vm.sade.omatsivut.servlet

import fi.vm.sade.haku.oppija.hakemus.domain.Application
import fi.vm.sade.omatsivut.auditlog.{AuditLoggerComponent, AuditLogger}
import fi.vm.sade.omatsivut.config.AppConfig.AppConfig
import fi.vm.sade.omatsivut.config.{OmatSivutSpringContext, SpringContextComponent}
import fi.vm.sade.omatsivut.hakemus._
import fi.vm.sade.omatsivut.hakemus.domain.{HakemusMuutos, ValidationError, _}
import fi.vm.sade.omatsivut.haku.domain.{HakuAika, QuestionNode}
import fi.vm.sade.omatsivut.haku.{HakuRepository, HakuRepositoryComponent}
import fi.vm.sade.omatsivut.json.JsonFormats
import fi.vm.sade.omatsivut.security.Authentication
import fi.vm.sade.omatsivut.valintatulokset.{Vastaanotto, ValintatulosService, ValintatulosServiceComponent}
import org.json4s.jackson.Serialization
import org.scalatra.json._
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder
import org.scalatra.swagger._
import org.scalatra.{BadRequest, Forbidden, NotFound, Ok}

trait ApplicationsServletContainer {
  this: HakuRepositoryComponent with
    HakemusRepositoryComponent with
    ValintatulosServiceComponent with
    ApplicationValidatorComponent with
    HakemusPreviewGeneratorComponent with
    SpringContextComponent with
    AuditLoggerComponent =>

  val hakuRepository: HakuRepository
  val hakemusRepository: HakemusRepository
  val springContext: OmatSivutSpringContext
  val valintatulosService: ValintatulosService

  class ApplicationsServlet(val appConfig: AppConfig)(implicit val swagger: Swagger) extends OmatSivutServletBase with JacksonJsonSupport with JsonFormats with SwaggerSupport with Authentication {
    override def applicationName = Some("api")
    private val applicationSystemService = springContext.applicationSystemService
    private val applicationValidator: ApplicationValidator = newApplicationValidator
    override val authAuditLogger: AuditLogger = auditLogger

    protected val applicationDescription = "Oppijan henkilökohtaisen palvelun REST API, jolla voi hakea ja muokata hakemuksia ja omia tietoja"

    val getApplicationsSwagger: OperationBuilder = (apiOperation[List[Hakemus]]("getApplications")
      summary "Hae kirjautuneen oppijan hakemukset"
      )

    val putApplicationsSwagger = (apiOperation[Unit]("putApplication")
      summary "Tallenna hakemus"
      )

    val validateApplicationsSwagger = (apiOperation[Unit]("validateApplication")
      summary "Tarkista hakemus ja palauta virheet sekä kysymykset joihin ei ole vastattu"
      )

    val previewApplicationSwagger: OperationBuilder = (apiOperation[String]("previewApplication")
      summary "Hakemuksen esikatselu HTML-muodossa"
    )

    before() {
      contentType = formats("json")
    }

    get("/applications", operation(getApplicationsSwagger)) {
      hakemusRepository.fetchHakemukset(personOid())
    }

    put("/applications/:oid", operation(putApplicationsSwagger)) {
      val content: String = request.body
      val updated = Serialization.read[HakemusMuutos](content)
      val applicationSystem = applicationSystemService.getApplicationSystem(updated.hakuOid)
      val errors = applicationValidator.validate(applicationSystem)(updated)
      if(errors.isEmpty) {
        hakemusRepository.updateHakemus(applicationSystem)(updated, personOid()) match {
          case Some(saved) => Ok(saved)
          case None => Forbidden()
        }
      } else {
        BadRequest(errors)
      }
    }

    post("/applications/validate/:oid", operation(validateApplicationsSwagger)) {
      val muutos = Serialization.read[HakemusMuutos](request.body)
      val applicationSystem = applicationSystemService.getApplicationSystem(muutos.hakuOid)
      val questionsOf: List[String] = paramOption("questionsOf").getOrElse("").split(',').toList
      val (errors: List[ValidationError], questions: List[QuestionNode], updatedApplication: Application) = applicationValidator.validateAndFindQuestions(applicationSystem)(muutos, questionsOf, personOid())
      ValidationResult(errors, questions, hakuRepository.getApplicationPeriods(updatedApplication, applicationSystem))
    }

    get("/applications/preview/:oid") {
      newHakemusPreviewGenerator(language).generatePreview(ServerContaxtPath(request), personOid(), params("oid")) match {
        case Some(previewHtml) =>
          contentType = formats("html")
          Ok(previewHtml)
        case None =>
          NotFound()
      }
    }

    post("/applications/vastaanota/:hakuOid/:hakemusOid") {
      val hakemusOid = params("hakemusOid")
      val hakuOid = params("hakuOid")
      if (!hakemusRepository.exists(personOid(), hakuOid, hakemusOid)) {
        response.setStatus(404)
        "Not found"
      } else {
        val vastaanotto = Serialization.read[ClientSideVastaanotto](request.body)
        val muokkaaja: String = "henkilö:" + personOid()
        val selite = "Muokkaus Omat Sivut -palvelussa"
        valintatulosService.vastaanota(hakemusOid, hakuOid, Vastaanotto(vastaanotto.hakukohdeOid, vastaanotto.tila, muokkaaja, selite))
        hakemusRepository.getHakemus(personOid(), hakemusOid)
      }
    }
  }
}

case class ClientSideVastaanotto(hakukohdeOid: String, tila: String)

case class ValidationResult(errors: List[ValidationError], questions: List[QuestionNode], applicationPeriods: List[HakuAika])

