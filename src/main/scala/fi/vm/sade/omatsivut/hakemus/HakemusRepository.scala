package fi.vm.sade.omatsivut.hakemus
import fi.vm.sade.haku.oppija.hakemus.domain.Application
import fi.vm.sade.haku.oppija.lomake.domain.ApplicationSystem
import fi.vm.sade.omatsivut.domain.Language.Language
import fi.vm.sade.omatsivut.hakemus.domain.{HakemuksenTunniste, Hakemus, HakemusMuutos}

trait HakemusRepository {
  def updateHakemus(applicationSystem: ApplicationSystem)(hakemus: HakemusMuutos, userOid: String)(implicit lang: Language): Option[Hakemus]
  def findStoredApplicationByOid(oid: String): Application
  def fetchHakemukset(personOid: String)(implicit lang: Language): List[Hakemus]
  def getHakemus(personOid: String, hakemusOid: String)(implicit lang: Language): Option[Hakemus]
}
