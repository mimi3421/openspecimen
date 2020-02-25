angular.module('os.biospecimen.extensions')
  .config(function($stateProvider) {
    $stateProvider.state('patient-data-entry', {
      url: '/patient-data-entry?token',
      views: {
        'nav-buttons': {
          template: '<os-info></os-info>' +
                    '<li>' +
                    '  <a ng-click="login()"> <span translate="user.sign_in">Sign In</span> </a>' +
                    '</li>',

          controller: function($scope, $rootScope, $state, fdeToken) {
            $scope.login = function() {
              if (fdeToken.entityType == 'Participant' || fdeToken.entityType == 'CommonParticipant') {
                fdeToken.cprId = fdeToken.objectId;
                $rootScope.reqState = {name: 'participant-detail.extensions.list', params: fdeToken}
              }

              $state.go('login');
            }
          }
        },
        'app-body': {
          templateUrl: 'modules/biospecimen/extensions/patient-data-entry.html',
          controller: 'PatientDataEntryCtrl'
        }
      },
      resolve: {
        fdeToken: function($stateParams, Form) {
          return Form.getDataEntryToken($stateParams.token);
        }
      },
      parent: 'default'
    })
  })

  .controller('PatientDataEntryCtrl', function($scope, $http, $timeout, fdeToken, Form, Alerts) {

    var ctx;

    function init() {
      if (!fdeToken) {
        return;
      }

      $http.defaults.headers.common['X-OS-FDE-TOKEN'] = fdeToken.token;
      ctx = $scope.ctx = {
        fdeToken: fdeToken,
        saved: false
      }     

      Form.getDefinition(fdeToken.formId).then(
        function(formDef) {
          fdeToken.formDef = formDef;
        }
      );

      addSaveAndErrorHandler();
      $scope.$on('$destroy', cleanupToken);
    }

    function addSaveAndErrorHandler() {
      fdeToken.onSave  = function() {
        $timeout(function() {
          ctx.saved = true;
          cleanupToken();
        });
      }

      fdeToken.onError = function(data) {
        $timeout(function() {
          if (data.responseJSON instanceof Array) {
            Alerts.errorText(data.responseJSON.map(function(err) { return err.message + " (" + err.code + ")"; }))
          } else {
            Alerts.errorText("Unknown error: " + data.responseText);
          }
        });
      }
    }

    function cleanupToken() {
      delete $http.defaults.headers.common['X-OS-FDE-TOKEN'];
    }

    init();
  });
