require("angular");
require('ng-resource')(window, angular);
require('angular-animate');
_ = require("underscore");
require("../lib/ui-bootstrap-custom-tpls-0.10.0.min.js");
var listApp = angular.module('listApp', ["ngResource", "ngAnimate", "ui.bootstrap.typeahead", "template/typeahead/typeahead-popup.html", "template/typeahead/typeahead-match.html"], function($locationProvider) {
  $locationProvider.html5Mode(true);
});

listApp.factory("applicationsResource", ["$resource", "$location", function($resource, $location) {
  return $resource("api/applications", null, {
    "update": {
      method: "PUT",
      url: "api/applications/:id"
    }
  });
}]);

listApp.factory("settings", ["$animate", function($animate) {
  var testMode = window.parent.location.href.indexOf("runner.html") > 0;
  if (testMode) $animate.enabled(false);

  return {
    uiTransitionTime: testMode ? 10 : 500
  };
}]);

var Hakutoive = function(json) {
  this.data = json
  this.isModified = false
  this.isNew = _.isEmpty(json)
}

Hakutoive.prototype = {
  toJson: function() {
    return this.data
  },

  clear: function() {
    this.data = {}
    this.isNew = true
    this.isModified = false
  },

  hasData: function() {
    return !_.isEmpty(this.data)
  },

  setOpetuspiste: function(id, name) {
    this.data["Opetuspiste"] = name
    this.data["Opetuspiste-id"] = id
    this.isModified = true
  },

  setKoulutus: function(koulutus) {
    this.data["Koulutus"] = koulutus.name.toString()
    this.data["Koulutus-id"] = koulutus.id.toString()
    this.data["Koulutus-educationDegree"] = koulutus.educationDegree.toString()
    this.data["Koulutus-id-sora"] = koulutus.sora.toString()
    this.data["Koulutus-id-aoIdentifier"] = koulutus.aoIdentifier.toString()
    this.data["Koulutus-id-kaksoistutkinto"] = koulutus.kaksoistutkinto.toString()
    this.data["Koulutus-id-vocational"] = koulutus.vocational.toString()
    this.data["Koulutus-id-educationcode"] = koulutus.educationCodeUri.toString()
    this.data["Koulutus-id-athlete"] = koulutus.athleteEducation.toString()
    this.isModified = true
  },

  hasOpetuspiste: function() {
    return !_.isEmpty(this.data["Opetuspiste-id"])
  },

  removeOpetuspisteData: function() {
    var self = this
    _.each(this.data, function(value, key) {
      if (key.indexOf("$")!==0 && key != "Opetuspiste")
        delete self.data[key]
    })
  },

  isValid: function() {
    if (!_.isEmpty(this.data["Opetuspiste"]))
      return !_.isEmpty(this.data["Koulutus-id"])
    else
      return true
  },

  setAsSaved: function() {
    this.isNew = false
    this.isModified = false
  },

  setAsModified: function() {
    this.isModified = true
  }
}

var Hakemus = function(json) {
  _.extend(this, json)
  this.hakutoiveet = _(this.hakutoiveet).map(function(hakutoive) { return new Hakutoive(hakutoive) })
}

Hakemus.prototype = {
  removePreference: function(index) {
    var row = this.hakutoiveet.splice(index, 1)[0]
  },

  addPreference: function(hakutoive) {
    this.hakutoiveet.push(hakutoive)
  },

  hasPreference: function(index) {
    return index >= 0 && index <= this.hakutoiveet.length-1 && this.hakutoiveet[index].hasData()
  },

  canMoveTo: function(from, to) {
    return this.hasPreference(from) && this.hasPreference(to);
  },

  toJson: function() {
    return _.extend({}, this, { hakutoiveet: _(this.hakutoiveet).map(function(hakutoive) { return hakutoive.toJson() })})
  },

  setAsSaved: function() {
    _(this.hakutoiveet).each(function(hakutoive) {
        if (hakutoive.hasData()) hakutoive.setAsSaved() }
    )
  },

  isValid: function() {
    return _(this.hakutoiveet).every(function(hakutoive) { return hakutoive.isValid() })
  },

  getHakutoiveWatchCollection: function() {
    return _(this.hakutoiveet).map(function(hakutoive) { return hakutoive.data })
  },

  moveHakutoive: function(from, to) {
    this.hakutoiveet[from].setAsModified()
    this.hakutoiveet[to].setAsModified()
    this.hakutoiveet.splice(to, 0, this.hakutoiveet.splice(from, 1)[0])
  },

  getChangedItems: function() {
    return _(this.hakutoiveet).chain()
      .map(function(hakutoive, index) { return hakutoive.isModified ? index : null })
      .without(null)
      .value()
  },

  isEditable: function(index) {
    var firstEditableIndex = _(this.hakutoiveet).filter(function(hakutoive) { return hakutoive.hasData() && hakutoive.isValid() }).length
    return index >= 0 && index < this.hakutoiveet.length && index <= firstEditableIndex
  }
}

listApp.controller("listCtrl", ["$scope", "applicationsResource", function ($scope, applicationsResource) {
  $scope.applicationStatusMessage = "Hakemuksia ladataan...";
  $scope.applicationStatusMessageType = "ajax-spinner";
  applicationsResource.query(success, error)

  function success(data) {
    $scope.applications = _.map(data, function(json) { return new Hakemus(json) })
    $scope.applicationStatusMessage = "";
    $scope.applicationStatusMessageType = "";
  }

  function error(err) {
    switch (err.status) {
      case 401: $scope.applicationStatusMessage = "Tietojen lataus epäonnistui: et ole kirjautunut sisään."; break;
      default: $scope.applicationStatusMessage = "Tietojen lataus epäonnistui. Yritä myöhemmin uudelleen.";
    }
    $scope.applicationStatusMessageType = "error"
    $scope.applications = [];
  }
}]);

listApp.controller("hakutoiveCtrl", ["$scope", "$http", "$timeout", "settings", function($scope, $http, $timeout, settings) {
  $scope.isEditingDisabled = function() { return !$scope.hakutoive.isNew || !$scope.application.isEditable($scope.$index) }

  $scope.isKoulutusSelectable = function() { return !$scope.isEditingDisabled() && this.hakutoive.hasOpetuspiste() && !_.isEmpty($scope.koulutusList) }

  $scope.isLoadingKoulutusList = function() { return !$scope.isEditingDisabled() && this.hakutoive.hasOpetuspiste() && _.isEmpty($scope.koulutusList) }

  $scope.opetuspisteValittu = function($item, $model, $label) {
    this.hakutoive.setOpetuspiste($item.id, $item.name)

    findKoulutukset(this.application.haku.oid, $item.id, this.application.educationBackground)
      .then(function(koulutukset) { $scope.koulutusList = koulutukset; })
  }

  $scope.opetuspisteModified = function() {
    if (_.isEmpty(this.hakutoive.data.Opetuspiste))
      this.hakutoive.clear()
    else
      this.hakutoive.removeOpetuspisteData()
  }

  $scope.removeHakutoive = function(index) {
    $scope.application.removePreference(index)

    $timeout(function() {
      $scope.application.addPreference(new Hakutoive({}))
    }, settings.uiTransitionTime)
  }

  $scope.canRemovePreference = function(index) {
    if (index === 0)
      return $scope.application.hasPreference(1)
    else
      return $scope.application.hasPreference(index)
  }

  $scope.koulutusValittu = function(index) {
    this.hakutoive.setKoulutus(this["valittuKoulutus"])
  }

  function findKoulutukset(applicationOid, opetuspisteId, educationBackground) {
    return $http.get("https://testi.opintopolku.fi/ao/search/" + applicationOid + "/" + opetuspisteId, {
      params: {
        baseEducation: educationBackground.baseEducation,
        vocational: educationBackground.vocational,
        uiLang: "fi" // TODO: kieliversio
      }
    }).then(function(res){
      return res.data;
    });
  }

  $scope.findOpetuspiste = function(val) {
    return $http.get('https://testi.opintopolku.fi/lop/search/' + val, {
      params: {
        asId: '1.2.246.562.5.2014022711042555034240'
      }
    }).then(function(res){
      return res.data;
    });
  };
}])

listApp.controller("questionsCtrl", ["$scope", "$element", "$http", function ($scope, $element, $http) {
  $scope.$watch("application.getHakutoiveWatchCollection()", function(hakutoiveet, oldHakutoiveet) {
    // Skip initial values angular style
    var application = $scope.application

    var responsePromise = $http.post("api/applications/validate/" + application.oid, application.toJson());
    responsePromise.success(function(data, status, headers, config) {
      $scope.questions = data.questions
    })
  }, true)

}])

listApp.controller("hakemusCtrl", ["$scope", "$element", "$http", "applicationsResource", function ($scope, $element, $http, applicationsResource) {
  $scope.hasChanged = false
  $scope.isSaving = false
  $scope.isValid = true

  $scope.$watch("application.getHakutoiveWatchCollection()", function(hakutoiveet, oldHakutoiveet) {
    // Skip initial values angular style
    if (!_.isEqual(hakutoiveet, oldHakutoiveet)) {
      $scope.hasChanged = true
      setSaveMessage("")

      $scope.isValid = $scope.application.isValid()
      if (!$scope.isValid)
        setSaveMessage("Täytä kaikki tiedot", "error");
    }
  }, true)

  function setSaveMessage(msg, type) {
    $scope.saveMessage = msg
    $scope.saveMessageType = type
  }

  $scope.movePreference = function(from, to) {
    if (to >= 0 && to < this.application.hakutoiveet.length) {
      this.application.moveHakutoive(from, to)
      setSaveMessage()
    }
  }

  $scope.saveApplication = function() {
    $scope.isSaving = true;
    applicationsResource.update({id: $scope.application.oid }, $scope.application.toJson(), onSuccess, onError)

    function onSuccess() {
      $scope.$emit("highlight-items", $scope.application.getChangedItems());
      $scope.application.setAsSaved();
      $scope.isSaving = false;
      $scope.hasChanged = false
      setSaveMessage("Kaikki muutokset tallennettu", "success");
    }

    function onError(err) {
      switch (err.status) {
        case 401: setSaveMessage("Tallentaminen epäonnistui, sillä istunto on vanhentunut. Kirjaudu uudestaan sisään.", "error"); break
        default: setSaveMessage("Tallentaminen epäonnistui", "error")
      }

      $scope.isSaving = false
      console.log(err)
    }
  };
}]);

listApp.directive('sortable', ["settings", function(settings) {
  return function($scope, $element, attrs) {
    var slide = function(el, offset) {
      el.css("transition", "all 0.5s");
      el.css("transform", "translate3d(0px, " + offset + "px, 0px)");
    };

    var moveDown = function(el) {
      slide(el, el.outerHeight());
    };

    var moveUp = function(el) {
      slide(el, -el.outerHeight());
    };

    var resetSlide = function(el) {
      el.css({
        "transition": "",
        "transform": ""
      });
    };

    var switchPlaces = function(element1, element2) {
      if (element1.index() < element2.index()) {
        moveDown(element1);
        moveUp(element2);
      } else {
        moveUp(element1);
        moveDown(element2);
      }

      setTimeout(function() {
        $scope.$apply(function(self) {
          self[attrs.sortableMoved](element1.index(), element2.index());
          resetSlide(element1);
          resetSlide(element2);
        });
      }, settings.uiTransitionTime);
    };

    var arrowClicked = function(elementF) {
      return function(evt) {
        var btn = $(evt.target);
        if (!btn.hasClass("disabled")) {
          var element1 = btn.closest("li");
          var element2 = element1[elementF]();
          switchPlaces(element1, element2);
        }
      };
    };

    $element.on("click", ".sort-arrow-down", arrowClicked("next"));
    $element.on("click", ".sort-arrow-up", arrowClicked("prev"));
  };
}]);

listApp.directive("highlightItems", function() {
  return function($scope, $element) {
    $scope.$on("highlight-items", function(event, indexes) {
      var items = $element.find(".preference-list-item")
      _.each(indexes, function(index) {
        items.eq(index).addClass("saved")
      })

      window.setTimeout(function() {
        items.removeClass("saved");
      }, 3000);
    });
  };
});

listApp.directive("confirm", function () {
  return {
    scope: {
      callback : '&confirmAction'
    },
    link: function (scope, element, attrs) {
      function cancel() {
        element.removeClass("confirm");
        element.text(originalText);
        element.off(".cancelConfirm");
        $("body").off(".cancelConfirm");
      }

      var originalText = element.text();

      element.on("click", function() {
        if (element.hasClass("confirm")) {
          scope.$apply(scope.callback);
          cancel();
        } else {
          element.hide()
          element.addClass("confirm");
          element.text(attrs.confirmText);
          $("body").one("click.cancelConfirm", cancel);
          element.one("mouseout.cancelConfirm", cancel);
          element.fadeIn(100)
        }
        return false;
      });
    }
  };
});