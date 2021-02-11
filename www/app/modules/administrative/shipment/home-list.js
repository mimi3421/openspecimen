
angular.module('os.administrative.shipment')
  .controller('HomeShipmentListCtrl', function($scope, Shipment) {
    function init() {
      $scope.shipments = [];
      Shipment.query().then(
        function(shipments) {
          $scope.shipments = shipments;
        }
      );
    }

    init();
  });
