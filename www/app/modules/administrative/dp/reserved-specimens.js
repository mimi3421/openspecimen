angular.module('os.administrative.dp')
  .controller('DpReservedSpecimensCtrl', function($scope, $state, distributionProtocol, Specimen, SpecimensHolder) {

    var lctx;

    function init() {
      lctx = $scope.lctx = {
        params: {
          listName: 'reserved-specimens-list-view',
          objectId: distributionProtocol.id
        },
        emptyState: {
          loadingMessage: 'specimens.loading_list',
          emptyMessage: 'specimens.empty_list'
        },
        orderCreateOpts: {resource: 'Order', operations: ['Create']}
      };
    }

    function loadSpecimens() {
      lctx.listCtrl.loadList();
    }

    $scope.distributeSpecimens = function() {
      var spmnIds = lctx.listCtrl.getSelectedItems().map(function(spmn) { return spmn.hidden.specimenId });
      Specimen.getByIds(spmnIds).then(
        function(spmns) {
          SpecimensHolder.setSpecimens(spmns);
          $state.go('order-addedit', {dpId: distributionProtocol.id});
        }
      );
    }

    $scope.cancelReservation = function() {
      var payload = {
        dpId: distributionProtocol.id,
        specimens: lctx.listCtrl.getSelectedItems().map(function(spmn) { return {id: spmn.hidden.specimenId } }),
        cancelOp: true
      };
      distributionProtocol.reserveSpecimens(payload).then(loadSpecimens);
    }

    $scope.setListCtrl = function(listCtrl) {
      lctx.listCtrl = listCtrl;
      lctx.showSearch = listCtrl.haveFilters;
    }

    init();
  });
