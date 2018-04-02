angular.module('os.biospecimen.specimenlist')
  .controller('SpecimenListSpecimensCtrl', function(
    $scope, $state, $stateParams, $timeout, $filter, $injector, currentUser, list,
    SpecimensHolder, SpecimenList, CollectionProtocol, Container, DeleteUtil, Alerts, Util) {

    function init() { 
      $scope.specimenUpdateOpts = {resource: 'VisitAndSpecimen', operations: ['Update']};
      $scope.orderCreateOpts    = {resource: 'Order', operations: ['Create']};

      $scope.ctx = {
        list: list,
        spmnsInView: [],
        filterOpts: Util.filterOpts({}),
        filterPvs: {init: false},
        selection: {all: false, any: false, specimens: []},
        url: SpecimenList.url(),
        breadcrumbs: $stateParams.breadcrumbs,
        reqBasedDistOrShip: false
      }

      if ($injector.has('spmnReqCfgUtil')) {
        $injector.get('spmnReqCfgUtil').isReqBasedDistOrShippingEnabled().then(
          function(result) {
            $scope.ctx.reqBasedDistOrShip = (result.value == 'true');
          }
        );
      }

      $scope.$on('osRightDrawerOpen', initFilterPvs);

      $scope.pagingOpts = {
        totalSpmns: 0,
        currPage: 1,
        spmnsPerPage: 100,
        recordsPerPage: 100
      };

      Util.filter($scope, 'ctx.filterOpts', loadSpecimens);
      $scope.$watch('pagingOpts.currPage', function() {
        loadSpecimens();
      });
    }

    function loadSpecimens(filterOpts) {
      if (filterOpts) {
        $scope.pagingOpts = {
          totalSpmns: 0,
          currPage: 1,
          spmnsPerPage: $scope.pagingOpts.spmnsPerPage,
          recordsPerPage: $scope.pagingOpts.recordsPerPage
        };
      }

      var pagingOpts = $scope.pagingOpts;
      var startAt = (pagingOpts.currPage - 1) * pagingOpts.spmnsPerPage;
      var maxResults = pagingOpts.spmnsPerPage + 1;

      var queryParams = angular.extend({startAt: startAt, maxResults: maxResults}, $scope.ctx.filterOpts);
      $scope.ctx.list.getSpecimens(queryParams).then(
        function(specimens) {
          pagingOpts.totalSpmns = (pagingOpts.currPage - 1) * pagingOpts.spmnsPerPage + specimens.length;
          if (specimens.length >= maxResults) {
            specimens.splice(specimens.length - 1, 1);
          }

          $scope.ctx.spmnsInView = specimens;
          $scope.ctx.selection = {all: false, any: false, specimens: []};
        }
      );
    };

    function initFilterPvs() {
      if ($scope.ctx.filterPvs.init) {
        return;
      }

      $scope.ctx.filterPvs.init = true;
      $scope.ctx.filterPvs.lineages = ['New', 'Aliquot', 'Derivative'];
      loadCpList();
      loadContainerList();
    }

    function loadCpList(name) {
      if (!$scope.ctx.filterPvs.defCpList || (!!name && $scope.ctx.filterPvs.defCpList.length >= 100)) {
        var opts = {detailedList: false};
        if (!!name) {
          opts.query = name;
        }

        CollectionProtocol.list(opts).then(
          function(cpList) {
            $scope.ctx.filterPvs.cpList = cpList;
            if (!name) {
              $scope.ctx.filterPvs.defCpList = cpList;
            }
          }
        );
      } else {
        $scope.ctx.filterPvs.cpList = $scope.ctx.filterPvs.defCpList;
      }
    }

    // containers are invariably more than 100
    // so no point in trying to optimise the load
    function loadContainerList(name) {
      var opts = {topLevelContainers: false};
      if (!!name) {
        opts.name = name;
      }

      Container.list(opts).then(
        function(containerList) {
          $scope.ctx.filterPvs.containerList = containerList;
        }
      );
    }

    function removeSpecimensFromList() {
      var list = $scope.ctx.list;
      list.removeSpecimens($scope.ctx.selection.specimens).then(
        function(listSpecimens) {
          var type = list.getListType(currentUser);
          Alerts.success('specimen_list.specimens_removed_from_' + type, list);
          $scope.ctx.selection.all = false;
          loadSpecimens();
        }
      );
    }

    function gotoView(state, params, msgCode) {
      if (!$scope.ctx.selection.any) {
        Alerts.error('specimen_list.' + msgCode);
        return;
      }

      SpecimensHolder.setSpecimens($scope.ctx.selection.specimens);
      $state.go(state, params);
    }

    $scope.loadSpecimens = loadSpecimens;

    $scope.getSelectedSpecimens = function() {
      return $scope.ctx.selection.specimens;
    }

    $scope.addChildSpecimens = function() {
      var list = $scope.ctx.list;
      list.addChildSpecimens().then(
        function() {
          Alerts.success('specimen_list.child_specimens_added');
          loadSpecimens();
        }
      );
    }

    $scope.sortByRel = function() {
      $scope.ctx.list.getSpecimenSortedByRel().then(
        function(listSpmns) {
          $scope.ctx.spmnsInView = listSpmns;
          $scope.pagingOpts.totalSpmns = listSpmns.length;
        }
      );
    }

    $scope.viewSpecimen = function(specimen) {
      $state.go('specimen', {specimenId: specimen.id});
    }

    $scope.toggleAllSpecimenSelect = function(event) {
      event.preventDefault();

      $scope.ctx.selection.all = !$scope.ctx.selection.all;
      $scope.ctx.selection.any = $scope.ctx.selection.all;
      if (!$scope.ctx.selection.all) {
        $scope.ctx.selection.specimens = [];
      } else {
        $scope.ctx.selection.specimens = [].concat($scope.ctx.spmnsInView);
      }

      angular.forEach($scope.ctx.spmnsInView,
        function(specimen) {
          specimen.selected = $scope.ctx.selection.all;
        }
      );
    }

    $scope.toggleSpecimenSelect = function (event, specimen) {
      event.preventDefault();

      specimen.selected = !specimen.selected;
      var specimens = $scope.ctx.selection.specimens;
      if (specimen.selected) {
        specimens.push(specimen);
      } else {
        var idx = specimens.indexOf(specimen);
        if (idx != -1) {
          specimens.splice(idx, 1);
        }
      }

      $scope.ctx.selection.all = (specimens.length == $scope.ctx.spmnsInView.length);
      $scope.ctx.selection.any = (specimens.length > 0);
    };

    $scope.confirmRemoveSpecimens = function () {
      if (!$scope.ctx.selection.any) {
        Alerts.error("specimen_list.no_specimens_for_deletion");
        return;
      }

      var listType = list.getListType(currentUser);
      DeleteUtil.confirmDelete({
        entity: list,
        props: {messageKey: 'specimen_list.confirm_remove_specimens_from_' + listType},
        templateUrl: 'modules/biospecimen/specimen-list/confirm-remove-specimens.html',
        delete: removeSpecimensFromList
      });
    }

    $scope.searchCp = function(name) {
      loadCpList(name);
    }

    $scope.searchContainer = function(name) {
      loadContainerList(name);
    }

    $scope.distributeCart = function() {
      $state.go('order-addedit', {specimenListId: list.id});
    }

    $scope.clearFilters = function() {
      $scope.ctx.filterOpts = {};
    }

    $scope.pageSizeChanged = function() {
      $scope.pagingOpts.spmnsPerPage = $scope.pagingOpts.recordsPerPage;
      if ($scope.pagingOpts.currPage == 1) {
        loadSpecimens();
      } else {
        $scope.pagingOpts.currPage = 1;
      }
    }

    init();
  });
