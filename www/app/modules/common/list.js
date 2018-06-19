angular.module('openspecimen')
  .directive('osList', function($http, $timeout, osRightDrawerSvc, ApiUrls, CheckList, ListPagerOpts, Util) {

    function getUrl() {
      return ApiUrls.getBaseUrl() + 'lists/';
    }

    return {
      restrict: 'E',

      templateUrl: 'modules/common/list.html',

      scope: {
        params: '=',

        itemSref: '@',

        enableSelection: '@',

        showItem: '&',
 
        initCtrl: '&'
      },

      controller: function($scope) {
        var ctrl = this;

        var pagerOpts, listParams, ctx;

        function init() {
          pagerOpts = ctrl.pagerOpts = new ListPagerOpts({listSizeGetter: getListSize});
          listParams = ctrl.listParams = {maxResults: pagerOpts.recordsPerPage + 1};

          $http.get(getUrl() + 'config', {params: $scope.params}).then(
            function(resp) {
              if (!resp.data) {
                alert("The list is not configured");
                return;
              }

              ctx = $scope.ctx = {
                filtersCfg: resp.data.filters,
                filters: Util.filterOpts({}),
                data: {},
                listSize: -1,
                pagerOpts: pagerOpts
              };

              ctrl.haveFilters = ctx.filtersCfg && ctx.filtersCfg.length > 0;
              Util.filter($scope, 'ctx.filters', loadList);

              if ($scope.initCtrl) {
                $scope.initCtrl({$list: ctrl});
              }

              $timeout(loadList);
            }
          );
        }

        function loadList() {
          var params = angular.extend({}, $scope.params);
          angular.extend(params, listParams);
          if (pagerOpts.$$pageSizeChanged > 0) {
            params.includeCount = false;
          }

          $http.post(getUrl() + 'data', getFilters(), {params: params}).then(
            function(resp) {
              ctx.data = resp.data;
              if (params.includeCount) {
                ctx.listSize = resp.data.size;
              }

              pagerOpts.refreshOpts(resp.data.rows);
              if (ctx.data.rows.length > 12 && ctrl.haveFilters) {
                osRightDrawerSvc.open();
              }

              if (ctrl.enableSelection) {
                ctrl.checkList = $scope.checkList = new CheckList(ctx.data.rows);
              }
            }
          );
        }

        function getFilters() {
          var filters = [];
          if (ctrl.filtersCtrl) {
            filters = ctrl.filtersCtrl.getFilters();
          }

          return filters;
        }

        function getListSize() {
          if (!listParams.includeCount) {
            listParams.includeCount = true;

            var params = angular.extend({}, $scope.params);
            angular.extend(params, listParams);

            return $http.post(getUrl() + 'size', getFilters(), {params: params}).then(
              function(resp) {
                ctx.listSize = +resp.data.size;
                return {count: ctx.listSize};
              }
            );
          } else {
            return {count: ctx.listSize};
          }
        }

        function getExpressionValues(expr, searchTerm) {
          var params = angular.extend({expr: expr, searchTerm: searchTerm}, $scope.params);
          return $http.get(getUrl() + 'expression-values', {params: params}).then(
            function(resp) {
              return resp.data;
            }
          );
        }


        this.getSelectedItems = function() {
          if (!$scope.checkList) {
            return[];
          }

          return $scope.checkList.getSelectedItems()
        }

        this.loadList = loadList;

        this.getExpressionValues = getExpressionValues;

        init();
      },

      controllerAs: '$list',

      link: function(scope, element, attrs, ctrl) {
        ctrl.enableSelection = (scope.enableSelection == 'true' || scope.enableSelection == true);
        if (ctrl.enableSelection) {
          ctrl.checkList = scope.checkList = new CheckList([]);
        }

        scope.setFiltersCtrl = function(filtersCtrl) {
          ctrl.filtersCtrl = filtersCtrl;
        }

        scope.pageSizeChanged = function(newPageSize) {
          ctrl.listParams.maxResults = ctrl.pagerOpts.recordsPerPage + 1;
          ctrl.loadList();
        }

        scope.loadFilterValues = function(expr) {
          return ctrl.getExpressionValues(expr);
        }
      }
    }
  });
