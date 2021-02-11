
angular.module('os.biospecimen.cp')
  .controller('HomeCpListCtrl', function($scope, CollectionProtocol) {
    function init() {
      $scope.cps = [];
      CollectionProtocol.list({detailedList: false, orderByStarred: true}).then(
        function(cps) {
          $scope.cps = cps;
        }
      );
    }

    init();
  });
