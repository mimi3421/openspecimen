
angular.module('os.biospecimen.specimen.addaliquots', [])
  .controller('AddAliquotsCtrl', function(
    $scope, $rootScope, $state, $stateParams, specimen, cpr,
    visit, extensionCtxt, hasDict, onValueChangeCb,
    CollectSpecimensSvc, SpecimenUtil, ExtensionsUtil, Alerts) {

    function init() {
      $scope.parentSpecimen = specimen;
      $scope.cpr = cpr;
      $scope.visit = visit;
      $scope.aliquotSpec = {
        cpId: visit.cpId,
        lineage: 'Aliquot',
        specimenClass: specimen.specimenClass,
        type: specimen.type,
        createdOn : Date.now(),
        freezeThawCycles: specimen.freezeThawCycles + 1,
        incrParentFreezeThaw: 1,
        labelFmt: cpr.aliquotLabelFmt
      };

      //
      // On successful collection of aliquots, direct user to specimen detail view
      // TODO: where to go should be state input param
      //
      if ($rootScope.stateChangeInfo.fromState.url.indexOf("collect-specimens") == 1) {
        var params = {specimenId:  $scope.parentSpecimen.id, srId:  $scope.parentSpecimen.reqId};
        $state.go('specimen-detail.overview', params);
      }

      var exObjs = [
        'specimen.label', 'specimen.barcode', 'specimen.lineage',
        'specimen.parentLabel', 'specimen.initialQty',
        'specimen.availableQty', 'specimen.storageLocation',
        'specimen.events', 'specimen.collectionEvent', 'specimen.receivedEvent'
      ];

      if (hasDict) {
        $scope.spmnCtx = {
          aobj: {cpr: cpr, visit: visit, specimen: $scope.aliquotSpec},
          ainObjs: ['specimen'], aexObjs: exObjs,
          aopts: {onValueChange: onValueChangeCb}
        }
      } else {
        $scope.aextnOpts = ExtensionsUtil.getExtnOpts($scope.aliquotSpec, extensionCtxt);
      }

      $scope.adeFormCtrl = {};
    }

    function getState() {
      return {state: $state.current, params: $stateParams};
    }

    $scope.toggleIncrParentFreezeThaw = function() {
      if ($scope.aliquotSpec.incrParentFreezeThaw) {
        if ($scope.parentSpecimen.freezeThawCycles == $scope.aliquotSpec.freezeThawCycles) {
          $scope.aliquotSpec.freezeThawCycles = $scope.parentSpecimen.freezeThawCycles + 1;
        }
      } else {
        if (($scope.parentSpecimen.freezeThawCycles + 1) == $scope.aliquotSpec.freezeThawCycles) {
          $scope.aliquotSpec.freezeThawCycles = $scope.parentSpecimen.freezeThawCycles;
        }
      }
    }

    $scope.onChange = function(fieldName) {
      var ctx = $scope.spmnCtx;
      if (ctx.aopts.$$sdeFormFields) {
        ctx.aopts.$$sdeFormFields.valueChanged(undefined, ctx.aobj, 'specimen.' + fieldName, undefined);
      }
    }

    $scope.collectAliquots = function() {
      $scope.deFormCtrl = $scope.adeFormCtrl;
      var specimens = SpecimenUtil.collectAliquots($scope);
      if (specimens) {
        var opts = {ignoreQtyWarning: true, showCollVisitDetails: false};
        CollectSpecimensSvc.collect(getState(), $scope.visit, specimens, opts);
      }
    }

    init();
  });
