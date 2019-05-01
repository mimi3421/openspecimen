angular.module('os.administrative.job',
  [ 
    'ui.router',
    'os.administrative.job.list',
    'os.administrative.job.runlog',
    'os.administrative.job.addedit'
  ])

  .config(function($stateProvider) {
    $stateProvider
      .state('job-root', {
        abstract: true,
        template: '<div ui-view></div>',
        controller: function($scope) {
          $scope.jobResource = {
            readOpts  : {resource: 'ScheduledJob', operations: ['Read']},
            createOpts: {resource: 'ScheduledJob', operations: ['Create']},
            updateOpts: {resource: 'ScheduledJob', operations: ['Update']},
            runOpts   : {resource: 'ScheduledJob', operations: ['Read']},
            deleteOpts: {resource: 'ScheduledJob', operations: ['Delete']}
          };
        },
        parent: 'signed-in'
      })
      .state('job-list', {
        url: '/jobs',
        templateUrl: 'modules/administrative/job/list.html',
        controller: 'JobListCtrl',
        parent: 'job-root'
      })
      .state('job-addedit', {
        url: '/job-addedit/:jobId?queryId',
        templateUrl: 'modules/administrative/job/addedit.html',
        controller: 'JobAddEditCtrl',
        resolve: {
          query: function($stateParams, SavedQuery) {
            if (!!$stateParams.jobId) {
              return null;
            }

            if ($stateParams.queryId) {
              return SavedQuery.getById($stateParams.queryId);
            }
          },

          job: function($stateParams, query, ScheduledJob) {
            if (!!$stateParams.jobId) {
              return ScheduledJob.getById($stateParams.jobId);
            }

            return new ScheduledJob({
              name: query && query.title,
              type: !query ? 'INTERNAL' : 'QUERY',
              savedQuery: query,
              repeatSchedule: 'ONDEMAND',
              recipients: [],
              startDate: new Date()
            })
          }
        },
        parent: 'job-root'
      })
      .state('job-run-log', {
        url: '/job-run-log/:jobId',
        templateUrl: 'modules/administrative/job/runlog.html',
        controller: 'JobRunLogCtrl',
        resolve: {
          job: function($stateParams, ScheduledJob) {
            return ScheduledJob.getById($stateParams.jobId);
          }
        },
        parent: 'job-root'
      })
  })

  .run(function(UrlResolver) {
    UrlResolver.regUrlState('job-run-log', 'job-run-log', 'jobId');
  });
