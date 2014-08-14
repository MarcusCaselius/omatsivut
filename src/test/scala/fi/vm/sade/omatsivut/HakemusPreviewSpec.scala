package fi.vm.sade.omatsivut

import fi.vm.sade.omatsivut.fixtures.TestFixture._
import fi.vm.sade.omatsivut.fixtures.{FixtureImporter, TestFixture}
import fi.vm.sade.omatsivut.servlet.ApplicationsServlet

class HakemusPreviewSpec extends HakemusApiSpecification {
  override implicit lazy val appConfig = new AppConfig.IT

  sequential

  "GET /applications/preview/:oid" should {
    "generate application preview" in {
      FixtureImporter().applyOverrides("peruskoulu")
      authGet("/applications/preview/" + TestFixture.hakemus2, personOid) {
        response.getContentType() must_== "text/html; charset=UTF-8"

        body must contain("""<label>Vastaanotettu</label><span>25.6.2014 15:52</span>""")
        body must contain("""<label>Hakemusnumero</label><span>00000441368</span>""")

        // henkilötiedot
        body must contain("""<div class="question"><label>Sukunimi</label><span class="answer">Testaaja</span>""")
        body must contain("""<div class="question"><label>Äidinkieli</label><span class="answer">suomi</span>""")
        // koulutustausta
        body must contain("""<div class="question"><label>Valitse tutkinto, jolla haet koulutukseen</label><span class="answer">Perusopetuksen oppimäärä</span>""")
        // hakutoiveet
        body must contain("""<li class="preference-row"><span class="index">1</span><span class="learning-institution"><label>Opetuspiste</label><span>Kallion lukio</span></span><span class="education"><label>Koulutus</label><span>Lukion ilmaisutaitolinja</span></span></li>""")
        // lupatiedot
        body must contain("""<label>Minulle saa lähettää postia ja sähköpostia vapaista opiskelupaikoista ja muuta koulutusmarkkinointia.</label><span class="answer">Ei</span>""")
        // harkinnanvarainen haku liitepyynnöt
        body must contain("""<div>SALO</div><div>Liitteiden viimeinen palautuspäivä 14.3.2014</div>""")
      }
    }
    "support higher grade attachements" in {
      authGet("/applications/preview/" + TestFixture.hakemusWithHigherGradeAttachments, personOid) {
        println(prettyPrintHtml(body))
        // hakutoiveet
        body must contain("""<p>Toimita todistuskopiot muista perusteistasi seuraaviin kouluihin</p><div class="group"><h3>Diakonia-ammattikorkeakoulu, Helsingin toimipiste</h3><div>Sturenkatu 2</div>""")
      }
    }

    "support additional questions per preference" in {
      authGet("/applications/preview/" + TestFixture.hakemus3, personOid) {
        println(prettyPrintHtml(body))
        // hakutoiveet
        body must contain("""<div class="question"><label>Haetko urheilijan ammatilliseen koulutukseen?</label><span class="answer">Kyllä</span></div><div class="question"><label>Haluaisitko suorittaa lukion ja/tai ylioppilastutkinnon samaan aikaan kuin ammatillisen perustutkinnon?</label><span class="answer">Kyllä</span></div>""")
      }
    }

    "support grade grid" in {
      authGet("/applications/preview/" + TestFixture.hakemusWithGradeGrid, personOid) {
        // hakutoiveet
        body must contain("""<tr><td id="PK_A1_column1">A1-kieli</td><td id="PK_A1_column2">englanti</td><td id="PK_A1_column3">9</td><td id="PK_A1_column4">Ei arvosanaa</td><td id="PK_A1_column5">Ei arvosanaa</td></tr>""")
        body must contain("""<tr><td id="PK_MA_column1" colspan="2">Matematiikka</td><td id="PK_MA_column3">9</td><td id="PK_MA_column4">Ei arvosanaa</td><td id="PK_MA_column5">Ei arvosanaa</td></tr>""")
      }
    }

    "support grade grid from grade 10" in {
      FixtureImporter().applyOverrides("kymppiluokka")
      authGet("/applications/preview/" + TestFixture.hakemus2, personOid) {
        // hakutoiveet
        body must contain("""<tr><td id="PK_B1_column1">B1-kieli</td><td id="PK_B1_column2">englanti</td><td id="PK_B1_column3">10(9)</td><td id="PK_B1_column4">Ei arvosanaa</td><td id="PK_B1_column5">Ei arvosanaa</td></tr>""")
        body must contain("""<tr><td id="PK_MA_column1" colspan="2">Matematiikka</td><td id="PK_MA_column3">10(9)</td><td id="PK_MA_column4">Ei arvosanaa</td><td id="PK_MA_column5">Ei arvosanaa</td></tr>""")
      }
    }
  }

  private def prettyPrintHtml(content: String) = {
    val prettier = new scala.xml.PrettyPrinter(80, 4)
    prettier.format(scala.xml.XML.loadString(content))
  }

  addServlet(new ApplicationsServlet(), "/*")
}