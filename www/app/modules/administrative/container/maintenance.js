angular.module('os.administrative.container')
  .controller('ContainerMaintenanceCtrl', function(
    $scope, $modal, container, ScheduledContainerActivity, ContainerActivityLog, Util, AuthorizationService) {

    var lctx;
    function init() {
      $scope.ctx.showTree = false;
      $scope.$on('$destroy', function() { $scope.ctx.showTree = true });

      var permOpts = {resource: 'StorageContainer', operations: ['Update'], sites: [container.siteName]};
      lctx = $scope.mctx = {
        schedActivities: [],
        activityLogs: undefined,
        allowEdits: AuthorizationService.isAllowed(permOpts),
        exportDetail: { objectType: 'containerActivityLog', params: {containerId: container.id} }
      }

      loadScheduledActivities();
    }

    function loadScheduledActivities() {
      ScheduledContainerActivity.query({containerId: container.id, maxResults: 1000}).then(
        function(schedActivities) {
          lctx.schedActivities = schedActivities;
          angular.forEach(schedActivities, addUiProps);
        }
      );
    }

    function loadActivityLogs() {
      ContainerActivityLog.query({containerId: container.id}).then(
        function(activityLogs) {
          lctx.activityLogs = activityLogs;
        }
      ); 
    }

    function addUiProps(schedActivity) {
      schedActivity.$$uiProps = {
        cycleUnit: schedActivity.cycleIntervalUnit.toLowerCase().charAt(0),
        remUnit: schedActivity.reminderIntervalUnit.toLowerCase().charAt(0),
        task: {id: schedActivity.taskId, name: schedActivity.taskName}
      }
    }

    $scope.selectActivitiesLog = function() {
      if (lctx.activityLogs) {
        return;
      }

      loadActivityLogs();
    }

    $scope.addEditSchedActivity = function(toEdit) {
      $modal.open({
        templateUrl: 'modules/administrative/container/addedit-sched-activity.html',
        controller: 'AddEditContainerSchedActivity',
        resolve: {
          activity: function() {
            if (toEdit) {
              return angular.copy(toEdit);
            } else {
              return new ScheduledContainerActivity({containerId: container.id});
            }
          }
        }
      }).result.then(
        function(savedActivity) {
          addUiProps(savedActivity);

          if (toEdit) {
            var idx = lctx.schedActivities.findIndex(function(s) { return s.id == toEdit.id; });
            lctx.schedActivities[idx] = savedActivity;
          } else {
            lctx.schedActivities.push(savedActivity);
          }
        }
      );
    }

    $scope.archiveSchedActivity = function(activity) {
      var opts = {
        title:      'container.maintenance.confirm_archive_sched_activity_title',
        confirmMsg: 'container.maintenance.confirm_archive_sched_activity_q',
        input:      activity
      };

      Util.showConfirm(opts).then(
        function() {
          var toSave = angular.copy(activity);
          toSave.activityStatus = 'Closed';
          toSave.$saveOrUpdate().then(
            function() {
              var idx = lctx.schedActivities.indexOf(activity);
              lctx.schedActivities.splice(idx, 1);
            }
          );
        }
      );
    }

    $scope.addEditActivityLog = function(toEdit) {
      $modal.open({
        templateUrl: 'modules/administrative/container/addedit-activity-log.html',
        controller: 'AddEditContainerActivityLogCtrl',
        resolve: {
          activityLog: function() {
            if (toEdit) {
              toEdit = angular.copy(toEdit);
              toEdit.$$uiProps = {
                adHoc: !toEdit.scheduledActivityId,
                task: {id: toEdit.taskId, name: toEdit.taskName},
                activity: {id: toEdit.scheduledActivityId, name: toEdit.scheduledActivityName}
              };
              return toEdit;
            } else {
              return new ContainerActivityLog({
                containerId: container.id,
                '$$uiProps': {adHoc: true},
                activityDate: new Date().getTime()
              });
            }
          }
        }
      }).result.then(
        function(savedActivity) {
          if (toEdit) {
            var idx = lctx.activityLogs.findIndex(function(al) { return al.id == toEdit.id; });
            lctx.activityLogs[idx] = savedActivity;
          } else {
            lctx.activityLogs.unshift(savedActivity);
          }
        }
      );
    }

    $scope.archiveActivityLog = function(activity) {
      var opts = {
        title:      'container.maintenance.confirm_archive_activity_title',
        confirmMsg: 'container.maintenance.confirm_archive_activity_q',
        input:      activity
      };

      Util.showConfirm(opts).then(
        function() {
          var toSave = angular.copy(activity);
          toSave.activityStatus = 'Closed';
          toSave.$saveOrUpdate().then(
            function() {
              var idx = lctx.activityLogs.indexOf(activity);
              lctx.activityLogs.splice(idx, 1);
            }
          );
        }
      );
    }

    init();
  });
