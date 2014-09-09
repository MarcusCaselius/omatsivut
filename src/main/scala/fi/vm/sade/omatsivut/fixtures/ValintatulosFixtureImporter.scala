package fi.vm.sade.omatsivut.fixtures

import fi.vm.sade.omatsivut.config.AppConfig
import AppConfig.AppConfig
import fi.vm.sade.omatsivut.valintatulokset.{Valintatulos, MockValintatulosService}

case class ValintatulosFixtureImporter(implicit val appConfig: AppConfig) {
  def applyFixtures(fixtureName: String = "") {
    val fixture = JsonFixtureMaps.findByKey[List[Valintatulos]]("/mockdata/valintatulokset.json", fixtureName).getOrElse(Nil)
    appConfig.componentRegistry.valintatulosService.asInstanceOf[MockValintatulosService].useFixture(fixture)
  }
}