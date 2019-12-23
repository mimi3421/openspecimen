
angular.module('os.biospecimen.mobile')
  .controller('MobileDataUploadCtrl', function($scope, $state, $sce, cp, MobileUploadJob, Alerts) {
    var ctx;

    function init() {
      ctx = $scope.ctx = {
        uploader: {},
        uploadUrl: $sce.trustAsResourceUrl(MobileUploadJob.url() + '?cpId=' + cp.id)
      }
    }
    
    $scope.upload = function(event) {
      event.preventDefault();
      ctx.uploader.submit().then(
        function(jobDetail) {
          alert("uploaded: " + jobDetail.jobId);
          $state.go('mobile-upload-jobs', {cpId: cp.id});
        }
      );
    }

    $scope.cancel = function(event) {
      event.preventDefault();
      $scope.back();
    }

    init();
  });
