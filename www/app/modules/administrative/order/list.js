
angular.module('os.administrative.order.list', ['os.administrative.models'])
  .controller('OrderListCtrl', function(
    $scope, $state,
    DistributionOrder, DistributionProtocol, Util, ListPagerOpts) {

    var pvsLoaded = false;
    var pagerOpts;

    function init() {
      $scope.orders = [];
      $scope.dps = [];
      pagerOpts = $scope.pagerOpts = new ListPagerOpts({listSizeGetter: getOrdersCount, recordsPerPage: 50});
      $scope.filterOpts = Util.filterOpts({maxResults: pagerOpts.recordsPerPage + 1});

      loadOrders($scope.filterOpts);
      Util.filter($scope, 'filterOpts', loadOrders);
    }

    function loadOrders(filterOpts) {
      DistributionOrder.list(filterOpts).then(
        function(orders) {
          $scope.orders = orders;
          pagerOpts.refreshOpts(orders);
        }
      );
    }


    function loadSearchPvs() {
      if (pvsLoaded) {
        return;
      }

      loadDps();
      pvsLoaded = true;
    }

    function loadDps(title) {
      DistributionProtocol.query({query: title}).then(
        function(dps) {
          $scope.dps = dps;
        }
      );
    }
 
    function getOrdersCount() {
      return DistributionOrder.getOrdersCount($scope.filterOpts);
    }
    
    $scope.loadSearchPvs = loadSearchPvs;

    $scope.loadDps = loadDps;

    $scope.showOrderOverview = function(order) {
      $state.go('order-detail.overview', {orderId: order.id});
    }

    init();
  });
