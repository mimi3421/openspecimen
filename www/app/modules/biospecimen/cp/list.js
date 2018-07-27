
angular.module('os.biospecimen.cp.list', ['os.biospecimen.models'])
  .controller('CpListCtrl', function(
    $scope, $state, cpList, CollectionProtocol, Util, DeleteUtil,
    PvManager, CheckList, ListPagerOpts, AuthorizationService) {

    var pagerOpts, filterOpts;

    function init() {
      pagerOpts  = $scope.pagerOpts    = new ListPagerOpts({listSizeGetter: getCpCount});
      filterOpts = $scope.cpFilterOpts = Util.filterOpts({maxResults: pagerOpts.recordsPerPage + 1});

      $scope.allowReadJobs = AuthorizationService.isAllowed($scope.participantResource.createOpts) ||
        AuthorizationService.isAllowed($scope.participantResource.updateOpts) ||
        AuthorizationService.isAllowed($scope.specimenResource.updateOpts);

      $scope.ctx = {};
      setList(cpList);
      Util.filter($scope, 'cpFilterOpts', loadCollectionProtocols);
    }

    function setList(list) {
      $scope.cpList = list;
      $scope.ctx.checkList = new CheckList(list);
      pagerOpts.refreshOpts(list);
    }

    function getCpCount() {
      return CollectionProtocol.getCount(filterOpts);
    }

    function loadCollectionProtocols() {
      CollectionProtocol.list(filterOpts).then(
        function(cpList) {
          setList(cpList);
        }
      );
    };

    function getCpIds(cps) {
      return cps.map(function(cp) { return cp.id; });
    }

    $scope.showCpSummary = function(cp) {
      $state.go('cp-summary-view', {cpId: cp.id});
    };

    $scope.deleteCps = function() {
      var cps = $scope.ctx.checkList.getSelectedItems();

      var opts = {
        confirmDelete:  'cp.delete_cps',
        successMessage: 'cp.cps_deleted',
        pendingMessage: 'cp.cps_delete_pending',
        onBulkDeletion: loadCollectionProtocols,
        askReason:      true
      }

      DeleteUtil.bulkDelete({bulkDelete: CollectionProtocol.bulkDelete}, getCpIds(cps), opts);
    }

    $scope.pageSizeChanged = function() {
      filterOpts.maxResults = pagerOpts.recordsPerPage + 1;
    }

    init();
  });
