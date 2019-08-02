angular.module('os.administrative.container')
  .controller('ContainerEventsCtrl', function($scope, events) {

    function init() {
      $scope.ctx.viewState = 'container-detail.events';
      $scope.lctx = {events: events.sort(function(e1, e2) { return e2.id - e1.id; })};
    }

    init();
  });
