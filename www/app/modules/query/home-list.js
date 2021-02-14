
angular.module('os.query')
  .controller('HomeQueryListCtrl', function($scope, SavedQuery) {
    var ctx;

    function init(opts) {
      ctx = $scope.ctx = {
        defList: undefined,
        queries: []
      };

      $scope.$watch('opts.searchTerm', function(newVal) { loadQueries(newVal); });
    }

    function loadQueries(searchTerm) {
      if (!searchTerm && ctx.defList) {
        ctx.queries = ctx.defList;
        return;
      }

      SavedQuery.query({searchString: searchTerm, orderByStarred: true, max: 25}).then(
        function(queries) {
          ctx.queries = queries;
          if (!searchTerm) {
            ctx.defList = queries;
          }
        }
      );
    }

    $scope.init = init;
  });
