
angular.module('openspecimen')
  .factory('AuthService', function($http, $rootScope, $window, ApiUtil, ApiUrls) {
    var url = function() {
      return ApiUrls.getUrl('sessions');
    };

    return {
      authenticate: function(loginData) {
        return $http.post(url(), loginData).then(ApiUtil.processResp);
      },

      logout: function() {
        var q = $http.delete(url());
        this.removeToken();
        $rootScope.loggedIn = false;
        delete $rootScope.reqState;
        delete $rootScope.currentUser;
        return q.then(ApiUtil.processResp);
      },

      saveToken: function(token) {
        $window.localStorage['osAuthToken'] = token;
        $http.defaults.headers.common['X-OS-API-TOKEN'] = token;
        $http.defaults.withCredentials = true;
      },

      removeToken: function() {
        delete $window.localStorage['osAuthToken'];
        delete $http.defaults.headers.common['X-OS-API-TOKEN'];
        delete $http.defaults.headers.common['Authorization'];
      },

      refreshCookie: function() {
        return $http.post(url() + "/refresh-cookie").then(ApiUtil.processResp);
      }
    }
  })
  .controller('LoginCtrl', function(
    $scope, $rootScope, $state, $stateParams, $q, $http, $location, $injector, $window,
    Alerts, AuthDomain, AuthService) {

    function init() {
      $scope.errors     = [];
      $scope.loginData  = {};
      $scope.samlDomain = '';
      $scope.showSignIn = true;
      
      var logoutQ;
      if ($location.search().logout) {
        logoutQ = $scope.logout();
      }
 
      if ($http.defaults.headers.common['X-OS-API-TOKEN']) {
        if ($rootScope.reqState) {
          $state.go($rootScope.reqState.name, $rootScope.reqState.params);
        } else {
          $state.go('home');
        }
        //return;
      } else if (!$stateParams.directVisit && $injector.has('scCatalog')) {
        //
        // User not logged in
        //
        $q.when(logoutQ, gotoCatalog, gotoCatalog);
      }

      if ($stateParams.directVisit == 'true') {
        $rootScope.reqState = undefined;
      }

      loadDomains();
    }

    function gotoCatalog() {
      var catalogId = $injector.get('scCatalog').defCatalogId;
      if (catalogId) {
        $state.go('sc-catalog-dashboard', {catalogId: catalogId}, {location: 'replace'});
      }
    }

    function loadDomains() {
      $scope.domains = [];
      AuthDomain.query().then(
        function(domains) {
          $scope.samlDomain = domains.find(function(d) { return d.type == 'saml'; });

          var defaultDomain = $scope.global.appProps.default_domain;
          $scope.domains = domains;
          if (domains.length == 1) {
            $scope.loginData.domainName = domains[0].name;
          } else if (!!defaultDomain && domains.some(function(d) { return d.name == defaultDomain; })) {
            $scope.loginData.domainName = defaultDomain;
            if ($scope.samlDomain && $scope.samlDomain.name == defaultDomain) {
              gotoIdp();
            }
          }
        }
      );
    }

    function gotoIdp() {
      $scope.showSignIn = false;
      $window.location.replace('saml/login');
    }

    function onLogin(result) {
      $scope.loginError = false;

      if (result.status == "ok" && result.data) {
        $rootScope.currentUser = {
          id: result.data.id,
          firstName: result.data.firstName,
          lastName: result.data.lastName,
          loginName: result.data.loginName,
          admin: result.data.admin
        };
        $rootScope.loggedIn = true;
        AuthService.saveToken(result.data.token);
        if ($rootScope.reqState && $rootScope.state.name != $rootScope.reqState.name) {
          $state.go($rootScope.reqState.name, $rootScope.reqState.params);
          $rootScope.reqState = undefined;
        } else {
          $state.go('cp-list');
        }
      } else {
        $rootScope.currentUser = {};
        $rootScope.loggedIn = false;
        AuthService.removeToken();
        $scope.loginError = true;
      }
    };

    $scope.onDomainSelect = function(domain) {
      if (domain.type == 'saml') {
        gotoIdp();
      }
    }

    $scope.login = function() {
      $scope.errors = [];
      AuthService.authenticate($scope.loginData).then(
        onLogin,
        function(resp) {
          $scope.errors = (resp.data || []);
          Alerts.clear();
        }
      );
    }

    $scope.logout = function() {
      return AuthService.logout();
    }

    init();
  });
