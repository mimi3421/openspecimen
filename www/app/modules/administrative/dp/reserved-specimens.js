angular.module('os.administrative.dp')
  .controller('DpReservedSpecimensCtrl', function(
    $scope, $state, distributionProtocol, 
    Util, CollectionProtocol, CheckList, ListPagerOpts, SpecimensHolder) {

    var lctx, filterOpts, pagerOpts;

    function init() {
      pagerOpts = new ListPagerOpts({listSizeGetter: getSpecimensCount});
      filterOpts = Util.filterOpts({maxResults: pagerOpts.recordsPerPage + 1});

      lctx = $scope.lctx = {
        specimens: [],
        cps: [],
        filterOpts: filterOpts,
        checkList: new CheckList([]),
        pagerOpts: pagerOpts,
        orderCreateOpts: {resource: 'Order', operations: ['Create']}
      };

      loadSpecimens(lctx.filterOpts);
      Util.filter($scope, 'lctx.filterOpts', loadSpecimens);
    }

    function loadSpecimens(filterOpts) {
      distributionProtocol.getReservedSpecimens(filterOpts).then(
        function(specimens) {
          lctx.specimens = specimens;
          lctx.checkList = new CheckList(specimens);
          lctx.pagerOpts.refreshOpts(specimens);
        }
      );
    }

    function loadCps(shortTitle) {
      var params = {query: shortTitle};
      CollectionProtocol.list(params).then(
        function(cps) {
          lctx.cps = cps;
        }
      );
    }

    function getSpecimensCount() {
      return distributionProtocol.getReservedSpecimensCount(lctx.filterOpts);
    }

    $scope.loadCps = loadCps;

    $scope.distributeSpecimens = function() {
      SpecimensHolder.setSpecimens(lctx.checkList.getSelectedItems());
      $state.go('order-addedit', {dpId: distributionProtocol.id});
    }

    $scope.cancelReservation = function() {
      var payload = {
        dpId: distributionProtocol.id,
        specimens: lctx.checkList.getSelectedItems().map(function(spmn) { return {id: spmn.id}; }),
        cancelOp: true
      }

      distributionProtocol.reserveSpecimens(payload).then(
        function() {
          loadSpecimens(lctx.filterOpts);
        }
      )
    }

    $scope.pageSizeChanged = function() {
      filterOpts.maxResults = pagerOpts.recordsPerPage + 1;
    }

    init();
  });
