module.exports = function(app) {
    app.controller('HakutoiveidenMuokkausController', function($scope, $location, $http) {
        var matches = $location.path().match(/token\/(.+)/)
        var token = matches && matches[1]

        $scope.lang = 'fi' // Todo

        if (token) {
            $location.path('')
            $http.get('http://weerfdsfinsecure/applications/application/' + token)
                .success(function(hakemus) {
                    $scope.hakemus = hakemus
                })
                .error(function(response) {
                    $scope.error = angular.extend({}, response)
                })
        }
        else {
            // TODO: redirect to help page?
        }
    })
}