angular.module('os.administrative.dp.requirement.addedit', ['os.administrative.models'])
  .controller('DprAddEditCtrl', function(
    $scope, $state, distributionProtocol, dpr, extensionCtxt,
    ExtensionsUtil, PvManager) {

    function init() {
      $scope.dpr = dpr;
      $scope.deFormCtrl = {};
      $scope.extnOpts = ExtensionsUtil.getExtnOpts(dpr, extensionCtxt);
      loadAllSpecimenTypes();
    }

    function loadAllSpecimenTypes() {
      $scope.specimenTypes = [];
      
      return PvManager.loadPvsByParent('specimen-class', undefined, true).then(
        function(specimenTypes) {
          angular.forEach(specimenTypes, function(type) {
            if ($scope.specimenTypes.indexOf(type.value) < 0) {
              $scope.specimenTypes.push(type.value);
            }
          });
        }
      );
    }
  
    $scope.cancel = function() {
      $state.go('req-list');
    }
    
    $scope.save = function() {
      var formCtrl = $scope.deFormCtrl.ctrl;
      if (formCtrl && !formCtrl.validate()) {
        return;
      }

      var dpr = angular.extend(angular.copy($scope.dpr), {dp: {id: distributionProtocol.id}});
      if (formCtrl) {
        dpr.extensionDetail = formCtrl.getFormData();
      }

      dpr.$saveOrUpdate().then(
        function(saveReq) {
          $state.go('req-list');
        }
      );
    }
    
    init();
    
  });

