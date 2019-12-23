
angular.module('os.biospecimen.mobile', ['os.biospecimen.participant'])
  .config(function($stateProvider) {

    $stateProvider
      .state('mobile-uploads', {
        url: '/mobile-uploads',
        template: '<div ui-view></div>',
        controller: function() { },
        parent: 'cp-view',
        abstract: true
      })
      .state('mobile-upload-jobs', {
        url: '/list',
        templateUrl: 'modules/biospecimen/mobile/list.html',
        controller: 'MobileUploadJobsListCtrl',
        parent: 'mobile-uploads'
      })
      .state('mobile-upload-data', {
        url: '/upload-data',
        templateUrl: 'modules/biospecimen/mobile/upload.html',
        controller: 'MobileDataUploadCtrl',
        parent: 'mobile-uploads'
      });
  });
