angular.module('os.administrative.container')
  .controller('ContainerSpecimensCtrl', function(
    $scope, $state, $stateParams, container, currentUser, Util, CollectionProtocol,
    Container, SpecimensHolder, Alerts, CheckList, ListPagerOpts) {

    var filterOpts, pagerOpts;

    function init() {
      $scope.ctx.showTree = !$stateParams.filters;
      $scope.ctx.viewState = 'container-detail.specimens';

      pagerOpts = new ListPagerOpts({listSizeGetter: getSpecimensCount});
      filterOpts = Util.filterOpts({maxResults: pagerOpts.recordsPerPage + 1});

      $scope.lctx = {
        specimens: [],
        cps: [],
        containers: [],
        filterOpts: filterOpts,
        checkList: new CheckList([]),
        pagerOpts: pagerOpts
      };

      loadSpecimens($scope.lctx.filterOpts);
      Util.filter($scope, 'lctx.filterOpts', loadSpecimens);
    }

    function loadSpecimens(filterOpts) {
      container.getSpecimens(filterOpts).then(
        function(specimens) {
          angular.forEach(specimens,
            function(spmn) {
              spmn.$$cpCentric = spmn.ppid && spmn.ppid.indexOf('$$cp_reg_') == 0;
            }
          );

          $scope.lctx.pagerOpts.refreshOpts(specimens);
          $scope.lctx.specimens = specimens;
          $scope.lctx.checkList = new CheckList(specimens);
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
