angular.module('os.administrative.shipment')
  .controller('ShipmentContainersCtrl', function($scope, shipment) {
  
    var ctx = {
      totalItems: 0,
      currPage: 1,
      itemsPerPage: 25,
      shipmentContainers: [],
      loading: false
    };

    function init() {
      $scope.ctx = ctx;
      loadContainers(); 
      $scope.$watch('ctx.currPage', loadContainers);
    }

    function loadContainers() {
      var startAt     = (ctx.currPage - 1) * ctx.itemsPerPage;
      var maxResults  = ctx.itemsPerPage + 1;
      ctx.loading = true;
      shipment.getContainers(startAt, maxResults).then(
        function(shipmentContainers) {
          ctx.totalItems = (ctx.currPage - 1) * ctx.itemsPerPage + shipmentContainers.length;
          if (shipmentContainers.length >= maxResults) {
            shipmentContainers.splice(shipmentContainers.length - 1, 1);
          }

          ctx.shipmentContainers = shipmentContainers;
          ctx.loading = false;
        }
      );
    }

    init();
  });