
angular.module('os.administrative.job.addedit', ['os.administrative.models'])
  .controller('JobAddEditCtrl', function($scope, $state, $translate, SavedQuery, currentUser, job) {
    
    var defSavedQueries = undefined;

    function init() {
      job.startDate = job.startDate || new Date();
      job.rtArgsProvided = job.rtArgsProvided || false;

      $scope.weekDays = getWeekDays();
      $scope.job =  job;
      $scope.savedQueries = [];
      if (job.type == 'QUERY') {
        onQueryTypeSelect();
      }
    }

    function getWeekDays() {
      var weekDays = [
        {name: 'SUNDAY'}, {name: 'MONDAY'}, {name: 'TUESDAY'}, {name: 'WEDNESDAY'},
        {name: 'THURSDAY'}, {name: 'FRIDAY'}, {name: 'SATURDAY'}
      ];

      $translate('job.week_days.SUNDAY').then(
        function(wd) {
          angular.forEach(weekDays,
            function(wd) {
              wd.caption = $translate.instant('jobs.week_days.' + wd.name);
            }
          );
        }
      );

      return weekDays;
    }

    function loadSavedQueries(searchTerm) {
      if (!searchTerm && defSavedQueries) {
        $scope.savedQueries = defSavedQueries;
        return;
      }

      SavedQuery.query({searchString: searchTerm}).then(
        function(savedQueries) {
          $scope.savedQueries = savedQueries;
          if (!searchTerm) {
            defSavedQueries = savedQueries;
          }
        }
      );
    }

    function onQueryTypeSelect() {
      job.runAs = currentUser;
      loadSavedQueries();
    }

    $scope.selectQueryType = onQueryTypeSelect;

    $scope.loadSavedQueries = loadSavedQueries;

    $scope.saveOrUpdateJob = function() {
      $scope.job.$saveOrUpdate().then(
        function(result) {
          $state.go('job-list');
        }
      );
    }
    
    init();
  });
