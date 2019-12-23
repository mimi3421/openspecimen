
angular.module('os.biospecimen.mobile')
  .factory('MobileUploadJob', function(osModel) {
    var MobileUploadJob = osModel('mobile-app/upload-jobs');

    return MobileUploadJob;
  });
