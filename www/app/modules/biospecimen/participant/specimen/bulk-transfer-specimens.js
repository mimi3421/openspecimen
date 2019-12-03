angular.module('os.biospecimen.specimen')
  .controller('BulkTransferSpecimensCtrl', function($scope, SpecimensHolder, Specimen, Alerts) {
    function init() {
      var specimens = SpecimensHolder.getSpecimens() || [];
      SpecimensHolder.setSpecimens(null);

      var transferTime = new Date().getTime();
      angular.forEach(specimens, function(spmn) {
        spmn.oldLocation = spmn.storageLocation;
        spmn.storageLocation = {};
        spmn.transferTime = transferTime;
      });

      $scope.specimens = specimens;
    }

    $scope.removeSpecimen = function(index) {
      $scope.specimens.splice(index, 1);
    }

    $scope.copyFirstLocationToAll = function() {
      var location = $scope.specimens[0].storageLocation;
      location = !location ? {} : {name: location.name, mode: location.mode};
      for (var i = 1; i < $scope.specimens.length; i++) {
        $scope.specimens[i].storageLocation = angular.extend({}, location);
      }
    }

    $scope.copyFirstTimeToAll = function() {
      var transferTime = $scope.specimens[0].transferTime;
      for (var i = 1; i < $scope.specimens.length; ++i) {
        $scope.specimens[i].transferTime = transferTime;
      }
    }

    $scope.copyFirstCommentsToAll = function() {
      var comments = $scope.specimens[0].transferComments;
      for (var i = 1; i < $scope.specimens.length; ++i) {
        $scope.specimens[i].transferComments = comments;
      }
    }

    $scope.transferSpecimens = function() {
      var specimens = $scope.specimens.map(
        function(spmn) {
          return {
            id: spmn.id,
            storageLocation: spmn.storageLocation,
            transferComments: spmn.transferComments,
            transferTime: spmn.transferTime
          };
        }
      );

      Specimen.bulkUpdate(specimens).then(
        function() {
          Alerts.success('specimens.bulk_transfer.success', {spmnsCount: specimens.length});
          $scope.back();
        }
      );
    }

    init();
  });
