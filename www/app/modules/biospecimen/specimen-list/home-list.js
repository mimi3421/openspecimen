
angular.module('os.biospecimen.specimenlist')
  .controller('HomeSpecimenListCtrl', function($scope, SpecimenList) {
    function init() {
      $scope.lists = [];
      SpecimenList.query({includeStats: false}).then(
        function(lists) {
          $scope.lists = lists;
        }
      );
    }

    init();
  });
