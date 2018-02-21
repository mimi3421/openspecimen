
angular.module('os.biospecimen.specimen.overview', ['os.biospecimen.models'])
  .controller('SpecimenOverviewCtrl', function(
    $scope, $rootScope, hasDict, cpr, visit, specimen, showSpmnActivity,
    osRightDrawerSvc, ExtensionsUtil) {

    function init() {
      if (hasDict) {
        ExtensionsUtil.createExtensionFieldMap(specimen);
      }

      $scope.spmnCtx = {
        obj: {cpr: cpr, visit: visit, specimen: specimen},
        inObjs: ['specimen', 'calcSpecimen',],
        exObjs: ['specimen.events'],
        watcher: ['specimen.collectionEvent.user', 'specimen.receivedEvent.user'],
        showActivity: showSpmnActivity
      }

      loadActivities();

      $scope.$watch('specimen.activityStatus', function(newVal, oldVal) {
        if (newVal == oldVal) {
          return;
        }

        if (newVal == 'Closed') {
          loadActivities();
        }
      });

      if (showSpmnActivity) {
        osRightDrawerSvc.open();
      }
    }

    function loadActivities() {
      $scope.activities = [];
      specimen.getEvents(
        function(activities) {
          $scope.activities = activities.map(
            function(activity) {
              return angular.extend({global: $rootScope.global}, activity);
            }
          );
        }
      );
    };

    $scope.toggleShowActivity = function() {
      $scope.spmnCtx.showActivity = !$scope.spmnCtx.showActivity;
    }

    init();
  });
