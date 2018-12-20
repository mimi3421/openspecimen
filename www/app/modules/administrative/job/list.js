
angular.module('os.administrative.job.list', ['os.administrative.models'])
  .controller('JobListCtrl', function($scope, $modal, $translate, Util, ScheduledJob, DeleteUtil, Alerts, ListPagerOpts) {

    var pagerOpts, filterOpts;

    function init() {
      $scope.jobs = [];
      pagerOpts = $scope.pagerOpts = new ListPagerOpts({listSizeGetter: getJobsCount});
      filterOpts = $scope.filterOpts = {query: undefined, maxResults: pagerOpts.recordsPerPage + 1};

      loadTypes();
      loadJobs(filterOpts);
      Util.filter($scope, 'filterOpts', loadJobs);
    }

    function loadJobs(filterOpts) {
      ScheduledJob.query(filterOpts).then(
        function(jobs) {
          $scope.jobs = jobs;
          pagerOpts.refreshOpts(jobs);
        }
      );
    }

    function runJob(job, args) {
      args = args || {};
      job.executeJob(args).then(
        function() {
          Alerts.success("jobs.queued_for_exec", job);
        }
      );
    }

    function getJobsCount() {
      return ScheduledJob.getCount($scope.filterOpts);
    }

    function loadTypes() {
      $scope.types = [];
      $translate('jobs.types.INTERNAL').then(
        function() {
          $scope.types = ['INTERNAL', 'EXTERNAL', 'QUERY'].map(
            function(type) {
              return {type: type, caption: $translate.instant('jobs.types.' + type)}
            }
          );
        }
      );
    }
    
    $scope.executeJob = function(job) {
      if (!job.rtArgsProvided) {
        runJob(job);
        return;
      }

      $modal.open({
        templateUrl: 'modules/administrative/job/args.html',
        controller: function($scope, $modalInstance) {
          $scope.job = job;
          $scope.args = {};

          $scope.ok = function() {
            $modalInstance.close($scope.args); 
          }

          $scope.cancel = function() {
            $modalInstance.dismiss('cancel');
          }
        }
      }).result.then(
        function(args) {
          runJob(job, args);
        }
      );
    }


    $scope.deleteJob = function(job) {
      DeleteUtil.confirmDelete({
        templateUrl: 'modules/administrative/job/confirm-delete.html',
        deleteWithoutCheck: true,
        entity: job,
        delete: function() {
          job.$remove().then(
            function() {
              var idx = $scope.jobs.indexOf(job);
              $scope.jobs.splice(idx, 1);
            }
          );
        }
      });
    }

    init();
  });
