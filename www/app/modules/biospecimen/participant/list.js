
angular.module('os.biospecimen.participant.list', ['os.biospecimen.models'])
  .controller('ParticipantListCtrl', function(
    $scope, $state, osRightDrawerSvc, cp, participantListCfg, twoStepReg,
    PluginReg, Util, ListPagerOpts) {

    var ctrl = this;

    var pagerOpts, listParams;

    function init() {
      pagerOpts  = new ListPagerOpts({listSizeGetter: getParticipantsCount});
      listParams = {listName: 'participant-list-view', maxResults: pagerOpts.recordsPerPage + 1};

      $scope.cpId = cp.id;

      $scope.ctx = {
        filtersCfg: angular.copy(participantListCfg.filters),
        filters: Util.filterOpts({}),
        participants: {},
        listSize: -1,
        pagerOpts: pagerOpts
      };

      angular.extend($scope.listViewCtx, {
        twoStepReg: twoStepReg,
        listName: 'participant.list',
        ctrl: ctrl,
        headerButtonsTmpl: 'modules/biospecimen/participant/register-button.html',
        headerActionsTmpl: 'modules/biospecimen/participant/list-pager.html',
        showSearch: (participantListCfg.filters && participantListCfg.filters.length > 0),
        showPrimaryBtnDd: !!cp.bulkPartRegEnabled || (PluginReg.getTmpls('participant-list', 'primary-button').length > 0)
      });

      Util.filter($scope, 'ctx.filters', loadParticipants);
    }

    function loadParticipants() {
      var params = angular.extend({}, listParams);
      if (pagerOpts.$$pageSizeChanged > 0) {
        params.includeCount = false;
      }

      cp.getListDetail(params, getFilters()).then(
        function(participants) {
          $scope.ctx.participants = participants;
          if (params.includeCount) {
            $scope.ctx.listSize = participants.size;
          }

          pagerOpts.refreshOpts(participants.rows);
          if (participants.rows.length > 12 && $scope.listViewCtx.showSearch) {
            osRightDrawerSvc.open();
          }
        }
      );
    }

    function getParticipantsCount() {
      if (!listParams.includeCount) {
        listParams.includeCount = true;
        return cp.getListSize(listParams, getFilters()).then(
          function(size) {
            $scope.ctx.listSize = size;
            return {count: size};
          }
        );
      } else {
        return {count: $scope.ctx.listSize};
      }
    }

    function getFilters() {
      var filters = [];
      if ($scope.ctx.$listFilters) {
        filters = $scope.ctx.$listFilters.getFilters();
      }

      return filters;
    }

    $scope.showParticipant = function(row) {
      $state.go('participant-detail.overview', {cprId: row.hidden.cprId});
    };

    $scope.loadFilterValues = function(expr) {
      return cp.getExpressionValues(listParams.listName, expr);
    }

    $scope.setListCtrl = function($list) {
      $scope.ctx.$list = $list;
    }

    $scope.setFiltersCtrl = function($listFilters) {
      $scope.ctx.$listFilters = $listFilters;
      loadParticipants();
    }

    $scope.pageSizeChanged = function(newPageSize) {
      listParams.maxResults = pagerOpts.recordsPerPage + 1;
      loadParticipants();
    }

    this.pagerOpts = function() {
      return pagerOpts;
    }

    init();
  });
