
angular.module('os.biospecimen.specimen.close', ['os.biospecimen.models'])
  .controller('SpecimenCloseCtrl', function($scope, $modalInstance, Specimen, specimens, Alerts) {
    function init() {
      $scope.closeSpec = { reason: '' };
    }

    function bulkClose() {
      var statusSpecs = [];
      angular.forEach(specimens,
        function(specimen) {
          var statusSpec = {status: 'Closed', reason: $scope.closeSpec.reason, id: specimen.id};
          statusSpecs.push(statusSpec);
        }
      );

      Specimen.bulkStatusUpdate(statusSpecs).then(
        function(result) {
          Alerts.success('specimens.specimens_closed');
          $modalInstance.close(result);
        }
      );
    }

    $scope.close = function() {
      bulkClose();
    }

    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    }

    init();
  });
