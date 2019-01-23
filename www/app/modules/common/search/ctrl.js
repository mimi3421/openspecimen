
angular.module('os.common.search.ctrl', [])
  .controller('QuickSearchCtrl', function($scope, $state, $timeout, $window, SettingUtil, QuickSearchSvc) {

    var ctx, closeHandler;

    function init() {
      ctx = $scope.ctx = {searching: false, matches: []};
    }

    function searchManual(searchTerm) {
      SettingUtil.getSetting('training', 'manual_search_link').then(
        function(setting) {
          $window.open(setting.value + searchTerm);
        }
      );
    }


    $scope.search = function(keyword, selectCtrl) {
      if (!keyword) {
        ctx.matches = undefined;
        ctx.selectedMatch = undefined;

        if (!closeHandler) {
          ctx.selectCtrl = selectCtrl;
          closeHandler = ctx.selectCtrl.close;
          ctx.selectCtrl.close = function(s) {
            $timeout(function() {
              ctx.searching = false;
              return closeHandler(s);
            });
          };
        }

        return;
      }

      QuickSearchSvc.search(keyword).then(
        function(result) {
          ctx.matches = result;
        }
      );
    }

    $scope.onMatchSelect = function() {
      var match = ctx.selectedMatch;
      if (match.id == 0) {
        searchManual(match.key);
        return;
      }

      var state = QuickSearchSvc.getState(match.entity);
      var stateParams = {stateName: state, objectName: match.entity, key: 'id', value: match.entityId};
      $state.go('object-state-params-resolver', stateParams);

      ctx.selectedMatch = undefined;
      ctx.matches = [];
      ctx.searching = false;
    }

    $scope.activateSearch = function() {
      ctx.searching = true;
      $timeout( function() { ctx.selectCtrl.activate(); });
    }

    init();
  });
