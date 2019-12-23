
angular.module('os.common.import.list', ['os.common.import.importjob'])
  .controller('MobileUploadJobsListCtrl', function($scope, cp, MobileUploadJob) {

    var ctx;

    function init() {
      ctx = $scope.ctx = {
        jobs: [],
        pagingOpts: {
          totalJobs: 0,
          currPage: 1,
          jobsPerPage: 25
        }
      };

      $scope.$watch('ctx.pagingOpts.currPage', function() { loadJobs(ctx.pagingOpts); });
    };

    function loadJobs(pagingOpts) {
      var startAt = (pagingOpts.currPage - 1) * pagingOpts.jobsPerPage;
      var maxResults = pagingOpts.jobsPerPage + 1;

      var queryParams = {cpId: cp.id, startAt: startAt, maxResults: maxResults};
      MobileUploadJob.query(queryParams).then(
        function(jobs) {
          pagingOpts.totalJobs = (pagingOpts.currPage - 1) * pagingOpts.jobsPerPage + jobs.length;
 
          if (jobs.length >= maxResults) {
            jobs.splice(jobs.length - 1, 1);
          }

          ctx.jobs = jobs;
          angular.forEach(jobs,
            function(job) {
              job.outputFileUrl = MobileUploadJob.url() + job.$id() + '/report';
            }
          );
        }
      );
    };

    init();
  });
