angular.module('os.administrative.order')
  .controller('OrderItemsCtrl', function($scope, order, PluginReg, CommentsUtil, CheckList, DistributionLabelPrinter) {
  
    var ctx = {
      totalItems: 0,
      currPage: 1,
      itemsPerPage: 100,
      items: [],
      checkList: new CheckList([]),
      loading: false,
      showRetrieveBtn: false,
      itemFieldsHdrTmpls:  PluginReg.getTmpls('order-detail', 'item-fields-header', ''),
      itemFieldsCellTmpls: PluginReg.getTmpls('order-detail', 'item-fields-overview', '')
    };

    function init() {
      $scope.ctx = ctx;
      loadOrderItems(); 
      showOrHideRetrieve();
      $scope.$watch('ctx.currPage', loadOrderItems);
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

    function loadOrderItems(fromStart) {
      //
      // if pending order is created using specimen list, show specimen list link
      //
      if (order.status === 'PENDING' && !!order.specimenList) {
        return;
      }

      if (fromStart) {
        ctx.currPage = 1;
      }

      var startAt     = (ctx.currPage - 1) * ctx.itemsPerPage;
      var maxResults  = ctx.itemsPerPage + 1;
      var queryParams = {startAt: startAt, maxResults: maxResults};
      ctx.loading = true;
      order.getOrderItems(queryParams).then(
        function(orderItems) {
          ctx.totalItems = (ctx.currPage - 1) * ctx.itemsPerPage + orderItems.length;
          if (orderItems.length >= maxResults) {
            orderItems.splice(orderItems.length - 1, 1);
          }

          ctx.items = orderItems;
          ctx.checkList = new CheckList(orderItems);
          ctx.loading = false;

          var i = 0;
          ctx.showLocations = false, ctx.showDistLabels = false;
          for (var i = 0; i < orderItems.length; ++i) {
            var item = orderItems[i];

            if (!ctx.showLocations) {
              ctx.showLocations = !!item.specimen.storageLocation && !!item.specimen.storageLocation.name;
            }

            if (!ctx.showDistLabels) {
              ctx.showDistLabels = !!item.label;
            }

            if (ctx.showLocations && ctx.showDistLabels) {
              break;
            }
          }

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
              loadOrderItems(true);
              ctx.showRetrieveBtn = false;
            }
          );
        }
      );
    }

    $scope.printLabels = function() {
      var items = ctx.checkList.getSelectedItems();
      if (!items || items.length == 0) {
        alert("No order items selected");
        return;
      }

      var itemIds = items.map(function(item) { return item.id; });
      DistributionLabelPrinter.printLabels({orderId: order.id, itemIds: itemIds}, order.name + '.csv');
    }

    init();
  });
