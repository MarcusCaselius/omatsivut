package fi.vm.sade.omatsivut.hakemus

import fi.vm.sade.omatsivut.PersonOid
import fi.vm.sade.omatsivut.config.AppConfig
import fi.vm.sade.omatsivut.fixtures.ValintatulosFixtureImporter
import fi.vm.sade.omatsivut.hakemus.domain.{Hakemus, HakuPaattynyt}
import fi.vm.sade.omatsivut.servlet.ClientSideVastaanotto
import fi.vm.sade.omatsivut.valintatulokset.MockValintatulosService
import org.json4s.jackson._

class VastaanottoSpec extends HakemusApiSpecification with FixturePerson {
  override implicit lazy val appConfig = new AppConfig.IT
  sequential
  addServlet(componentRegistry.newApplicationsServlet, "/*")

  "POST /applications/vastaanota/:hakuOid/:hakemusOid" should {
    "vastaanottaa paikan" in {
      new ValintatulosFixtureImporter(componentRegistry.valintatulosService.asInstanceOf[MockValintatulosService]).applyFixtures("hyvaksytty")

      authPost("/applications/vastaanota/1.2.246.562.5.2013080813081926341928/1.2.246.562.11.00000441369", Serialization.write(ClientSideVastaanotto("1.2.246.562.5.72607738902", "VASTAANOTTANUT"))) {
        status must_== 200
      }
    }

    "hylkää pyynnön väärältä henkilöltä" in {
      new ValintatulosFixtureImporter(componentRegistry.valintatulosService.asInstanceOf[MockValintatulosService]).applyFixtures("hyvaksytty")

      authPost("/applications/vastaanota/1.2.246.562.5.2013080813081926341928/1.2.246.562.11.00000441369", Serialization.write(ClientSideVastaanotto("1.2.246.562.5.72607738902", "VASTAANOTTANUT"))) {
        status must_== 404
      }(PersonOid("WRONG PERSON"))
    }
  }
}