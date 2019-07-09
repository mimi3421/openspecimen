
angular.module('os.biospecimen.cpgroups')
  .controller('CpGroupOverviewCtrl', function($scope, $state, group, CollectionProtocolGroup, Util) {

    var octx;
    function init() {
      octx = $scope.octx = {};
      octx.workflowUri = CollectionProtocolGroup.url() + group.id + '/workflows-file';
    }

    $scope.showCpOverview = function(cp) {
      $state.go('cp-detail.overview', {cpId: cp.id});
    }

    init();
  });
