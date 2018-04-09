
angular.module('os.administrative.shipment.list', ['os.administrative.models'])
  .controller('ShipmentListCtrl', function(
    $scope, $state, Shipment, Site, Util, ListPagerOpts) {

    var pagerOpts;

    function init() {
      pagerOpts = $scope.pagerOpts = new ListPagerOpts({listSizeGetter: getShipmentsCount, recordsPerPage: 50});
      $scope.filterOpts = Util.filterOpts({maxResults: pagerOpts.recordsPerPage + 1, includeStat: true});

      loadShipments($scope.filterOpts);
      Util.filter($scope, 'filterOpts', loadShipments);
    }

    function loadShipments(filterOpts) {
      Shipment.query(filterOpts).then(
        function(result) {
          $scope.shipments = result;
          pagerOpts.refreshOpts(result);
        }
      );
    }
 
    function getShipmentsCount() {
      return Shipment.getCount($scope.filterOpts);
    }

    $scope.showShipmentOverview = function(shipment) {
      $state.go('shipment-detail.overview', {shipmentId: shipment.id});
    };
    
    $scope.onInstituteSelect = function() {
      $scope.filterOpts.recvSite = undefined;
    }

    init();
  });
