angular.module('os.administrative.container')
  .controller('ContainerSpecimensCtrl', function(
    $scope, $state, $stateParams, container, currentUser, Util, CollectionProtocol,
    Container, SpecimensHolder, Alerts, CheckList, ListPagerOpts) {

    var filterOpts, pagerOpts, lctx;

    function init() {
      $scope.ctx.showTree = !$stateParams.filters;
      $scope.ctx.viewState = 'container-detail.specimens';

      pagerOpts = new ListPagerOpts({listSizeGetter: getSpecimensCount});
      filterOpts = Util.filterOpts({maxResults: pagerOpts.recordsPerPage + 1});

      lctx = $scope.lctx = {
        specimens: [],
        cps: [],
        containers: [],
        filterOpts: filterOpts,
        checkList: new CheckList([]),
        pagerOpts: pagerOpts,
        emptyState: {
          empty: true,
          loading: true,
          loadingMessage: 'specimens.loading_list',
          emptyMessage: 'specimens.empty_list'
        }
      };

      loadSpecimens($scope.lctx.filterOpts);
      Util.filter($scope, 'lctx.filterOpts', loadSpecimens);
    }

    function loadSpecimens(filterOpts) {
      lctx.emptyState.loading = true;
      container.getSpecimens(filterOpts).then(
        function(specimens) {
          lctx.emptyState.loading = false;
          lctx.emptyState.empty = specimens.length <= 0;

          angular.forEach(specimens,
            function(spmn) {
              spmn.$$cpCentric = spmn.ppid && spmn.ppid.indexOf('$$cp_reg_') == 0;
            }
          );

          lctx.pagerOpts.refreshOpts(specimens);
          lctx.specimens = specimens;
          lctx.checkList = new CheckList(specimens);
        }
      );
    }

    function loadCps(shortTitle) {
      var params = {query: shortTitle, repositoryName: container.siteName};
      CollectionProtocol.list(params).then(
        function(cps) {
          $scope.lctx.cps = cps;
        }
      );
    }

    function loadContainers(name) {
      container.getDescendantContainers({name: name}).then(
        function(containers) {
          $scope.lctx.containers = containers;
        }
      );
    }

    function createNewList(spmns) {
      SpecimensHolder.setSpecimens(spmns);
      $state.go('specimen-list-addedit', {listId: ''});
    }

    function getSpecimensCount() {
      return container.getSpecimensCount($scope.lctx.filterOpts);
    }

    $scope.toggleSearch = function() {
      $scope.ctx.showTree = !$scope.ctx.showTree;
    }

    $scope.downloadReport = function() {
      Util.downloadReport(container, "container.specimens");
    }

    $scope.loadSpecimens = function() {
      loadSpecimens($scope.lctx.filterOpts);
    };

    $scope.loadCps = loadCps;

    $scope.loadContainers = loadContainers;

    $scope.getSelectedSpecimens = function() {
      return $scope.lctx.checkList.getSelectedItems();
    }

    $scope.addSpecimensToList = function(list) {
      var items = $scope.lctx.checkList.getSelectedItems();
      if (!items || items.length == 0) {
        Alerts.error('container.specimens.no_specimens_for_specimen_list');
        return;
      }

      var spmns = items.map(function(item) { return {id: item.id}; });
      if (!list) {
        createNewList(spmns);
      } else {
        list.addSpecimens(spmns).then(
          function() {
            var type = list.getListType(currentUser);
            Alerts.success('specimen_list.specimens_added_to_' + type, list);
          }
        );
      }
    }

    $scope.pageSizeChanged = function() {
      filterOpts.maxResults = pagerOpts.recordsPerPage + 1;
    }

    init();
  });
