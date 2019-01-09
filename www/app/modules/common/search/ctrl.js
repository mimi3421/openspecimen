
angular.module('os.common.search.ctrl', [])
  .controller('QuickSearchCtrl', function($scope, $state, QuickSearchSvc) {

    var ctx;

    function init() {
      ctx = $scope.ctx = {matches: []};
    }

    $scope.search = function(keyword) {
      if (!keyword) {
        ctx.matches = undefined;
        ctx.selectedMatch = undefined;
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
      var state = QuickSearchSvc.getState(match.entity);
      var stateParams = {stateName: state, objectName: match.entity, key: 'id', value: match.entityId};
      $state.go('object-state-params-resolver', stateParams);

      $scope.ctx.selectedMatch = undefined;
      $scope.ctx.matches = [];
    }

    init();
  });
