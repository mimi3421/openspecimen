
angular.module('os.biospecimen.specimen.overview', ['os.biospecimen.models'])
  .controller('SpecimenOverviewCtrl', function(
    $scope, $rootScope, hasDict, specimen, showEvents,
    osRightDrawerSvc, ExtensionsUtil) {

    function init() {
      if (hasDict) {
        ExtensionsUtil.createExtensionFieldMap(specimen);
      }

      $scope.spmnCtx = {
        obj: {specimen: specimen},
        inObjs: ['specimen'],
        exObjs: ['specimen.events'],
        watcher: ['specimen.collectionEvent.user', 'specimen.receivedEvent.user'],
        showEvents: showEvents
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

      if (showEvents) {
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

    $scope.toggleShowEvents = function() {
      $scope.spmnCtx.showEvents = !$scope.spmnCtx.showEvents;
    }

    init();
  });
