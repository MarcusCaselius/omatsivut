(function () {
  describe('hakemuslistaus', function () {
    before(function (done) {
      session.init("010101-123N").then(ApplicationListPage().resetDataAndOpen).done(done)
    })

    describe("näyttäminen", function() {
      it('hakemuslistassa on hakemus henkilölle 010101-123N', function () {
        expect(ApplicationListPage().applications()).to.deep.equal([
          {
            applicationSystemName: "Perusopetuksen jälkeisen valmistavan koulutuksen kesän 2014 haku MUOKATTU"
          }
        ])
      })

      it("henkilön 010101-123N hakutoiveet ovat näkyvissä", function () {
        expect(preferencesAsText()).to.deep.equal([
          {
            "hakutoive.Opetuspiste": "Amiedu, Valimotie 8",
            "hakutoive.Koulutus": "Maahanmuuttajien ammatilliseen peruskoulutukseen valmistava koulutus"
          },
          {
            "hakutoive.Opetuspiste": "Ammatti-instituutti Iisakki",
            "hakutoive.Koulutus": "Kymppiluokka"
          },
          {
            "hakutoive.Opetuspiste": "Turun Kristillinen opisto",
            "hakutoive.Koulutus": "Kymppiluokka"
          }
        ]);
      })

      it("tyhjiä rivejä ei voi muokata", function () {
        _.each(ApplicationListPage().emptyPreferencesForApplication(0), function (row) {
          row.isDisabled().should.be.true
        })
      })
    })

    describe("hakemuslistauksen muokkaus", function () {
      describe("järjestys", function() {
        it("järjestys muuttuu nuolta klikkaamalla", function (done) {
          endToEndTest(function () {
            var pref2 = getPreference(1)
            var pref3 = getPreference(2)
            pref2.arrowDown().click()
            return wait.until(function () {
              return pref2.element().index() === 2 && pref3.element().index() === 1 && pref2.number().text() === "3."
            })()
          }, function (dbStart, dbEnd) {
            dbStart.hakutoiveet[1].should.deep.equal(dbEnd.hakutoiveet[2])
            dbStart.hakutoiveet[2].should.deep.equal(dbEnd.hakutoiveet[1])
            done()
          })
        })
      })
      describe("poisto", function() {
        it("hakutoiveen voi poistaa", function (done) {
          endToEndTest(function () {
            var pref1 = getPreference(0)
            var pref2 = getPreference(1)
            pref1.deleteBtn().click().click()
            return wait.until(function () {
              return pref2.element().index() === 0 && pref1.element().parent().length === 0
            })()
          }, function (dbStart, dbEnd) {
            dbEnd.hakutoiveet.should.deep.equal(_.flatten([_.rest(dbStart.hakutoiveet), {}]))
            done()
          })
        })
      })
    })
  })

  function endToEndTest(manipulationFunction, dbCheckFunction) {
    function save() {
      return wait.until(ApplicationListPage().saveButton(0).isEnabled(true))()
        .then(ApplicationListPage().saveButton(0).click)
        .then(wait.until(ApplicationListPage().saveButton(0).isEnabled(false)))
    }

    var sameArgs = function (f) {
      return function (val) {
        return f().then(function () {
          return val;
        })
      }
    }

    db.getApplications()
      .then(sameArgs(manipulationFunction))
      .then(sameArgs(save))
      .then(function (old) {
        return db.getApplications().done(function (val) {
          dbCheckFunction(old[0], val[0])
        })
      })
  }

  function preferencesAsText() {
    return ApplicationListPage().preferencesForApplication(0).map(function (item) {
      return item.data()
    })
  }

  function getPreference(index) {
    return ApplicationListPage().preferencesForApplication(0)[index]
  };
})()