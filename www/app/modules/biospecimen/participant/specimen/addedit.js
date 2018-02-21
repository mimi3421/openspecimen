
angular.module('os.biospecimen.specimen.addedit', [])
  .controller('AddEditSpecimenCtrl', function(
    $scope, $state, cp, cpr, visit, specimen, extensionCtxt, hasDict,
    Specimen, Container, CollectSpecimensSvc, PvManager, SpecimenUtil, Util, ExtensionsUtil) {

    function init() {
      var currSpecimen = $scope.currSpecimen = angular.copy(specimen);
      delete currSpecimen.children;

      currSpecimen.cpId = currSpecimen.cpId || cp.id;
      currSpecimen.visitId = visit && visit.id;
      currSpecimen.createdOn = currSpecimen.createdOn || new Date();

      if (currSpecimen.lineage == 'Aliquot') {
        currSpecimen.anatomicSite = currSpecimen.laterality = undefined;
      }

      if (currSpecimen.status != 'Collected') {
        if (!currSpecimen.id) {
          currSpecimen.status = 'Collected';
        }

        currSpecimen.availableQty = currSpecimen.initialQty;
      }

      if (!currSpecimen.labelFmt) {
        if (specimen.lineage == 'New') {
          currSpecimen.labelFmt = cpr.specimenLabelFmt;
        } else if (specimen.lineage == 'Aliquot') {
          currSpecimen.labelFmt = cpr.aliquotLabelFmt;
        } else if (specimen.lineage == 'Derived') {
          currSpecimen.labelFmt = cpr.derivativeLabelFmt;
        }
      }

      var exObjs = ['specimen.lineage', 'specimen.parentLabel', 'specimen.events'];
      if (!$scope.currSpecimen.id && !$scope.currSpecimen.reqId) {
        var currentDate = new Date();
        $scope.currSpecimen.collectionEvent = {
          user: $scope.currentUser,
          time: currentDate
        };

        $scope.currSpecimen.receivedEvent = {
          user: $scope.currentUser,
          time: currentDate
        };

        $scope.currSpecimen.collectionEvent.container = "Not Specified";
        $scope.currSpecimen.collectionEvent.procedure = "Not Specified";
        $scope.currSpecimen.receivedEvent.receivedQuality = "Acceptable";
      }

      if (currSpecimen.lineage != 'New') {
        exObjs.push('specimen.collectionEvent', 'specimen.receivedEvent');
      }

      $scope.currSpecimen.initialQty = Util.getNumberInScientificNotation($scope.currSpecimen.initialQty);
      $scope.currSpecimen.availableQty = Util.getNumberInScientificNotation($scope.currSpecimen.availableQty);
      $scope.currSpecimen.concentration = Util.getNumberInScientificNotation($scope.currSpecimen.concentration);

      $scope.aliquotSpec = {createdOn : Date.now()};
      var aexObjs = [
        'specimen.label', 'specimen.barcode', 'specimen.lineage', 'specimen.type',
        'specimen.parentLabel', 'specimen.initialQty', 'specimen.availableQty',
        'specimen.storageLocation', 'specimen.events', 'specimen.collectionEvent',
        'specimen.receivedEvent'
      ];

      var viewRule = {
        op: 'AND',
        rules: [{field: 'viewCtx.mode', op: '==', value: '\'single\''}]
      };
      var spmnCtx = $scope.spmnCtx = {
        obj: {specimen: $scope.currSpecimen, cp: cp}, inObjs: ['specimen'], exObjs: exObjs,
        opts: {viewShowIf: {'specimen.label': viewRule, 'specimen.barcode': viewRule, 'specimen.storageLocation': viewRule}},
        isVirtual: specimen.showVirtual(),
        manualSpecLabelReq: !!currSpecimen.label || !currSpecimen.labelFmt || cp.manualSpecLabelEnabled,
        mode: 'single',
        aobj: {specimen: $scope.aliquotSpec}, ainObjs: ['specimen'], aexObjs: aexObjs
      };
      spmnCtx.obj.viewCtx = spmnCtx;

      $scope.deFormCtrl = {};
      $scope.extnOpts = ExtensionsUtil.getExtnOpts(currSpecimen, extensionCtxt);

      $scope.adeFormCtrl = {};
      $scope.aextnOpts = ExtensionsUtil.getExtnOpts($scope.aliquotSpec, extensionCtxt);

      if (!hasDict) {
        loadPvs();
      }
    }

    function loadPvs() {
      $scope.biohazards = PvManager.getPvs('specimen-biohazard');
      $scope.specimenStatuses = PvManager.getPvs('specimen-status');
    };

    function saveSpecimen() {
      $scope.currSpecimen.$saveOrUpdate().then(
        function(result) {
          angular.extend($scope.specimen, result);
          var params = {specimenId: result.id, cprId: result.cprId, visitId: result.visitId, srId: result.reqId};
          $state.go('specimen-detail.overview', params);
        }
      );
    }

    function getSpecimensToSave(specimen) {
      var labels = [], numOfSpecimens = specimen.numOfSpecimens;
      if (!numOfSpecimens) {
        labels = Util.splitStr(specimen.labels, /,|\t|\n/);
        numOfSpecimens = labels.length;
      }

      // Create multiple specimens
      var specimensToSave = [];
      for (var i = 0; i < numOfSpecimens; ++i) {
        var toSave = angular.copy(specimen);
        if (labels.length > 0) {
          toSave.label = labels[i];
        }

        toSave.status = 'Pending';
        delete toSave.numOfSpecimens;
        delete toSave.labels;
        specimensToSave.push(toSave);
      }

      return specimensToSave;
    }

    function getState() {
      if (cp.specimenCentric) {
        return {state: $state.get('cp-specimens'), params: {cpId: cp.id}};
      } else {
        return {state: $state.get('visit-detail.overview'), params: {visitId: visit.id}};
      }
    };

    $scope.save = function() {
      var formCtrl = $scope.deFormCtrl.ctrl;
      if (formCtrl && !formCtrl.validate()) {
        return;
      }

      if (formCtrl) {
        $scope.currSpecimen.extensionDetail = formCtrl.getFormData();
      }

      var aliquotSpec = $scope.aliquotSpec;

      var specimensToCollect = [];
      var aliquotDetail = {
        aliquotSpec : aliquotSpec,
        cpr: cpr
      };

      if ($scope.spmnCtx.mode == 'single') {
        if (!aliquotSpec.createAliquots) {
          saveSpecimen();
          return;
        }

        aliquotDetail.parentSpecimen =  $scope.currSpecimen;
        aliquotDetail.deFormCtrl = $scope.adeFormCtrl;

        var tree = SpecimenUtil.collectAliquots(aliquotDetail);
        tree[0].status = "Pending";
        tree[0].selected = true;
        specimensToCollect = tree;
      } else if ($scope.spmnCtx.mode == 'multiple') {
        var primarySpmns = getSpecimensToSave($scope.currSpecimen);
        angular.forEach(primarySpmns,
          function(primarySpmn) {
            if (!aliquotSpec.createAliquots) {
              primarySpmn.selected = true;
              specimensToCollect.push(primarySpmn);
            } else {
              var detail = angular.copy(aliquotDetail);
              detail.parentSpecimen = primarySpmn;
              detail.deFormCtrl = $scope.adeFormCtrl;

              var tree = SpecimenUtil.collectAliquots(detail);
              tree[0].selected = true;
              Array.prototype.push.apply(specimensToCollect, tree);
            }
          }
        );
      }

      var opts = {showCollVisitDetails: false};
      CollectSpecimensSvc.collect(getState(), visit, specimensToCollect, opts);
    }

    $scope.toggleIncrParentFreezeThaw = function() {
      if ($scope.aliquotSpec.incrParentFreezeThaw) {
        if ($scope.currSpecimen.freezeThawCycles == $scope.aliquotSpec.freezeThawCycles) {
          $scope.aliquotSpec.freezeThawCycles = parseInt($scope.currSpecimen.freezeThawCycles) + 1;
        }
      } else {
        if ((parseInt($scope.currSpecimen.freezeThawCycles) + 1) == $scope.aliquotSpec.freezeThawCycles) {
          $scope.aliquotSpec.freezeThawCycles = $scope.currSpecimen.freezeThawCycles;
        }
      }
    }

    init();
  });
