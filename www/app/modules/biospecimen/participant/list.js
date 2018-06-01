
angular.module('os.biospecimen.participant.list', ['os.biospecimen.models'])
  .controller('ParticipantListCtrl', function($scope, $state, cp, twoStepReg, PluginReg) {

    var ctrl = this;

    function init() {
      $scope.cpId = cp.id;
      $scope.ctx = {
        params: {
          listName: 'participant-list-view',
          objectId: cp.id
        }
      };

      angular.extend($scope.listViewCtx, {
        twoStepReg: twoStepReg,
        listName: 'participant.list',
        ctrl: ctrl,
        headerButtonsTmpl: 'modules/biospecimen/participant/register-button.html',
        headerActionsTmpl: 'modules/biospecimen/participant/list-pager.html',
        showPrimaryBtnDd: !!cp.bulkPartRegEnabled || (PluginReg.getTmpls('participant-list', 'primary-button').length > 0)
      });
    }

    $scope.showParticipant = function(row) {
      $state.go('participant-detail.overview', {cprId: row.hidden.cprId});
    };

    $scope.setListCtrl = function(listCtrl) {
      $scope.ctx.listCtrl = listCtrl;
      $scope.listViewCtx.showSearch = listCtrl.haveFilters;
      $scope.listViewCtx.pagerOpts  =  listCtrl.pagerOpts;
    }

    init();
  });
