angular.module('os.administrative.order')
  .controller('OrderItemsCtrl', function($scope, $state, order, CommentsUtil, DistributionLabelPrinter) {
  
    var ctx;

    function init() {
      ctx = $scope.ctx = {
        showRetrieveBtn: false,
        params: {
          listName: 'order-specimens-list-view',
          objectId: order.id,
          hideEmptyColumns: true
        }
      };
      showOrHideRetrieve();
    }

    function showOrHideRetrieve() {
      ctx.showRetrieveBtn = false;
      if (order.status != 'EXECUTED') {
        return;
      }

      order.getOrderItems({maxResults: 1, storedInDistributionContainer: true}).then(
        function(items) {
          ctx.showRetrieveBtn = (items.length > 0);
        }
      );
    }

    $scope.retrieveSpecimens = function() {
      var opts = {
        header: 'specimen_list.retrieve_specimens', headerParams: {},
        placeholder: 'specimen_list.retrieve_reason',
        button: 'specimen_list.retrieve_specimens'
      };

      CommentsUtil.getComments(opts,
        function(comments) {
          order.retrieveSpecimens({comments: comments}).then(
            function(count) {
              ctx.listCtrl.loadList();
              ctx.showRetrieveBtn = false;
            }
          );
        }
      );
    }

    $scope.printLabels = function() {
      var itemIds = ctx.listCtrl.getSelectedItems().map(function(item) { return item.hidden.orderItemId_ });
      if (itemIds.length == 0) {
        alert("No order items selected");
        return;
      }

      DistributionLabelPrinter.printLabels({orderId: order.id, itemIds: itemIds}, order.name + '.csv');
    }

    $scope.setListCtrl = function(listCtrl) {
      ctx.listCtrl = listCtrl;
      ctx.showSearch = listCtrl.haveFilters;
    }

    $scope.showSpecimen = function(row) {
      $state.go('specimen', {specimenId: row.hidden.specimenId});
    }

    init();
  });
