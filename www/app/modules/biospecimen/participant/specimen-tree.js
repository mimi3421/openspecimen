
angular.module('os.biospecimen.participant.specimen-tree', 
  [
    'os.biospecimen.models', 
    'os.biospecimen.participant.collect-specimens',
  ])
  .directive('osSpecimenTree', function(
    $state, $stateParams, $modal, $timeout, $rootScope, $q, $injector,
    CpConfigSvc, CollectSpecimensSvc, Visit, Specimen, SpecimenLabelPrinter, SpecimensHolder,
    ExtensionsUtil, DistributionOrder, DistributionProtocol, Alerts, Util, DeleteUtil, SpecimenUtil) {

    var allowedDps = undefined;

    function openSpecimenTree(specimens) {
      angular.forEach(specimens, function(specimen) {
        specimen.isOpened = true;
        openSpecimenTree(specimen.children);
      });
    }

    function toggleAllSelected(selection, specimens, specimen) {
      if (!specimen.selected) {
        selection.all = false;
        return;
      }

      for (var i = 0; i < specimens.length; ++i) {
        if (!specimens[i].selected) {
          selection.all = false;
          return;
        }
      }

      selection.all = true;
    };

    function selectParentSpecimen(specimen) {
      if (!specimen.selected) {
        return false;
      }

      var parent = specimen.parent;
      while (parent) {
        parent.selected = true;
        parent = parent.parent;
      }
    };

    function isAnySelected(specimens) {
      for (var i = 0; i < specimens.length; ++i) {
        if (specimens[i].selected) {
          return true;
        }
      }

      return false;
    }

    function isAnyChildOrPoolSpecimenSelected(specimen) {
      if (!!specimen.specimensPool) {
        for (var i = 0; i < specimen.specimensPool.length; ++i) {
          if (specimen.specimensPool[i].selected) {
            return true;
          }
        }
      }

      if (!specimen.children) {
        return false;
      }

      for (var i = 0; i < specimen.children.length; ++i) {
        if (specimen.children[i].selected) {
          return true;
        }

        if (isAnyChildOrPoolSpecimenSelected(specimen.children[i])) {
          return true;
        }
      }

      return false;
    };

    function getState() {
      return {state: $state.current, params: $stateParams};
    };

    function showSelectSpecimens(msgCode) {
      if (!msgCode) {
        return;
      }

      Alerts.error(msgCode);
    };

    function getSelectedSpecimens (scope, message, anyStatus) {
      if (!scope.selection.any) {
        showSelectSpecimens(message);
        return [];
      }

      var specimens = [];
      angular.forEach(scope.specimens, function(specimen) {
        if (!specimen.selected) {
          return;
        }

        if ((specimen.status == 'Collected' || anyStatus) && specimen.id) {
          specimens.push(specimen);
        }
      });

      if (specimens.length == 0) {
        showSelectSpecimens(message);
      }

      return specimens;
    };

    function shouldHidePendingSpmns(collectionDate, pendingSpmnsDispInterval) {
      var hidePendingSpmns = false;

      if (!collectionDate || !pendingSpmnsDispInterval) {
        return hidePendingSpmns;
      }

      var dispCutOff = new Date(collectionDate);
      dispCutOff.setDate(dispCutOff.getDate() + pendingSpmnsDispInterval);
      return dispCutOff.getTime() < (new Date().getTime());
    }

    function onlyPendingSpmns(specimens) {
      return (specimens || []).every(
        function(spmn) {
          return !spmn.status || spmn.status == 'Pending';
        }
      );
    }

    function anyPendingSpmnsInTree(specimens) {
      return specimens.some(
        function(spmn) {
          if (!spmn.status || spmn.status == 'Pending') {
            return true;
          }

          if (!!spmn.children && anyPendingSpmnsInTree(spmn.children)) {
            return true;
          }

          if (!!spmn.specimensPool && anyPendingSpmnsInTree(spmn.specimensPool)) {
            return true;
          }
        }
      );
    }

    function initSdeTreeFields(scope) {
      var fieldsSvc = $injector.get('sdeFieldsSvc');

      angular.forEach(scope.specimens,
        function(spmn) {
          ExtensionsUtil.createExtensionFieldMap(spmn);
        }
      );

      var cpDictQ = CpConfigSvc.getDictionary(scope.cp.id, []);
      var fieldsQ = CpConfigSvc.getWorkflowData(scope.cp.id, 'specimenTree', []);
      $q.all([cpDictQ, fieldsQ]).then(
        function(resps) {
          scope.dispTree = true;

          var cpDict = resps[0] || [];
          var fields = (resps[1] && resps[1].fields) || [];
          scope.fields = fieldsSvc.commonFns().overrideFields(cpDict, fields);
          if (fields.length == 0) {
            return;
          }

          angular.forEach(scope.specimens,
            function(specimen) {
              var obj = {specimen: specimen};
              specimen.$$treeFields = fields.map(
                function(field) {
                  var result = {type: field.type, value: undefined};
                  if (field.type == 'specimen-desc') {
                    return result;
                  }

                  $q.when(fieldsSvc.commonFns().getValue({field: field}, obj)).then(
                    function(value) {
                      result.value = value;
                    }
                  );

                  return result;
                }
              );
            }
          );

          scope.hasDict = true;
        }
      );
    }

    return {
      restrict: 'E',

      scope: {
        cp: '=',
        cpr: '=',
        visit: '=',
        specimenTree: '=specimens',
        allowedOps: '=',
        reload: '&reload',
        collectionDate: '=?',
        pendingSpmnsDispInterval: '=?'
      },

      templateUrl: 'modules/biospecimen/participant/specimens.html',

      link: function(scope, element, attrs) {

        scope.hasDict = false;
        scope.dispTree = false;
        scope.fields = [];

        scope.view = 'list';
        scope.parentSpecimen = undefined;

        scope.hidePendingSpmns = shouldHidePendingSpmns(scope.collectionDate, scope.pendingSpmnsDispInterval);
        scope.onlyPendingSpmns = onlyPendingSpmns(scope.specimenTree);
        scope.anyPendingSpmns  = anyPendingSpmnsInTree(scope.specimenTree);

        scope.specimens = Specimen.flatten(scope.specimenTree);
        openSpecimenTree(scope.specimens);
        if ($injector.has('sdeFieldsSvc')) {
          initSdeTreeFields(scope);
        } else {
          scope.dispTree = true;
        }


        scope.openSpecimenNode = function(specimen) {
          specimen.isOpened = true;
        };

        scope.closeSpecimenNode = function(specimen) {
          specimen.isOpened = false;
        };

        scope.selection = {all: false, any: false};
        scope.toggleAllSpecimenSelect = function() {
          angular.forEach(scope.specimens, function(specimen) {
            specimen.selected = scope.selection.all;
          });

          scope.selection.any = scope.selection.all;
        };

        scope.toggleSpecimenSelect = function(specimen) {
          if (specimen.status != 'Collected') {
            selectParentSpecimen(specimen);
          }

          toggleAllSelected(scope.selection, scope.specimens, specimen);

          scope.selection.any = specimen.selected ? true : isAnySelected(scope.specimens);
        };

        scope.collectSpecimens = function() {
          if (!scope.selection.any) {
            if (!scope.visit || !scope.visit.id) {
              Alerts.error('specimens.errors.visit_not_completed');
            } else {
              $state.go('specimen-addedit', {specimenId: '', visitId: scope.visit.id});
            }

            return;
          }

          var specimensToCollect = [];
          angular.forEach(scope.specimens, function(specimen) {
            if (specimen.selected) {
              specimen.isOpened = true;
              specimensToCollect.push(specimen);
            } else if (isAnyChildOrPoolSpecimenSelected(specimen)) {
              if (specimen.status != 'Collected') {
                // a parent needs to be collected first
                specimen.selected = true;
              }
              specimen.isOpened = true;
              specimensToCollect.push(specimen);
            }
          });

          var onlyCollected = true;
          for (var i = 0; i < specimensToCollect.length; ++i) {
            if (specimensToCollect[i].status != 'Collected') {
              onlyCollected = false;
              break;
            }
          }

          if (onlyCollected) {
            showSelectSpecimens('specimens.no_specimens_for_collection');
            return;
          }

          var visit = scope.visit;
          if (!visit) {
            var eventId = undefined, visitId = undefined, error = false;
            for (var i = 0; i < specimensToCollect.length; ++i) {
              if (i == 0) {
                eventId = specimensToCollect[i].eventId;
                visitId = specimensToCollect[i].visitId;
              } else if (eventId != specimensToCollect[i].eventId || visitId != specimensToCollect[i].visitId) {
                error = true;
                break;
              }
            }

            if (error) {
              Alerts.error('specimens.errors.select_same_visit_spmns');
              return;
            }

            visit = new Visit({id: visitId, eventId: eventId, cpId: scope.cp.id});
          }

          CollectSpecimensSvc.collect(getState(), visit, specimensToCollect);
        };

        scope.printSpecimenLabels = function() {
          var specimensToPrint = getSelectedSpecimens(scope, 'specimens.no_specimens_for_print', false);
          if (specimensToPrint == undefined || specimensToPrint.length == 0) {
            return;
          }

          var specimenIds = specimensToPrint.map(function(s) { return s.id; });
          SpecimenLabelPrinter.printLabels({specimenIds: specimenIds});
        };

        scope.addSpecimensToSpecimenList = function(list) {
          if (!scope.selection.any) {
            showSelectSpecimens('specimens.no_specimens_for_specimen_list');
            return;
          }
          var selectedSpecimens = [];
          getSelectedSpecimens(scope, 'specimens.no_specimens_for_specimen_list', true).map(
            function(specimen) {
              selectedSpecimens.push({id: specimen.id});
            }
          );

          if (selectedSpecimens.length == 0) {
            return;
          }

          if (!!list) {
            list.addSpecimens(selectedSpecimens).then(
              function(specimens) {
                var listType = list.getListType($rootScope.currentUser);
                Alerts.success('specimen_list.specimens_added_to_' + listType , list);
              }
            )
          } else {
            SpecimensHolder.setSpecimens(selectedSpecimens);
            $state.go('specimen-list-addedit', {listId: ''});
          }
        }

        scope.loadSpecimenTypes = function(specimenClass, notClear) {
          SpecimenUtil.loadSpecimenTypes(scope, specimenClass, notClear);
        };

        scope.showCloseSpecimen = function(specimen) {
          scope.view = 'close_specimen';
          scope.specStatus = { reason: '' };
          scope.parentSpecimen = specimen;
        };

        scope.closeSpecimen = function() {
          scope.parentSpecimen.close(scope.specStatus.reason).then(
            function() {
              scope.revertEdit();
            }
          );
        };
         
        scope.revertEdit = function() {
          scope.view = 'list';
          scope.parentSpecimen = undefined;
        }

        scope.toggleHidePendingSpmns = function() {
          scope.hidePendingSpmns = !scope.hidePendingSpmns;
        }

        scope.getSelectedSpecimens = function(anyStatus) {
          return getSelectedSpecimens(scope, '', anyStatus);
        }

        scope.initTree = function() {
          if (!scope.reload) {
            return;
          }

          scope.reload().then(
            function() {
              $timeout(function() {
                scope.specimens = Specimen.flatten(scope.specimenTree);
                openSpecimenTree(scope.specimens);
                if ($injector.has('sdeFieldsSvc')) {
                  initSdeTreeFields(scope);
                }
              });
            }
          );
        }
      }
    }
  });
