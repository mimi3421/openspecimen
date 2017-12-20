
angular.module('os.biospecimen.participant.collect-specimens', 
  [ 
    'os.biospecimen.models'
  ])
  .factory('CollectSpecimensSvc', function($state, $parse, CpConfigSvc, Specimen, Container) {
    var data = {opts: {}};

    function getReservePositionsOp(cpId, cprId, allocRules, specimens) {
      var aliquots = {}, result = [];
      angular.forEach(specimens,
        function(specimen) {
          if (specimen.storageType == 'Virtual' || (!!specimen.status && specimen.status != 'Pending')) {
            return;
          }

          angular.extend(specimen, {cpId: cpId, cprId: cprId});
          var match = specimen.getMatchingRule(allocRules);

          var selectorCrit;
          if (specimen.lineage == 'Aliquot') {
            var key = specimen.parent.id + '-' + specimen.parent.reqId + '-' + match.index;
            selectorCrit = aliquots[key];
            if (!selectorCrit) {
              aliquots[key] = selectorCrit = getSelectorCriteria(match.rule, cpId, cprId, specimen);
              result.push(selectorCrit);
            }
          } else {
            selectorCrit = getSelectorCriteria(match.rule, cpId, cprId, specimen);
            result.push(selectorCrit);
          }

          selectorCrit.minFreePositions++;
          selectorCrit.$$group.push(specimen);
        }
      );

      return {cpId: cpId, criteria: result};
    }

    function getSelectorCriteria(allocRule, cpId, cprId, specimen) {
      var selectorCrit = {
        specimen: angular.extend({}, specimen),
        minFreePositions: 0,
        ruleName:   (allocRule && allocRule.name) || undefined,
        ruleParams: (allocRule && allocRule.params) || undefined,
        '$$group': []
      };

      var attrsToDelete = [
        'hasChildren', 'parent', 'children', 'depth',
        'hasOnlyPendingChildren', 'isOpened', 'selected',
        'aliquotGrp', 'grpLeader', 'pLabel', 'pBarcode',
        'isVirtual', 'existingStatus', 'showInTree',
        'expanded', 'aliquotLabels', 'aliquotBarcodes', 'removed'
      ];
      angular.forEach(attrsToDelete, function(attr) { delete selectorCrit.specimen[attr]; });
      return selectorCrit;
    }

    function assignReservedPositions(resvOp, positions) {
      var idx = 0;
      angular.forEach(resvOp.criteria,
        function(selectorCriteria) {
          angular.forEach(selectorCriteria.$$group,
            function(specimen) {
              specimen.storageLocation = positions[idx++];
            }
          );
        }
      );
    }

    //
    // opts: {ignoreQtyWarning: [true | false], showCollVisitDetails: [true | false]}
    //
    function collect(stateDetail, visit, specimens, opts) {
      data.stateDetail = stateDetail;
      data.visit = visit;
      data.specimens = specimens;
      data.opts = opts || {};

      allocatePositions(visit, specimens).then(
        function() {
          $state.go('participant-detail.collect-specimens', {visitId: visit.id, eventId: visit.eventId});
        }
      );
    }

    function allocatePositions(visit, specimens, reservationToCancel) {
      return CpConfigSvc.getWorkflowData(visit.cpId, 'auto-allocation').then(
        function(data) {
          var resvOp = getReservePositionsOp(visit.cpId, visit.cprId, data.rules || [], specimens);
          resvOp.reservationToCancel = reservationToCancel;
          return Container.getReservedPositions(resvOp).then(
            function(positions) {
              if (positions.length > 0) {
                assignReservedPositions(resvOp, positions);
              }
              return true;
            }
          )
        }
      );
    }

    function isAnyChildOrPoolSpecimenPending(spmn) {
      if (!spmn.status || spmn.status == 'Pending') {
        return true;
      }

      if ((spmn.specimensPool || []).some(isAnyChildOrPoolSpecimenPending)) {
        return true;
      }

      if ((spmn.children || []).some(isAnyChildOrPoolSpecimenPending)) {
        return true;
      }

      return false;
    }

    return {
      collect: collect,

      collectPending: function(returnState, cp, cprId, visit) {
        var visitDetail = {visitId: visit.id, eventId: visit.eventId};
        Specimen.listFor(cprId, visitDetail).then(
          function(specimens) {
            var spmnsToCollect = [];

            angular.forEach(Specimen.flatten(specimens),
              function(spmn) {
                if (isAnyChildOrPoolSpecimenPending(spmn)) {
                  spmn.isOpened = spmn.selected = true;
                  spmnsToCollect.push(spmn);
                }
              }
            );

            collect(returnState, visit, spmnsToCollect);
          }
        );
      },

      collectVisit: function(returnState, cp, cprId, visit) {
        var visitDetail = {visitId: visit.id, eventId: visit.eventId};
        Specimen.listFor(cprId, visitDetail).then(
          function(specimens) {
            var spmnsToCollect = Specimen.flatten(specimens);
            if (cp.visitCollectionMode == 'PRIMARY_SPMNS') {
              spmnsToCollect = spmnsToCollect.filter(function(spmn) { return spmn.lineage == 'New'; });
            }

            angular.forEach(spmnsToCollect,
              function(specimen) {
                specimen.isOpened = specimen.selected = true;
              }
            );

            collect(returnState, visit, spmnsToCollect);
          }
        );
      },

      allocatePositions: allocatePositions,

      clear: function() {
        data.stateDetail = undefined;
        data.visit = undefined;
        data.specimens = [];
      },

      getSpecimens: function() {
        return data.specimens || []; 
      },

      getVisit: function() {
        return data.visit;
      },

      getStateDetail: function() {
        return data.stateDetail;
      },

      ignoreQtyWarning: function() {
        return data.opts.ignoreQtyWarning || false;
      },

      showCollVisitDetails: function() {
        return data.opts.showCollVisitDetails !== false;
      }
    };
  })
  .controller('CollectSpecimensCtrl', 
    function(
      $scope, $translate, $state, $document, $q, $parse, $injector,
      cp, cpr, visit, latestVisit, cpDict, spmnCollFields, mrnAccessRestriction,
      Visit, Specimen, PvManager, CollectSpecimensSvc, Container,
      ExtensionsUtil, Alerts, Util, SpecimenUtil) {

      var ignoreQtyWarning = false;

      function init() {
        ignoreQtyWarning = CollectSpecimensSvc.ignoreQtyWarning();
        $scope.showCollVisitDetails = CollectSpecimensSvc.showCollVisitDetails();
        $scope.customFieldGroups = [];
        $scope.mrnAccessRestriction = mrnAccessRestriction;

        var printSettings = {};
        angular.forEach(cp.spmnLabelPrintSettings,
          function(setting) {
            printSettings[setting.lineage] = setting.printMode;
          }
        );

        $scope.specimens = CollectSpecimensSvc.getSpecimens().map(
          function(specimen) {
            specimen.existingStatus = specimen.status;
            specimen.isVirtual = specimen.showVirtual();
            specimen.initialQty = Util.getNumberInScientificNotation(specimen.initialQty);
            if (specimen.status != 'Collected') {
              specimen.status = 'Collected';
              specimen.printLabel = printLabel(printSettings, specimen);
            }

            if (specimen.closeAfterChildrenCreation) {
              specimen.selected = true;
            }

            specimen.pLabel = !!specimen.label;
            specimen.pBarcode = !!specimen.barcode;
            return specimen;
          }
        );

        if ($scope.showCollVisitDetails) {
          $scope.showCollVisitDetails = ($scope.specimens || []).some(
            function(specimen) {
              return specimen.lineage == 'New'  && specimen.existingStatus != 'Collected';
            }
          );
        }

        visit.visitDate = visit.visitDate || visit.anticipatedVisitDate || new Date();
        visit.cprId = cpr.id;
        delete visit.anticipatedVisitDate;
        if (!!latestVisit) {
          visit.clinicalDiagnoses = latestVisit.clinicalDiagnoses;
        }

        if (!!visit.site) {
          // visit.site = visit.site;
        } else if (latestVisit) {
          visit.site = latestVisit.site;
        } else if (cpr.participant.pmis && cpr.participant.pmis.length > 0) {
          visit.site = cpr.participant.pmis[0].siteName;
        }

        $scope.visit = visit;
        $scope.autoPosAllocate = !!$scope.cp.containerSelectionStrategy;
        
        $scope.collDetail = {
          collector: undefined,
          collectionDate: new Date(),
          receiver: undefined,
          receiveDate: new Date()
        };

        loadPvs();
        initAliquotGrps($scope.specimens);
        $scope.$on('$destroy', vacateReservedPositions);
      };

      function printLabel(cpPrintSettings, specimen) {
        return (specimen.labelAutoPrintMode == 'ON_COLLECTION') ||
          (!specimen.reqId && cpPrintSettings[specimen.lineage] == 'ON_COLLECTION');
      }

      function initAliquotGrps(specimens) {
        angular.forEach(specimens, function(specimen, $index) {
          if (specimen.parent == undefined || $index == 0) {
            //
            // Either primary specimen or parent of ad-hoc aliquots
            //
            specimen.showInTree = true;
            createAliquotGrp(specimen);
          }
        });

        // Logic of show/hide of aliquots and in the tree
        angular.forEach(specimens, function(specimen) {
          if (!!specimen.grpLeader) { 
            // A aliquot specimen's show/hide is determined by group leader
            return;
          }

          if (specimen.aliquotGrp || specimen.lineage != 'Aliquot') {
            specimen.showInTree = true;
          }

          if (!specimen.aliquotGrp) {
            return;
          }

          var expandGrp = specimen.aliquotGrp.some(
            function(sibling) {
              return sibling.children.length > 0 || specimen.initialQty != sibling.initialQty;
            }
          );

          if (expandGrp) {
            expandOrCollapseAliquotsGrp(specimen, expandGrp);
          }

          initAliquotGrpPrintLabel(specimen);
          specimen.aliquotLabels = getAliquotGrpInputs(specimen, 'label');
          specimen.aliquotBarcodes = getAliquotGrpInputs(specimen, 'barcode');
        });
      }

      function createAliquotGrp(specimen) {
        if (!specimen.children || specimen.children.length == 0) {
          return;
        }

        var aliquotGrp = [];
        var grpLeader = undefined;
        angular.forEach(specimen.children, function(child) {
          if (child.lineage == 'Aliquot' && child.selected && child.existingStatus != 'Collected') {
            aliquotGrp.push(child);

            if (!grpLeader) {
              grpLeader = child;
            } else {
              child.grpLeader = grpLeader;
            }
          }

          createAliquotGrp(child);
        });

        if (grpLeader) {
          grpLeader.aliquotGrp = aliquotGrp;
          listenContainerChanges(grpLeader);
        }
      }

      function expandOrCollapseAliquotsGrp(aliquot, expandOrCollapse) {
        if (!aliquot.aliquotGrp) {
          return;
        }

        setShowInTree(aliquot, expandOrCollapse)
        aliquot.expanded = expandOrCollapse;
        if (!aliquot.expanded) {
          aliquot.aliquotLabels = getAliquotGrpInputs(aliquot, 'label');
          aliquot.aliquotBarcodes = getAliquotGrpInputs(aliquot, 'barcode');
        }
      }

      function initAliquotGrpPrintLabel(aliquot) {
        if (aliquot.expanded) {
          return;
        }

        var printLabel = aliquot.aliquotGrp.some(
          function(sibling) {
            return sibling.printLabel;
          }
        );

        if (!printLabel) {
          return;
        }

        aliquot.printLabel = printLabel;
        setAliquotGrpPrintLabel(aliquot);
      }

      function setAliquotGrpPrintLabel(aliquot) {
        if (aliquot.expanded || !aliquot.aliquotGrp) {
          return;
        }

        angular.forEach(aliquot.aliquotGrp, function(sibling) {
          sibling.printLabel = aliquot.printLabel;
        });
      }

      function getAliquotGrpInputs(specimen, prop) {
        return specimen.aliquotGrp.filter(
          function(s) {
            return !!s[prop];
          }
        ).map(
          function(s) {
            return s[prop];
          }
        ).join(",");
      }

      function vacateReservedPositions() {
        var reservationId = getReservationToCancel();
        if (!reservationId) {
          return null;
        }

        return Container.cancelReservation(reservationId);
      }

      function setShowInTree(aliquot, showInTree) {
        angular.forEach(aliquot.aliquotGrp, function(specimen) {
          if (specimen == aliquot) {
            return;
          }

          if (showInTree) {
            specimen.showInTree = true;
            showSpecimenInTree(specimen);
          } else {
            hideSpecimenInTree(specimen);
          }
        });
      }

      function showSpecimenInTree(specimen) {
        if (specimen.grpLeader && (!specimen.children || specimen.children.length == 0)) {
          return;
        }

        specimen.showInTree = true;
        angular.forEach(specimen.children, function(child) {
          showSpecimenInTree(child);
        });
      }

      function hideSpecimenInTree(specimen) {
        specimen.showInTree = false;
        if (specimen.children.length > 0) {
          angular.forEach(specimen.children, function(child) {
            hideSpecimenInTree(child);
          });
        }
      }

      function addAliquotsToGrp(grpLeader, newSpmnsCnt) {
        var lastSpmn = grpLeader.aliquotGrp[grpLeader.aliquotGrp.length - 1];

        var newSpmns = [];
        var pos = $scope.specimens.indexOf(lastSpmn);
        for (var i = 0; i < newSpmnsCnt; ++i) {
          var newSpmn = shallowCopy(lastSpmn);
          grpLeader.aliquotGrp.push(newSpmn);
          grpLeader.parent.children.push(newSpmn);
          $scope.specimens.splice(pos + i + 1, 0, newSpmn);
        }
      }

      function shallowCopy(spmn) {
        var copy = new Specimen(spmn);
        copy.children = [];

        if (spmn.storageLocation) {
          copy.storageLocation = {name: spmn.storageLocation.name, mode: spmn.storageLocation.mode};
        } else {
          copy.storageLocation = {};
        }

        return copy;
      }

      function removeAliquotsFromGrp(grpLeader, count) {
        var grp = grpLeader.aliquotGrp;
        for (var i = grp.length - 1; i >= 0 && count >= 1; --i, --count) {
          $scope.remove(grp[i]);
        }
      }

      function listenContainerChanges(specimen) {
        $scope.$watch(
          function() {
            return specimen.storageLocation;
          },
          function(newVal, oldVal) {
            if (newVal == oldVal) {
              return;
            }

            if (specimen.expanded) {
              return;
            }

            angular.forEach(specimen.aliquotGrp, function(aliquot, $index) {
              if ($index == 0) {
                return;
              }

              aliquot.storageLocation = {
                name: specimen.storageLocation.name,
                mode: specimen.storageLocation.mode
              };
            });
          }
        );
      }

      function loadPvs() {
        $scope.notSpecified = $translate.instant('pvs.not_specified');
        $scope.specimenStatuses = PvManager.getPvs('specimen-status');
      };

      function flatten(specimens, result) {
        angular.forEach(specimens,
          function(specimen) {
            result.push(specimen);
            flatten(specimen.specimensPool, result);
            delete specimen.specimensPool;

            flatten(specimen.children, result);
            delete specimen.children
          }
        );

        return result;
      }

      function collected(reqIds, specimens) {
        return (specimens || []).filter(
          function(specimen) {
            return !specimen.reqId || reqIds.indexOf(specimen.reqId) != -1;
          }
        );
      }

      function getSpmnReqIds(spmnsToSave) {
        var result = [];
        angular.forEach(spmnsToSave,
          function(spmn) {
            if (spmn.reqId) {
              result.push(spmn.reqId);
            }

            if (spmn.specimensPool) {
              result = result.concat(getSpmnReqIds(spmn.specimensPool));
            }

            if (spmn.children) {
              result = result.concat(getSpmnReqIds(spmn.children));
            }
          }
        );

        return result;
      }

      function displayCustomFieldGroups(spmnReqIds, specimens, navigateTo) {
        var groups = $scope.customFieldGroups = SpecimenUtil.sdeGroupSpecimens(
          cpDict, spmnCollFields.fieldGroups || [], collected(spmnReqIds, flatten(specimens, [])));

        var visitFieldsGrp = getVisitFieldsGroup(spmnCollFields);
        if (visitFieldsGrp) {
          groups.unshift(visitFieldsGrp);
          ExtensionsUtil.createExtensionFieldMap(visit)
        }

        if (groups.length == 0 || (groups.length == 1 && groups[0].noMatch)) {
          navigateTo();
          return;
        }

        $scope.updateSpecimens = function() {
          updateSpecimens(navigateTo);
        }
      }

      function getVisitFieldsGroup(spmnCollFields) {
        if (!spmnCollFields.visitFields) {
          return undefined;
        }

        return {
          visitFields: true,
          multiple: true,
          fields: {groups: [spmnCollFields.visitFields], table: []},
          baseFields: cpDict,
          input: [{visit: visit}],
          opts: {static: true}
        };
      }

      function updateSpecimens(navigateTo) {
        var sdeSampleSvc = $injector.get('sdeSample');

        var specimens = [];
        angular.forEach($scope.customFieldGroups,
          function(group) {
            if (group.noMatch || group.visitFields) {
              return;
            }

            specimens = specimens.concat(group.input);
          }
        );


        var visitToSave = undefined;
        if ($scope.customFieldGroups[0].visitFields) {
          visitToSave = $scope.customFieldGroups[0].input[0].visit;
        }

        if (specimens.length > 0) {
          specimens[0].visit = visitToSave;
          sdeSampleSvc.updateSamples(specimens).then(navigateTo);
        } else if (visitToSave) {
          new Visit(visitToSave).$saveOrUpdate().then(navigateTo);
        } else {
          navigateTo();
        }
      }

      function getReservationToCancel() {
        for (var i = 0; i < $scope.specimens.length; ++i) {
          var loc = $scope.specimens[i].storageLocation;
          if (loc && loc.reservationId) {
            return loc.reservationId;
          }
        }

        return undefined;
      }

      $scope.openSpecimenNode = function(specimen) {
        specimen.isOpened = true;
      };

      $scope.closeSpecimenNode = function(specimen) {
        specimen.isOpened = false;
      };

      $scope.remove = function(specimen) {
        var idx = $scope.specimens.indexOf(specimen);
        var descCnt = descendentCount(specimen);

        for (var i = idx + descCnt; i >= idx; --i) {
          $scope.specimens[i].selected = false;
          $scope.specimens[i].removed = true;
          $scope.specimens.splice(i, 1);
        }

        if (specimen.grpLeader) {
          var grp = specimen.grpLeader.aliquotGrp;
          var grpIdx = grp.indexOf(specimen);
          grp.splice(grpIdx, 1);
        } else if (specimen.aliquotGrp) {
          if (!specimen.expanded) {
            angular.forEach(specimen.aliquotGrp, function(aliquot) {
              aliquot.selected = false;
              aliquot.removed = true;
            });
          } else {
            // logic of changing group leader.
            adjustGrpLeader(specimen);
          }
        }
      };

      function adjustGrpLeader(specimen) {
        if (!specimen.aliquotGrp) {
          return;
        }

        var members = specimen.aliquotGrp.splice(1);
        var newLeader = members.length > 0 ? members[0] : null;
        if (!newLeader) {
          return;
        }

        newLeader.aliquotGrp = members;
        newLeader.expanded = true;
        newLeader.grpLeader = null;
        angular.forEach(members, function(member) {
          if (member != newLeader) {
            member.grpLeader = newLeader;
          }
        });
      }

      function handleSpecimensPoolStatus(specimen) {
        var pooledSpmn = specimen.pooledSpecimen;
        if (!pooledSpmn) {
          return;
        }

        var allSameStatus = pooledSpmn.specimensPool.every(
          function(s) {
            return s.status == specimen.status;
          }
        );

        if (allSameStatus|| pooledSpmn.status == 'Missed Collection') {
          pooledSpmn.status = specimen.status;
        } else if (specimen.status != 'Collected' && pooledSpmn.status == 'Collected') {
          var atLeastOneColl = pooledSpmn.specimensPool.some(
            function(s) {
              return s.status == 'Collected';
            }
          );

          if (!atLeastOneColl) {
            pooledSpmn.status = specimen.status;
          }
        }
      }  

      $scope.statusChanged = function(specimen) {
        if (!specimen) {
          return;
        }

        setDescendentStatus(specimen); 

        if (specimen.status == 'Collected') {
          var curr = specimen.parent;
          while (curr) {
            curr.status = specimen.status;
            curr = curr.parent;
          }
        }
        
        handleSpecimensPoolStatus(specimen);

        if (!specimen.expanded) {
          angular.forEach(specimen.aliquotGrp, function(sibling) {
            sibling.status = specimen.status;
          });
        }
      };

      $scope.togglePrintLabels = setAliquotGrpPrintLabel;
        
      $scope.saveSpecimens = function() {
        var prop = $scope.barcodingEnabled ? 'barcode' : 'label';
        if (areDupInputsPresent($scope.specimens, prop)) {
          Alerts.error('specimens.errors.duplicate_' + prop + 's');
          return;
        }

        if (!ignoreQtyWarning && !areAliquotsQtyOk($scope.specimens)) {
          return;
        }

        var sd = CollectSpecimensSvc.getStateDetail() || {};
        var navigateTo = function() { return $scope.back; };
        if (sd.state && sd.state.name) {
          navigateTo = function(visit) {
            return function() {
              $state.go(sd.state.name, angular.extend(sd.params, {visitId: visit.id}));
            }
          }
        }

        var specimensToSave = getSpecimensToSave($scope.cp, $scope.specimens, []);
        var savedSpmnReqIds = getSpmnReqIds(specimensToSave);
        if (cp.specimenCentric || (!!$scope.visit.id && $scope.visit.status == 'Complete')) {
          Specimen.save(specimensToSave).then(
            function(savedSpecimens) {
              $scope.specimens.length = 0;
              CollectSpecimensSvc.clear();
              displayCustomFieldGroups(savedSpmnReqIds, savedSpecimens, navigateTo($scope.visit))
            }
          );
        } else {
          var visitToSave = angular.copy($scope.visit);
          visitToSave.status = 'Complete';

          var payload = {visit: visitToSave, specimens: specimensToSave};
          Visit.collectVisitAndSpecimens(payload).then(
            function(result) {
              angular.extend(visit, result.data.visit);

              $scope.specimens.length = 0;
              CollectSpecimensSvc.clear();
              displayCustomFieldGroups(savedSpmnReqIds, result.data.specimens, navigateTo(visit));
            }
          );
        }
      };

      function descendentCount(specimen, onlySelected) {
        onlySelected = (onlySelected != false);

        var count = 0;
        angular.forEach(specimen.children, function(child) {
          if (child.removed || (!child.selected && onlySelected)) {
            return;
          }

          count += 1 + descendentCount(child);
        });

        angular.forEach(specimen.specimensPool, function(poolSpmn) {
          if (poolSpmn.removed || (!poolSpmn.selected && onlySelected)) {
            return;
          }

          count += 1 + descendentCount(poolSpmn);
        });

        return count;
      };

      function setDescendentStatus(specimen) {
        angular.forEach(specimen.specimensPool, 
          function(poolSpmn) {
            poolSpmn.status = specimen.status;
          }
        );

        angular.forEach(specimen.children,
          function(child) {
            child.status = specimen.status;
            setDescendentStatus(child);
          }
        );
      };

      function areDupInputsPresent(input, prop) {
        var values = [];
        for (var i = 0; i < input.length; ++i) {
          if (!!input[i][prop] && values.indexOf(input[i][prop]) != -1) {
            return true;
          }

          values.push(input[i][prop]);
        }

        return false;
      }

      function getSpecimensToSave(cp, uiSpecimens, visited) {
        var result = [];
        angular.forEach(uiSpecimens, 
          function(uiSpecimen) {
            if (visited.indexOf(uiSpecimen) >= 0 || // already visited
                !uiSpecimen.selected || // not selected
                (uiSpecimen.existingStatus == 'Collected' && 
                !uiSpecimen.closeAfterChildrenCreation)) {
                // collected and not close after children creation
              return;
            }

            visited.push(uiSpecimen);

            if ((cp.manualSpecLabelEnabled || !uiSpecimen.labelFmt) && !uiSpecimen.label) {
              if (!uiSpecimen.grpLeader.expanded) {
                //
                // Specimen label is not specified when expected but aliquot group is
                // in collapsed state. Therefore ignore the specimen or do not save
                //
                return;
              }
            }

            var specimen = getSpecimenToSave(uiSpecimen);
            specimen.children = getSpecimensToSave(cp, uiSpecimen.children, visited);
            specimen.specimensPool = getSpecimensToSave(cp, uiSpecimen.specimensPool, visited);
            result.push(specimen);
            return result;
          }
        );

        return result;
      };

      function getSpecimenToSave(uiSpecimen) { // Make it object Specimen and do checks like isNew/isCollected
        var specimen = {
          id: uiSpecimen.id,
          initialQty: uiSpecimen.initialQty,
          label: uiSpecimen.label,
          barcode: uiSpecimen.barcode,
          printLabel: uiSpecimen.printLabel,
          cpId: cp.id,
          reqId: uiSpecimen.reqId,
          visitId: $scope.visit.id,
          storageLocation: uiSpecimen.storageLocation,
          parentId: angular.isDefined(uiSpecimen.parent) ? uiSpecimen.parent.id : undefined,
          lineage: uiSpecimen.lineage,
          concentration: uiSpecimen.concentration,
          status: uiSpecimen.status,
          closeAfterChildrenCreation: uiSpecimen.closeAfterChildrenCreation,
          createdOn: uiSpecimen.lineage != 'New' ? uiSpecimen.createdOn : undefined,
          freezeThawCycles: uiSpecimen.freezeThawCycles,
          incrParentFreezeThaw: uiSpecimen.incrParentFreezeThaw,
          comments: uiSpecimen.comments,
          extensionDetail: uiSpecimen.extensionDetail
        };

        if (specimen.lineage == 'New' && specimen.status == 'Collected') {
          var collEvent = specimen.collectionEvent = uiSpecimen.collectionEvent || {};
          var recvEvent = specimen.receivedEvent = uiSpecimen.receivedEvent || {};

          if ($scope.showCollVisitDetails) {
            var collDetail = $scope.collDetail;
            angular.extend(collEvent, {user: collDetail.collector, time: collDetail.collectionDate});
            angular.extend(recvEvent, {user: collDetail.receiver,  time: collDetail.receiveDate});
          }
        }

        if (!!specimen.reqId || specimen.lineage == 'Aliquot') {
          return specimen;
        }

        specimen.specimenClass = uiSpecimen.specimenClass;
        specimen.type = uiSpecimen.type;

        if (uiSpecimen.lineage == 'Derived') {
          return specimen;
        }

        specimen.pathology = uiSpecimen.pathology;
        specimen.anatomicSite = uiSpecimen.anatomicSite;
        specimen.laterality = uiSpecimen.laterality;
        return specimen;
      };

      function areAliquotsQtyOk(specimens) {
        for (var i = 0; i < specimens.length; i++) {
          var specimen = specimens[i];
          if (!specimen.children || specimen.children.length == 0) {
            continue;
          }

          var aliquots = specimen.children.filter(
            function(child) {
               return child.selected && child.lineage == 'Aliquot' && child.existingStatus != 'Collected';
             }
          );

          var aliquotsQty = aliquots.reduce(
            function(sum, aliquot) {
              return sum + (!aliquot.initialQty ? 0 : +aliquot.initialQty);
            }, 0);

          var parentQty = specimen.existingStatus == 'Collected' ? specimen.availableQty : specimen.initialQty;
          if (parentQty != undefined && (aliquotsQty - parentQty) > 0.000001) {
            showInsufficientQtyWarning();
            return false;
          }

          if (!areAliquotsQtyOk(specimen.children)) {
            return false;
          }
        };

        return true;
      }

      function showInsufficientQtyWarning() {
        SpecimenUtil.showInsufficientQtyWarning({
          ok: function () {
            ignoreQtyWarning = true;
            $scope.saveSpecimens();
          }
        });
      }

      function assignInputs(aliquot, inputs, prop) {
        var inputs = Util.splitStr(inputs, /,|\t|\n/);
        var newSpmnsCnt = inputs.length - aliquot.aliquotGrp.length;
        if (newSpmnsCnt > 0) {
          addAliquotsToGrp(aliquot, newSpmnsCnt);
        }

        angular.forEach(aliquot.aliquotGrp, function(spmn, $index) {
          if ($index < inputs.length) {
            spmn[prop] = inputs[$index];
            spmn.selected = true;
            spmn.removed = false;
          }
        });
      }

      $scope.assignBarcodes = function(aliquot, barcodes) {
        assignInputs(aliquot, barcodes, 'barcode');
      }

      $scope.assignLabels = function(aliquot, labels) {
        assignInputs(aliquot, labels, 'label');
      }

      $scope.expandAliquotsGroup = function(aliquot) {
        expandOrCollapseAliquotsGrp(aliquot, true);
      }

      $scope.collapseAliquotsGroup = function(aliquot) {
        expandOrCollapseAliquotsGrp(aliquot, false);
      }

      $scope.changeQuantity = function(specimen, qty) {
        ignoreQtyWarning = false;
        if (!specimen.expanded) {
          angular.forEach(specimen.aliquotGrp, function(sibling) {
            sibling.initialQty = qty;
          });
        }
      }

      $scope.updateCount = function(specimen) {
        var grp = specimen.aliquotGrp;
        var grpLen = grp.length;

        if (specimen.newAliquotsCnt < grpLen) {
          removeAliquotsFromGrp(specimen, grpLen - specimen.newAliquotsCnt);
        } else {
          ignoreQtyWarning = false;
          addAliquotsToGrp(specimen, specimen.newAliquotsCnt - grpLen);
        }     

        Util.hidePopovers();
      }

      $scope.closePopover = function() {
        Util.hidePopovers();
      }

      $scope.reallocatePositions = function() {
        var specimens = [];
        for (var i = 0; i < $scope.specimens.length; ++i) {
          var spmn = $scope.specimens[i];
          if (spmn.existingStatus == 'Collected') {
            continue;
          }

          if (spmn.status == 'Collected') {
            spmn.status = '';
            specimens.push(spmn);
          } else {
            spmn.storageLocation = {};
          }
        }

        function reassignSelectedStatus() {
          angular.forEach(specimens, function(spmn) { spmn.status = 'Collected'; });
        }

        CollectSpecimensSvc.allocatePositions(visit, specimens, getReservationToCancel())
          .then(reassignSelectedStatus, reassignSelectedStatus);
      }

      $scope.selectPositionsManually = function(clearPositions) {
        $q.when(vacateReservedPositions()).then(
          function() {
            $scope.autoPosAllocate = false;
            angular.forEach($scope.specimens,
              function(spmn) {
                var loc = spmn.storageLocation;
                if (spmn.existingStatus == 'Collected' || !loc) {
                  return;
                }

                delete loc.reservationId;
                if (clearPositions) {
                  loc.positionX = loc.positionY = loc.position = undefined;
                }
              }
            );
          }
        );
      }

      $scope.applyFirstLocationToAll = function() {
        var location = {}, firstIdx = -1;
        for (var i = 0; i < $scope.specimens.length; ++i) {
          var spmn = $scope.specimens[i];
          if (spmn.existingStatus != 'Collected' && !spmn.isVirtual) {
            location = {name: spmn.storageLocation.name, mode: spmn.storageLocation.mode};
            firstIdx = i;
            break;
          }
        }

        if (firstIdx == -1) {
          return;
        }

        for (var i = 1; i < $scope.specimens.length; i++) {
          var spmn = $scope.specimens[i];
          if (spmn.existingStatus != 'Collected' && !spmn.isVirtual && firstIdx != i) {
            spmn.storageLocation = angular.extend({}, location);
          }
        }
      };

      init();
    });
