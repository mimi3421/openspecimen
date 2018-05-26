angular.module('os.biospecimen.specimen')
  .factory('SpecimenUtil', function(
    $modal, $q, $parse, $location,
    ParticipantSpecimensViewState, Specimen, PvManager, Alerts, Util) {

    var URL_LEN_LIMIT = 8192; // 8 KB

    function collectAliquots(scope) {
      var spec = scope.aliquotSpec;
      if (!spec.type || !spec.specimenClass) {
        return [];
      }

      var parent = scope.parentSpecimen;
      if (parent.availableQty == '') {
        delete parent.availableQty;
      }

      if (parent.initialQty == '') {
        delete parent.initialQty;
      }

      var extensionDetail = getExtensionDetail(scope);
      if (!extensionDetail) {
        extensionDetail = scope.aliquotSpec.extensionDetail;
      }

      if (!!spec.qtyPerAliquot && !!spec.noOfAliquots) {
        var requiredQty = spec.qtyPerAliquot * spec.noOfAliquots;
        if (parent.type == spec.type &&
          parent.availableQty != undefined && requiredQty > parent.availableQty &&
          !scope.ignoreQtyWarning) {

          showInsufficientQtyWarning({
            ok: function () {
              scope.ignoreQtyWarning = true;
              scope.collectAliquots();
            }
          });
          return;
        }
      } else if (spec.specimenClass == parent.specimenClass && spec.type == parent.type && !!spec.qtyPerAliquot) {
        spec.noOfAliquots = Math.floor(parent.availableQty / spec.qtyPerAliquot);
      } else if (spec.specimenClass == parent.specimenClass && spec.type == parent.type && !!spec.noOfAliquots) {
        spec.qtyPerAliquot = Math.round(parent.availableQty / spec.noOfAliquots * 10000) / 10000;
      }

      if (scope.aliquotSpec.createdOn < scope.parentSpecimen.createdOn) {
        Alerts.error("specimens.errors.created_on_lt_parent");
        return;
      } else if (scope.aliquotSpec.createdOn > Date.now()) {
        Alerts.error("specimens.errors.created_on_gt_curr_time");
        return;
      }

      parent.isOpened = parent.hasChildren = true;
      parent.depth = 0;
      parent.closeAfterChildrenCreation = spec.closeParent;
      parent.storageType = (!parent.storageLocation || !parent.storageLocation.name) && (parent.storageType || 'Virtual');

      var derived = undefined;
      if (spec.specimenClass != parent.specimenClass || spec.type != parent.type) {
        derived = getSpmnToSave(
          'Derived', spec, parent,
          Math.round(spec.qtyPerAliquot * spec.noOfAliquots),
          spec.concentration,
          scope.cpr.derivativeLabelFmt);
      }

      var aliquot = getSpmnToSave(
        'Aliquot', spec, (derived ? derived : parent),
        spec.qtyPerAliquot,
        spec.concentration,
        scope.cpr.aliquotLabelFmt);

      var aliquots = [];
      for (var i = 0; i < spec.noOfAliquots; ++i) {
        var clonedAlqt = new Specimen(aliquot);
        clonedAlqt.extensionDetail = extensionDetail;
        aliquots.push(clonedAlqt);
      }

      var specimens = [];
      if (derived) {
        derived.storageType = 'Virtual';
        derived.closeAfterChildrenCreation = spec.closeParent;
        derived.hasChildren = true;
        derived.children = [].concat(aliquots);
        parent.children = [derived];
        specimens = [derived].concat(aliquots);
      } else {
        parent.children = [].concat(aliquots);
        specimens = aliquots;
      }

      specimens.unshift(parent);
      return specimens;
    }

    function createDerivatives(scope) {
      var extensionDetail = getExtensionDetail(scope);
      if (extensionDetail) {
        scope.derivative.extensionDetail = extensionDetail;
      }

      var closeParent = scope.derivative.closeParent;
      delete scope.derivative.closeParent;

      if (scope.derivative.createdOn < scope.parentSpecimen.createdOn) {
        Alerts.error("specimens.errors.created_on_lt_parent");
        return;
      } else if (scope.derivative.createdOn > Date.now()) {
        Alerts.error("specimens.errors.created_on_gt_curr_time");
        return;
      }

      var specimensToSave = undefined;
      if (closeParent) {
        specimensToSave = [new Specimen({
          id: scope.parentSpecimen.id,
          lineage: scope.parentSpecimen.lineage,
          visitId: scope.visit.id,
          closeAfterChildrenCreation: true,
          children: [scope.derivative]
        })];
      } else {
        specimensToSave = [scope.derivative];
      }

      return Specimen.save(specimensToSave).then(
        function(result) {
          if (closeParent) {
            scope.parentSpecimen.children.push(result[0].children[0]);
            scope.parentSpecimen.activityStatus = 'Closed';
          } else {
            scope.parentSpecimen.children.push(result[0]);
          }

          if (scope.derivative.incrParentFreezeThaw) {
            scope.parentSpecimen.freezeThawCycles++;
          }

          if (scope.derivative.storageLocation) {
            delete scope.derivative.storageLocation.reservationId;
          }

          ParticipantSpecimensViewState.specimensUpdated(scope);
          scope.revertEdit();
        }
      );
    }

    function getNewDerivative(scope) {
      return new Specimen({
        parentId: scope.parentSpecimen.id,
        lineage: 'Derived',
        storageLocation: {},
        status: 'Collected',
        visitId: scope.visit.id,
        cpId: scope.visit.cpId,
        pathology: scope.parentSpecimen.pathology,
        anatomicSite: scope.parentSpecimen.anatomicSite,
        laterality: scope.parentSpecimen.laterality, 
        closeParent: false,
        createdOn : Date.now(),
        incrParentFreezeThaw: 1,
        freezeThawCycles: scope.parentSpecimen.freezeThawCycles + 1
      });
    }

    function loadSpecimenClasses(scope) {
      if (scope.classesLoaded) {
        return;
      }

      scope.specimenClasses = PvManager.getPvs('specimen-class');
      scope.classesLoaded = true;
    }

    function loadSpecimenTypes(scope, specimenClass, notClear) {
      if (!notClear) {
        scope.derivative.type = '';
      }

      if (!specimenClass) {
        scope.specimenTypes = [];
        return;
      }

      if (!scope.specimenClasses[specimenClass]) {
        scope.specimenClasses[specimenClass] = PvManager.getPvsByParent('specimen-class', specimenClass);
      }

      scope.specimenTypes = scope.specimenClasses[specimenClass];
    }

    function getExtensionDetail(scope) {
      var formCtrl = scope.deFormCtrl.ctrl;
      if (!formCtrl || !formCtrl.validate()) {
        return;
      }

      return formCtrl.getFormData();
    }

    function copyContainerName(src, array) {
      if (!src.storageLocation || !src.storageLocation.name) {
        return;
      }

      var containerName = src.storageLocation.name;
      angular.forEach(array,
        function(dst) {
          if (src == dst || src.specimenClass != dst.specimenClass || src.type != dst.type) {
            return;
          }

          if (!dst.storageLocation || containerName != dst.storageLocation.name) {
            dst.storageLocation = {name: containerName};
          }
        }
      );
    }

    function getSpecimens(labels, filterOpts, errorOpts) {
      filterOpts = filterOpts || {};
      filterOpts.label = labels;

      if (getUrlLength(filterOpts) >= URL_LEN_LIMIT) {
        Alerts.error("specimens.too_many_specimens");
        return deferred(undefined);
      }

      return Specimen.query(filterOpts).then(
        function(specimens) {
          return resolveSpecimens(labels, filterOpts.barcode, specimens, errorOpts);
        }
      );
    }

    function getUrlLength(filterOpts) {
      var url = Specimen.url();
      if (url.indexOf('http') != 0) {
        var viewUrl = $location.absUrl();
        url = viewUrl.substr(0, viewUrl.indexOf('#')) + url;
      }

      url += jQuery.param(filterOpts);
      return url.length;
    }

    function deferred(resp) {
      var deferred = $q.defer();
      deferred.resolve(resp);
      return deferred.promise;
    }

    function resolveSpecimens(labels, barcodes, specimens, errorOpts) {
      var inputs, attr;
      if (!!labels && labels.length > 0) {
        inputs = labels;
        attr = 'label';
      } else {
        inputs = barcodes;
        attr = 'barcode';
      }

      return resolveSpecimens1(inputs, attr, specimens, errorOpts);
    }

    //
    // labels could be either specimen label or barcode
    //
    function resolveSpecimens1(labels, attr, specimens, errorOpts) {
      var specimensMap = {};
      angular.forEach(specimens, function(spmn) {
        if (!specimensMap[spmn[attr]]) {
          specimensMap[spmn[attr]] = [spmn];
        } else {
          specimensMap[spmn[attr]].push(spmn);
        }
      });

      //
      // {label: label/barcode, specimens; [s1, s2], selected: s1}
      //
      var labelsInfo = [];
      var dupLabels = [], notFoundLabels = [];

      angular.forEach(labels, function(label) {
        var labelInfo = {label: label};
        var spmns = specimensMap[label];
        if (!spmns) {
          notFoundLabels.push(label);
          return;
        }

        labelInfo.specimens = spmns;
        if (spmns.length > 1) {
          dupLabels.push(labelInfo);
        } else {
          labelInfo.selected = spmns[0];
        }

        labelsInfo.push(labelInfo);
      });

      if (notFoundLabels.length != 0) {
        showError(notFoundLabels, errorOpts);
        return deferred(undefined);
      }

      if (dupLabels.length == 0) {
        return deferred(specimens);
      }

      return $modal.open({
        templateUrl: 'modules/biospecimen/participant/specimen/resolve-specimens.html',
        controller: 'ResolveSpecimensCtrl',
        resolve: {
          labels: function() {
            return dupLabels;
          },
          attr: function() {
            return attr;
          }
        }
      }).result.then(
        function(spmns) {
          //
          // Duplicate labels/barcodes info passed to modal is a sub-view of labelsInfo list;
          // therefore any updates/selection done in modal are visible in labelsInfo
          // list as well
          //
          return labelsInfo.map(
            function(labelInfo) {
              return labelInfo.selected;
            }
          );
        }
      );
    }

    function showError(notFoundLabels, errorOpts) {
      errorOpts = errorOpts || {};
      errorOpts.code = errorOpts.code || 'specimens.specimen_not_found';
      errorOpts.params = errorOpts.params || {};
      errorOpts.params.label = notFoundLabels.join(', ');
      Alerts.error(errorOpts.code, errorOpts.params);
    }

    function showInsufficientQtyWarning(opts) {
      Util.showConfirm(angular.extend({
        title: "common.warning",
        isWarning: true,
        confirmMsg: "specimens.errors.insufficient_qty",
      }, opts));
    }

    function getSpmnToSave(lineage, spec, parent, qty, concentration, fmt) {
      return new Specimen({
        lineage: lineage,
        specimenClass: spec.specimenClass,
        type: spec.type,
        parentId: parent.id,
        initialQty: qty,
        concentration: concentration,
        pathology: spec.pathology || parent.pathology,
        storageLocation: {name: '', positionX:'', positionY: ''},
        status: 'Pending',
        children: [],
        cprId: parent.cprId,
        visitId: parent.visitId,
        createdOn: spec.createdOn,
        freezeThawCycles: spec.freezeThawCycles,
        incrParentFreezeThaw: spec.incrParentFreezeThaw,
        comments: spec.comments,

        selected: true,
        parent: parent,
        depth: parent.depth + 1,
        isOpened: true,
        hasChildren: false,
        labelFmt: fmt
      });
    }

    function sdeGroupSpecimens(baseFields, groups, specimens, ctxtObjs, otherOpts) {
      var result = [];
      var unmatched = [].concat(specimens);

      groups = groups || [];
      for (var i = 0; i < groups.length; ++i) {
        var group = groups[i];
        var selectedSpmns = [];
        if (!group.criteria) {
          selectedSpmns = specimens.map(sdeGroupInput(ctxtObjs));
          unmatched.length = 0;
        } else {
          var exprs = group.criteria.rules.map(
            function(rule) {
              if (rule.op == 'exists') {
                return '!!' + rule.field;
              } else if (rule.op == 'not_exist') {
                return '!' + rule.field;
              } else {
                return rule.field + ' ' + rule.op + ' ' + rule.value;
              }
            }
          );

          var expr = $parse(exprs.join(group.criteria.op == 'AND' ? ' && ' : ' || '));
          for (var j = specimens.length - 1; j >= 0; j--) {
            if (expr({specimen: specimens[j]})) {
              selectedSpmns.unshift(sdeGroupInput(ctxtObjs)(specimens[j]));

              var uidx = unmatched.indexOf(specimens[j]);
              if (uidx > -1) {
                unmatched.splice(uidx, 1);
              }
            }
          }
        }

        if (selectedSpmns.length != 0) {
          var cofrc = (!angular.isDefined(group.enableCofrc) || group.enableCofrc == 'true' || group.enableCofrc === true);
          result.push({
            multiple: true,
            title: group.title,
            fields: { table: group.fields },
            baseFields: baseFields,
            input: selectedSpmns,
            opts: angular.extend({static: true, enableCofrc: cofrc, cofrc: cofrc}, otherOpts || {})
          });
        }
      }

      if (unmatched.length > 0) {
        result.push({
          input: unmatched.map(sdeGroupInput(ctxtObjs)),
          noMatch: true,
          opts: otherOpts || {}
        });
      }

      return result;
    }

    function sdeGroupInput(ctxtObjs) {
      return function(specimen) {
        return angular.extend({specimen: specimen}, ctxtObjs);
      };
    }

    function getAllDescendantsByProp0(groups, parentIdProp, parentId) {
      var result = [];
      angular.forEach(groups,
        function(group) {
          angular.forEach(group.input,
            function(obj) {
              if (obj.specimen && obj.specimen[parentIdProp] == parentId) {
                result.push(obj);
              }
            }
          );
        }
      );

      return result;
    }

    function getAllDescendantsByProp(groups, id, idProp, parentIdProp, allDescendants) {
      var result = getAllDescendantsByProp0(groups, parentIdProp, id);
      if (allDescendants) {
        var childrenIds = result.map(function(obj) { return obj.specimen[idProp]; });
        angular.forEach(childrenIds,
          function(childId) {
            result = result.concat(getAllDescendantsByProp(groups, childId, idProp, parentIdProp, allDescendants));
          }
        )
      }

      return result;
    }

    function sdeGroupSetChildrenValue(groups, object, prop, value, allDescendants) {
      var spmn = object.specimen;

      var idProp, parentIdProp;
      if (angular.isDefined(spmn.uid)) {
        idProp = 'uid';
        parentIdProp = 'parentUid';
      } else {
        idProp = 'id';
        parentIdProp = 'parentId';
      }

      var descendants = getAllDescendantsByProp(groups, spmn[idProp], idProp, parentIdProp, allDescendants);
      if (descendants.length == 0) {
        return;
      }

      var expr = $parse(prop);
      if (angular.isObject(value)) {
        value = angular.copy(value);
      }

      angular.forEach(descendants,
        function(descendant) {
          expr.assign(descendant, value);
        }
      );
    }

    return {
      collectAliquots: collectAliquots,

      createDerivatives: createDerivatives,

      getNewDerivative: getNewDerivative,

      loadSpecimenClasses: loadSpecimenClasses,

      loadSpecimenTypes: loadSpecimenTypes,

      copyContainerName: copyContainerName,

      getSpecimens: getSpecimens,

      resolveSpecimens: resolveSpecimens,

      showInsufficientQtyWarning: showInsufficientQtyWarning,

      sdeGroupSpecimens: sdeGroupSpecimens,

      sdeGroupSetChildrenValue: sdeGroupSetChildrenValue
    };
  })
  .controller('ResolveSpecimensCtrl', function($scope, $modalInstance, labels, attr, Alerts) {
    function init() {
      $scope.labels = labels;
      $scope.attr = attr;
    }

    $scope.cancel = function() {
      $modalInstance.dismiss('cancel');
    };

    $scope.done = function() {
      var selectedSpmns = $scope.labels.map(
        function(label) {
          return label.selected;
        }
      );

      $modalInstance.close(selectedSpmns);
    }

    init();
  });
