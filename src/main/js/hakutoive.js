function Hakutoive(json) {
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

module.exports = Hakutoive