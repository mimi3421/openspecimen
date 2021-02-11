
angular.module('os.administrative.order')
  .controller('HomeOrderListCtrl', function($scope, DistributionOrder) {
    function init() {
      $scope.orders = [];
      DistributionOrder.query().then(
        function(orders) {
          $scope.orders = orders;
        }
      );
    }

    init();
  });
