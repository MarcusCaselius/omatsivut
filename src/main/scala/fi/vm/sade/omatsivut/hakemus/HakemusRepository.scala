package fi.vm.sade.omatsivut.hakemus

import java.util.Date
import fi.vm.sade.haku.oppija.hakemus.domain.Application
import fi.vm.sade.haku.oppija.lomake.domain.ApplicationSystem
import fi.vm.sade.omatsivut._
import fi.vm.sade.omatsivut.config.AppConfig
import AppConfig.AppConfig
import fi.vm.sade.omatsivut.auditlog.{ShowHakemus, UpdateHakemus, AuditLogger}
import fi.vm.sade.omatsivut.domain.Language
import fi.vm.sade.omatsivut.hakemus.domain.{Haku, HakuAika, Hakemus}
import fi.vm.sade.omatsivut.util.Timer
import fi.vm.sade.haku.virkailija.lomakkeenhallinta.util.OppijaConstants
import fi.vm.sade.omatsivut.koulutusinformaatio.KoulutusInformaatioService

case class HakemusRepository(implicit val appConfig: AppConfig) extends Timer {
  import collection.JavaConversions._
  private val dao = appConfig.springContext.applicationDAO
  private val koulutusinformaatioService = KoulutusInformaatioService(appConfig)

  def canUpdate(applicationSystem: ApplicationSystem, application: Application)(implicit lang: Language.Language) = {
    val haku = HakuConverter.convertToHaku(applicationSystem)
    val isActiveHakuPeriod = haku.applicationPeriods.exists(hakuAika => hakuAika.active)
    val stateUpdateable = application.getState == Application.State.ACTIVE || application.getState == Application.State.INCOMPLETE
    val inPostProcessing = !(application.getRedoPostProcess() == Application.PostProcessingState.DONE || application.getRedoPostProcess() == null)
    isActiveHakuPeriod && stateUpdateable && !inPostProcessing
  }

  def updateHakemus(applicationSystem: ApplicationSystem)(hakemus: Hakemus, userOid: String)(implicit lang: Language.Language): Option[Hakemus] = {
    val updatedHakemus = hakemus.copy(updated = new Date().getTime)
    val applicationQuery: Application = new Application().setOid(updatedHakemus.oid)
    val applicationJavaObject: Option[Application] = timed({
      dao.find(applicationQuery).toList.headOption
    }, 1000, "Application fetch DAO")

    timed({
      applicationJavaObject
        .filter(application => canUpdate(applicationSystem, application))
        .map { application =>
        val originalAnswers: Hakemus.Answers = application.getAnswers.toMap.mapValues(_.toMap)
        ApplicationUpdater.update(applicationSystem)(application, updatedHakemus)
        timed({
          dao.update(applicationQuery, application)
        }, 1000, "Application update DAO")
        AuditLogger.log(UpdateHakemus(userOid, updatedHakemus.oid, originalAnswers, application.getAnswers.toMap.mapValues(_.toMap)))
        HakemusConverter.convertToHakemus(applicationSystem, HakuConverter.convertToHaku(applicationSystem))(application)
      }
    }, 1000, "Application update")
  }

  def findStoredApplication(hakemus: Hakemus): Application = {
    val applications = timed({
      dao.find(new Application().setOid(hakemus.oid)).toList
    }, 1000, "Application fetch DAO")
    if (applications.size > 1) throw new RuntimeException("Too many applications for oid " + hakemus.oid)
    if (applications.size == 0) throw new RuntimeException("Application not found for oid " + hakemus.oid)
    val application = applications.head
    application
  }

  def fetchHakemukset(personOid: String)(implicit lang: Language.Language): List[Hakemus] = {
    timed({
      val applicationJavaObjects: List[Application] = timed({
        dao.find(new Application().setPersonOid(personOid)).toList
      }, 1000, "Application fetch DAO")
      applicationJavaObjects.filter{
          application => {
            !application.getState.equals(Application.State.PASSIVE)
          }
      }.map(application => {
        val hakuOption = timed({
          HakuRepository().getHakuById(application.getApplicationSystemId)
        }, 1000, "HakuRepository get haku")
        hakuOption.map { case (applicationSystem: ApplicationSystem, haku: Haku) => {
          val hakemus = HakemusConverter.convertToHakemus(applicationSystem, haku)(application)
          AuditLogger.log(ShowHakemus(personOid, hakemus.oid))
          hakemus.withApplicationPeriods(getApplicationPeriods(hakemus, applicationSystem))
        }}
      }).flatten.toList.sortBy[Long](_.received).reverse
    }, 1000, "Application fetch")
  }

  private def getApplicationPeriods(hakemus: Hakemus, applicationSystem: ApplicationSystem) = {
    if (applicationSystem.getApplicationSystemType == OppijaConstants.LISA_HAKU)
      getHakutoiveApplicationPeriods(hakemus, applicationSystem)
    else
      hakemus.haku.applicationPeriods
  }

  private def getHakutoiveApplicationPeriods(hakemus: Hakemus, applicationSystem: ApplicationSystem) : List[HakuAika] = {
    val hakutoiveApplicationPeriods = hakemus.hakutoiveet.headOption
      .flatMap(_.get("Koulutus-id"))
      .flatMap(koulutusinformaatioService.koulutus(_))
      .flatMap { koulutus => (koulutus.applicationStartDate, koulutus.applicationEndDate) match {
        case (Some(start), Some(end)) =>
          Some(List(HakuAika(start, end)))
        case _ =>
          None
      }}
    hakutoiveApplicationPeriods.getOrElse(hakemus.haku.applicationPeriods)
  }
}