
angular.module('os.query')
  .controller('HomeQueryListCtrl', function($scope, SavedQuery) {
    function init() {
      $scope.queries = [];
      SavedQuery.query().then(
        function(queries) {
          $scope.queries = queries;
        }
      );
    }

    init();
  });
