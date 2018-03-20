
angular.module('os.biospecimen.visit.detail', ['os.biospecimen.models'])
  .controller('VisitDetailCtrl', function(
    $scope, $state, cp, cpr, visit, specimens, storeSpr,
    CpConfigSvc, Specimen, VisitNamePrinter, DeleteUtil, ExtensionsUtil) {

    function init() {
      $scope.cpr = cpr;
      $scope.visit = visit;
      $scope.specimens = specimens;
      $scope.allowPrintVisit = false;

      $scope.sprUploadAllowed = storeSpr && visit.status == 'Complete';
      ExtensionsUtil.createExtensionFieldMap(visit);

      CpConfigSvc.getCommonCfg(cp.id, 'printVisitsEnabled').then(
        function(value) {
          $scope.allowPrintVisit = (value == 'true' || value == true);
        }
      );
    }
          
    function onVisitDeletion() {
      $state.go('participant-detail.overview', {cprId: cpr.id, cpId: cpr.cpId});
    }

    $scope.deleteVisit = function() {
      DeleteUtil.delete($scope.visit, {onDeletion: onVisitDeletion, forceDelete: true});
    }

    $scope.reload = function() {
      var visitDetail = {
        visitId: visit.id,
        eventId: visit.eventId
      };

      return Specimen.listFor(cpr.id, visitDetail).then(
        function(specimens) {
          $scope.specimens = specimens;
        }
      );
    }

    $scope.printVisitName = function() {
      VisitNamePrinter.printNames({visitIds: [$scope.visit.id]});
    }

    init();
  });
