angular.module('os.administrative.shipment')
  .controller('ShipmentSpecimensCtrl', function($scope, shipment, ShipmentUtil) {
  
    var ctx = {
      totalItems: 0,
      currPage: 1,
      itemsPerPage: 50,
      shipmentSpmns: [],
      loading: false
    };

    function init() {
      $scope.ctx = ctx;
      loadSpecimens(); 
      $scope.$watch('ctx.currPage', loadSpecimens);
    }

    function loadSpecimens() {
      var startAt     = (ctx.currPage - 1) * ctx.itemsPerPage;
      var maxResults  = ctx.itemsPerPage + 1;
      ctx.loading = true;
      shipment.getSpecimens(startAt, maxResults).then(
        function(shipmentSpmns) {
          ctx.totalItems = (ctx.currPage - 1) * ctx.itemsPerPage + shipmentSpmns.length;
          if (shipmentSpmns.length >= maxResults) {
            shipmentSpmns.splice(shipmentSpmns.length - 1, 1);
          }

          ctx.shipmentSpmns = shipmentSpmns;
          ctx.loading = false;
          angular.extend(ctx, ShipmentUtil.hasPpidAndExtIds(shipmentSpmns));
        }
      );
    }

    init();
  });
