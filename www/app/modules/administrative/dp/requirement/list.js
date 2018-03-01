angular.module('os.administrative.dp.requirement.list', ['os.administrative.models'])
  .controller('DprListCtrl', function($scope, requirements, PluginReg, DeleteUtil) {
  
    function init() {
      $scope.ctx = {
        dprs: requirements,
        hdrTmpls: PluginReg.getTmpls('dp-req-detail', 'fields-header', ''),
        cellTmpls: PluginReg.getTmpls('dp-req-detail', 'fields-overview', '')
      }
    }
  
    $scope.deleteDpr = function(dpr) {
      DeleteUtil.confirmDelete({
        entity: dpr,
        templateUrl: 'modules/administrative/dp/requirement/delete.html',
        delete : function() {
          dpr.$remove().then(
            function() {
              var index = $scope.ctx.dprs.indexOf(dpr);
              $scope.ctx.dprs.splice(index, 1);
              $scope.input.targets.splice(index, 1);
            }
          );
        }
      });
      
    }
    
    init();
  });

