angular.module('os.administrative.container.list', ['os.administrative.models'])
  .controller('ContainerListCtrl', function($scope, $state, Container, Util, DeleteUtil, ListPagerOpts, CheckList) {

    var pagerOpts, filterOpts;

    function init() {
      pagerOpts = $scope.pagerOpts = new ListPagerOpts({listSizeGetter: getContainersCount});
      filterOpts = $scope.containerFilterOpts = Util.filterOpts({
        maxResults: pagerOpts.recordsPerPage + 1,
        includeStats: true,
        topLevelContainers: true
      });

      $scope.ctx = {
        exportDetail: { objectType: 'storageContainer' }
      }

      loadContainers($scope.containerFilterOpts);
      Util.filter($scope, 'containerFilterOpts', loadContainers, ['maxResults', 'includeStats', 'topLevelContainers']);
    }

    function loadContainers(filterOpts) {
      Container.list(filterOpts).then(
        function(containers) {
          pagerOpts.refreshOpts(containers);

          angular.forEach(containers,
            function(container) {
              if (container.capacity) {
                container.utilisation = Math.round(container.storedSpecimens / container.capacity * 100);
              }
            }
          );

          $scope.containerList = containers;
          $scope.ctx.checkList = new CheckList(containers);
        }
      );
    }

    function getContainerIds(containers) {
      return containers.map(function(container) { return container.id; });
    }

    function getContainersCount() {
      return Container.getCount($scope.containerFilterOpts);
    }

    $scope.showContainerDetail = function(container) {
      $state.go('container-detail.locations', {containerId: container.id});
    };

    $scope.deleteContainers = function() {
      var containers = $scope.ctx.checkList.getSelectedItems();

      var opts = {
        confirmDelete:  'container.delete_containers',
        successMessage: 'container.containers_deleted',
        onBulkDeletion: function() {
          loadContainers($scope.containerFilterOpts);
        }
      }

      DeleteUtil.bulkDelete({bulkDelete: Container.bulkDelete}, getContainerIds(containers), opts);
    }

    $scope.pageSizeChanged = function() {
      filterOpts.maxResults = pagerOpts.recordsPerPage + 1;
    }

    init();
  });
