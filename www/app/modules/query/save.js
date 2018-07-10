
angular.module('os.query.save', ['os.query.models'])
  .controller('QuerySaveCtrl', function($scope, $modalInstance, queryToSave, dependentQueries) {
    $scope.lctx = {
      queryToSave: queryToSave,
      dependentQueries: dependentQueries
    };

    $scope.save = function(copy) {
      if (copy) {
        queryToSave.id = undefined;
      }

      queryToSave.$saveOrUpdate().then(
        function(savedQuery) {
          $modalInstance.close(savedQuery);
        }
      );
    }

    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    }
  });
