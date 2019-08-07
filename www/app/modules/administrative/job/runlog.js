
angular.module('os.administrative.job.runlog', ['os.administrative.models'])
  .controller('JobRunLogCtrl', function($scope, $translate, job, Util) {

    var ctx;

    function init() {
      ctx = $scope.ctx = {
        job: job,
        runs: [],
        filters: {},
        statuses: [
          {name: 'IN_PROGRESS', caption: ''},
          {name: 'SUCCEEDED', caption: ''},
          {name: 'FAILED', caption: ''}
        ]
      };

      $translate('jobs.statuses.SUCCEEDED').then(
        function() {
          angular.forEach(ctx.statuses,
            function(s) {
              s.caption = $translate.instant('jobs.statuses.' + s.name);
            }
          );
        }
      );

      loadRuns();
      Util.filter($scope, 'ctx.filters', loadRuns);
    }

    function loadRuns() {
      job.getRuns(ctx.filters).then(
        function(runs) {
          ctx.runs = runs;
        }
      );
    }
    
    init();
  });
