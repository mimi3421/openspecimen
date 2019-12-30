angular.module('os.administrative.containertype.list', ['os.administrative.models'])
  .controller('ContainerTypeListCtrl', function($scope, $state, CheckList, ContainerType, Util, DeleteUtil, ListPagerOpts) {

    var pagerOpts, ctx;

    function init() {
      pagerOpts = $scope.pagerOpts = new ListPagerOpts({listSizeGetter: getContainerTypesCount});
      $scope.containerTypeFilterOpts = {maxResults: pagerOpts.recordsPerPage + 1};
      ctx = $scope.ctx = {
        exportDetail: {objectType: 'storageContainerType'},
        emptyState: {
          loading: true,
          empty: true,
          loadingMessage: 'container_type.loading_list',
          emptyMessage: 'container_type.empty_list'
        }
      };

      loadContainerTypes($scope.containerTypeFilterOpts);
      Util.filter($scope, 'containerTypeFilterOpts', loadContainerTypes);
    }

    function loadContainerTypes(filterOpts) {
      ctx.emptyState.loading = true;
      ContainerType.query(filterOpts).then(
        function(containerTypes) {
          ctx.emptyState.loading = false;
          ctx.emptyState.empty = (containerTypes.length <= 0);
          pagerOpts.refreshOpts(containerTypes);

          $scope.containerTypes = containerTypes;
          $scope.ctx.checkList = new CheckList(containerTypes);
          if (Object.keys(filterOpts).length == 0) {
            $scope.canHolds = angular.copy(containerTypes);
          }
        }
      );
    };

    function getContainerTypesCount() {
      return ContainerType.getCount($scope.containerTypeFilterOpts);
    }

    function getTypeIds(types) {
      return types.map(function(t) { return t.id; });
    }

    $scope.deleteTypes = function() {
      var types = $scope.ctx.checkList.getSelectedItems();
      var opts = {
        confirmDelete:  'container_type.delete_types',
        successMessage: 'container_type.types_deleted',
        onBulkDeletion: function() {
          loadContainerTypes($scope.containerTypeFilterOpts);
        }
      }

      DeleteUtil.bulkDelete({bulkDelete: ContainerType.bulkDelete}, getTypeIds(types), opts);
    }


    $scope.showContainerTypeOverview = function(containerType) {
      $state.go('container-type-detail.overview', {containerTypeId: containerType.id});
    };

    init();
  });

