angular.module('os.administrative.dp')
  .controller('HomeDpListCtrl', function($scope, DistributionProtocol) {
    function init() {
      $scope.dps = [];
      DistributionProtocol.query().then(
        function(dps) {
          $scope.dps = dps;
        }
      );
    }

    init();
  });
