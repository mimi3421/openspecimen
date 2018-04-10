
angular.module('os.biospecimen.cp.dp', [])
  .controller('CpDpSettingsCtrl', function($scope, cp, DistributionProtocol, Alerts, AuthorizationService) {
    var defDps = undefined;
    function init() {
      $scope.dpCtx = {
        cp: angular.copy(cp),
        savedDps: [],
        editAllowed: AuthorizationService.isAllowed($scope.cpResource.updateOpts)
      };

      initCpDps();
      loadDps();
    }

    function initCpDps() {
      var savedDps = angular.copy(cp.distributionProtocols);
      angular.forEach(savedDps, addDisplayValue);
      $scope.dpCtx.savedDps = savedDps;
      return savedDps;
    }

    function loadDps(searchString) {
      if (defDps && (!searchString || defDps.length < 100)) {
        $scope.dpCtx.dps = defDps;
        return;
      }

      var filterOpts = {activityStatus: 'Active', query: searchString, excludeExpiredDps: true, cp: cp.shortTitle};
      DistributionProtocol.query(filterOpts).then(
        function(dps) {
          angular.forEach(dps, addDisplayValue);
          $scope.dpCtx.dps = dps;

          if (!searchString) {
            defDps = dps;

            if (dps.length == 0) {
              Alerts.error('cp.dp.no_dp');
            }
          }
        }
      );
    }

    function addDisplayValue(obj) {
      return angular.extend(obj, {itemKey: obj.shortTitle, displayValue: obj.shortTitle});
    }

    function addDp(dp) {
      var dupDp = $scope.dpCtx.cp.distributionProtocols.some(
        function(savedDp) {
          return savedDp.shortTitle == dp.itemKey;
        }
      );

      if (dupDp) {
        Alerts.error('cp.dp.dup_dp');
        return false;
      }

      $scope.dpCtx.cp.distributionProtocols.push({shortTitle: dp.itemKey});
      return true;
    }

    function removeDp(dp) {
      var retainedDps = $scope.dpCtx.cp.distributionProtocols.filter(
        function(savedDp) {
          return savedDp.shortTitle != dp.itemKey;
        }
      );

      $scope.dpCtx.cp.distributionProtocols = retainedDps;
    }


    $scope.loadDps = loadDps;

    $scope.listChanged = function(action, dp) {
      switch(action) {
        case 'add':
          if (!addDp(dp)) {
            return;
          }

          break;

        case 'remove':
          removeDp(dp);
          break;

        case 'update':
          removeDp({itemKey: dp.shortTitle});
          if (!addDp({itemKey: dp.displayValue})) {
            return;
          }
      }

      delete $scope.dpCtx.cp.repositoryNames;
      delete $scope.dpCtx.cp.extensionDetail;
      delete $scope.dpCtx.cp.catalogSetting;
      return $scope.dpCtx.cp.$saveOrUpdate().then(
        function(savedCp) {
          angular.extend(cp, savedCp);
          return initCpDps();
        }
      );
    }

    init();
  });
