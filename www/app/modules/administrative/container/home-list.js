angular.module('os.administrative.container')
  .controller('HomeContainersListCtrl', function($scope, Container) {
    function init() {
      $scope.containers = [];
      Container.query({topLevelContainers: true}).then(
        function(containers) {
          $scope.containers = containers;
        }
      );
    }

    init();
  });
