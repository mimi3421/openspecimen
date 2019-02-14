angular.module('os.biospecimen.specimen')
  .controller('BulkCreateDerivativesCtrl', function(
    $scope, $injector, $translate, parentSpmns, cp, cpr,
    cpDict, derivedFields, spmnHeaders, incrFreezeThawCycles,
    Specimen, Alerts, Util, SpecimenUtil) {

    var ctx;

    function init() {
      var createdOn = new Date().getTime();
      var freezeThawIncrStep = incrFreezeThawCycles ? 1 : 0;

      var derivedSpmns = parentSpmns.map(
        function(ps) {
          return new Specimen({
            lineage: 'Derived',
            cpId: ps.cpId,
            ppid: ps.ppid,
            parentId: ps.id,
            parentLabel: ps.label,
            parentType: ps.type,
            parentCreatedOn: ps.createdOn,
            createdOn: createdOn,
            status: 'Collected',
            freezeThawCycles: ps.freezeThawCycles + freezeThawIncrStep,
            incrParentFreezeThaw: freezeThawIncrStep,
            parent: new Specimen(ps)
          });
        }
      );

      $scope.cp = cp;
      $scope.cpr = cpr;
      var inputLabels = $scope.inputLabels = (!!cp.id && (!cp.derivativeLabelFmt || cp.manualSpecLabelEnabled));
      ctx = $scope.ctx = {
        showCustomFields: true,
        derivedSpmns: derivedSpmns,
        inputLabels: inputLabels,
        spmnHeaders: spmnHeaders
      };

      var opts = $scope.opts = {
        viewCtx: $scope, static: false,
        showRowCopy: true, hideFooterActions: true, allowBulkUpload: false
      };
      var groups = ctx.customFieldGroups = SpecimenUtil.sdeGroupSpecimens(
        cpDict, derivedFields || [], derivedSpmns, {}, opts);
      ctx.warnNoMatch = groups.length > 1 && groups[groups.length - 1].noMatch;
      ctx.showCustomFields = (groups.length > 1) || (groups.length == 1 && !groups[0].noMatch);
      if (ctx.showCustomFields) {
        $scope.$watch(
          function() {
            return groups.map(
              function(group) {
                var types = [];
                angular.forEach(group.input, function(input) { types.push(input.specimen.type); });
                return types;
              }
            );
          },
          function() {
            countRows(groups);
          },
          true
        );
      }
    }

    function countRows(groups) {
      angular.forEach(groups,
        function(group) {
          var typeCounts = {};
          angular.forEach(group.input,
            function(input) {
              var count = typeCounts[input.specimen.type || 'Not Specified'] || 0;
              typeCounts[input.specimen.type || 'Not Specified'] = count + 1;
            }
          );

          var totalRows = 0;
          var result = '';
          angular.forEach(Object.keys(typeCounts).sort(),
            function(t) {
              if (result) {
                result += ' | ';
              }

              result += t + ': ' + typeCounts[t];
              totalRows += typeCounts[t];
            }
          );

          result = $translate.instant('common.total_rows') + ': ' + totalRows + ' | ' + result;
          group.$$counts = result;
        }
      );
    }

    function initCloseParent(parentSamples) {
      angular.forEach(parentSamples,
        function(samples) {
          var closeParent = false;
          angular.forEach(samples,
            function(sample) {
              closeParent = closeParent || sample.specimen.closeParent;
              sample.specimen.closeParent = false;
            }
          );

          samples[samples.length - 1].specimen.closeParent = closeParent;
        }
      );
    }

    function submitSamples() {
      var parentSamples = {};

      var samples = [];
      for (var i = 0; i < ctx.customFieldGroups.length; ++i) {
        var group = ctx.customFieldGroups[i];
        for (var j = 0; j < group.input.length; ++j) {
          var spec = group.input[j];
          var spmn = spec.specimen;
          if (!isValidCreatedOn(spmn)) {
            return;
          }

          var sample = {specimen: spmn, events: spec.events};
          samples.push(sample);

          parentSamples[spmn.parent.id] = parentSamples[spmn.parent.id] || [];
          parentSamples[spmn.parent.id].push(sample);
        }
      }

      initCloseParent(parentSamples);
      $injector.get('sdeSample').saveSamples(samples).then(
        function(resp) {
          Alerts.success('specimens.derivatives_created');
          $scope.back();
        }
      );
    }

    function isValidCreatedOn(spmn) {
      if (spmn.createdOn < spmn.parentCreatedOn) {
        Alerts.error("specimens.errors.children_created_on_lt_parent", {parentLabel: spmn.parentLabel});
        return false;
      } else if (spmn.createdOn > new Date().getTime()) {
        Alerts.error("specimens.errors.children_created_on_gt_curr_time", {parentLabel: spmn.parentLabel});
        return false;
      } else {
        return true;
      }
    }

    $scope.copyFirstToAll = function() {
      var spmnToCopy = $scope.ctx.derivedSpmns[0];
      var attrsToCopy = ['specimenClass', 'type', 'initialQty', 'createdOn', 'printLabel', 'closeParent'];
      Util.copyAttrs(spmnToCopy, attrsToCopy, $scope.ctx.derivedSpmns);
      SpecimenUtil.copyContainerName(spmnToCopy, $scope.ctx.derivedSpmns);
    }

    $scope.removeSpecimen = function(index) {
      $scope.ctx.derivedSpmns.splice(index, 1);
      if ($scope.ctx.derivedSpmns.length == 0) {
        $scope.back();
      }
    }

    $scope.addAnother = function(group) {
      group.input.push(angular.copy(group.lastRow));
    }

    $scope.createDerivatives = function() {
      if (ctx.showCustomFields) {
        submitSamples();
        return;
      }

      var result = [];
      for (var i = 0; i < ctx.derivedSpmns.length; ++i) {
        var spmn = angular.copy(ctx.derivedSpmns[i]);
        if (!isValidCreatedOn(spmn)) {
          return;
        }

        delete spmn.ppid;
        delete spmn.parentCreatedOn;
        delete spmn.parentType;

        if (spmn.closeParent) {
          result.push(new Specimen({
            id: spmn.parentId,
            status: 'Collected',
            children: [spmn],
            closeAfterChildrenCreation: true
          }));
        } else {
          result.push(spmn);
        }
      }

      Specimen.save(result).then(
        function() {
          Alerts.success('specimens.derivatives_created');
          $scope.back();
        }
      );
    }

    init();
  });
