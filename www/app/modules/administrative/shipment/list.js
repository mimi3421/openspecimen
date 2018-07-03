
angular.module('os.administrative.shipment.list', ['os.administrative.models'])
  .controller('ShipmentListCtrl', function(
    $scope, $state, $translate, Shipment, Site, Util, ListPagerOpts) {

    var pagerOpts;

    function init() {
      pagerOpts = $scope.pagerOpts = new ListPagerOpts({listSizeGetter: getShipmentsCount, recordsPerPage: 50});
      $scope.filterOpts = Util.filterOpts({maxResults: pagerOpts.recordsPerPage + 1, includeStats: true});

      loadShipments($scope.filterOpts);
      loadStatuses();
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

    function loadStatuses() {
      $scope.statuses = [ {name: 'Shipped'}, {name: 'Received'}, {name: 'Pending'} ];
      $translate('shipments.statuses.Shipped').then(
        function() {
          angular.forEach($scope.statuses,
            function(status) {
              status.caption = $translate.instant('shipments.statuses.' + status.name);
            }
          );
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
