
angular.module('os.biospecimen.extensions.util', [])
  .factory('ExtensionsUtil', function($modal, $parse, Form, Alerts, ApiUrls) {
    var filesUrl = ApiUrls.getBaseUrl() + 'form-files';

    function getFileDownloadUrl(formId, recordId, ctrlName, fileId) {
      var params = '?formId=' + formId +
        '&ctrlName=' + ctrlName +
        '&_reqTime=' + new Date().getTime();

      if (fileId) {
        params += '&fileId=' + fileId;
      } else {
        params += '&recordId=' + recordId;
      }

      return filesUrl + params;
    }

    function deleteRecord(record, onDeletion) {
      var modalInstance = $modal.open({
        templateUrl: 'modules/biospecimen/extensions/delete-record.html',
        controller: function($scope, $modalInstance) {
          $scope.record= record;

          $scope.yes = function() {
            Form.deleteRecord(record.formId, record.recordId).then(
              function(result) {
                $modalInstance.close();
                Alerts.success('extensions.record_deleted');
              },

              function() {
                $modalInstance.dismiss('cancel');
              }
            );
          }

          $scope.no = function() {
            $modalInstance.dismiss('cancel');
          }
        }
      });

      modalInstance.result.then(
        function() {
          if (typeof onDeletion == 'function') {
            onDeletion(record);
          }
        }
      );
    };
    
    function createExtensionFieldMap(entity, sdeMode) {
      var extensionDetail = entity.extensionDetail;
      if (!extensionDetail) {
        return;
      }

      extensionDetail.attrsMap = extensionDetail.attrsMap || {};
      angular.extend(extensionDetail.attrsMap, {id: extensionDetail.id, containerId: extensionDetail.formId});

      angular.forEach(extensionDetail.attrs, function(attr) {
        if (attr.type == 'datePicker') {
          if (!isNaN(attr.value) && !isNaN(parseInt(attr.value))) {
            attr.value = parseInt(attr.value);
          } else if (!!attr.value || attr.value === 0) {
            attr.value = new Date(attr.value);
          }
        } else if (sdeMode && attr.type == 'pvField') {
          attr.value = attr.displayValue;
        }

        extensionDetail.attrsMap[attr.name] = attr.type != 'subForm' ? attr.value : getSubformFieldMap(attr, sdeMode);
        if (attr.type != 'subForm') {
          extensionDetail.attrsMap['$$' + attr.name + '_displayValue'] = attr.displayValue;
        }
      });
    }

    function getSubformFieldMap(sf, sdeMode) {
      var attrsMap = [];
      angular.forEach(sf.value, function(attrs, idx) {
        var map = attrsMap[idx] = {};
        angular.forEach(attrs, function(attr) {
          if (attr.type == 'datePicker') {
            if (!isNaN(attr.value) && !isNaN(parseInt(attr.value))) {
              attr.value = parseInt(attr.value);
            } else if (!!attr.value || attr.value === 0) {
              attr.value = new Date(attr.value);
            }
          } else if (sdeMode && attr.type == 'pvField') {
            attr.value = attr.displayValue;
          }

          map[attr.name] = attr.value;
          map['$$' + attr.name + '_displayValue'] = attr.displayValue;
        });
      })

      return attrsMap; 
    }

    function getExtnOpts(entity, extnCtxt, disableFields) {
      if (!extnCtxt) {
        return undefined;
      }

      createExtensionFieldMap(entity, extnCtxt.sdeMode);

      return {
        formId: extnCtxt.formId,
        recordId: !!entity.extensionDetail ? entity.extensionDetail.id : undefined,
        formCtxtId: parseInt(extnCtxt.formCtxtId),
        objectId: entity.id,
        formData: !!entity.extensionDetail ? (entity.extensionDetail.attrsMap || {}): {},
        showActionBtns: false,
        showPanel: false, 
        labelAlignment: 'horizontal',
        disableFields: disableFields || []
      };
    }

    function getMatchingForms(forms, rules, ctxt) {
      var matchingRule = null;
      for (var i = 0; i < rules.length; ++i) {
        if (!rules[i].when) {
          continue;
        }

        try {
          if ($parse(rules[i].when)(ctxt)) {
            matchingRule = rules[i];
            break;
          }
        } catch (err) {
          alert('Invalid rule expression: ' + (i + 1) + ': ' + err);
        }
      }

      if (matchingRule && matchingRule.forms) {
        forms = forms.filter(function(f) { return matchingRule.forms.indexOf(f.formName) > -1; });
      }

      return forms;
    }

    function sortForms(inputForms, orderSpec) {
      if (!orderSpec || orderSpec.length == 0) {
        return inputForms;
      }

      var formsByType = {};
      angular.forEach(inputForms,
        function(form) {
          if (!formsByType[form.entityType]) {
            formsByType[form.entityType] = [];
          }

          formsByType[form.entityType].push(form);
        }
      );

      var result = [];
      angular.forEach(orderSpec,
        function(typeForms) {
          Array.prototype.push.apply(result, sortForms0(formsByType[typeForms.type], typeForms.forms));
          delete formsByType[typeForms.type];
        }
      );

      angular.forEach(inputForms,
        function(form) {
          if (formsByType[form.entityType]) {
            result.push(form);
          }
        }
      );

      return result;
    }

    function sortForms0(inputForms, orderSpec) {
      var formsById = {};
      angular.forEach(inputForms,
        function(form) {
          formsById[form.formId] = form;
        }
      );

      var result = [];
      angular.forEach(orderSpec,
        function(spec) {
          var form = formsById[spec.id];
          if (form) {
            result.push(form);
            inputForms.splice(inputForms.indexOf(form), 1);
          }
        }
      );

      Array.prototype.push.apply(result, inputForms);
      return result;
    }

    function linkFormRecords(inputForms, records) {
      var recsMap = {};
      angular.forEach(records,
        function(rec) {
          if (!recsMap[rec.fcId]) {
            recsMap[rec.fcId] = [];
          }
          recsMap[rec.fcId].push(rec);
        }
      );

      angular.forEach(inputForms,
        function(form) {
          form.records = recsMap[form.formCtxtId] || [];
        }
      );
    }

    var viewTmpls = {};

    return {
      getFileDownloadUrl: getFileDownloadUrl,

      deleteRecord: deleteRecord,

      createExtensionFieldMap: createExtensionFieldMap,

      getExtnOpts: getExtnOpts,

      getMatchingForms: getMatchingForms,

      sortForms: sortForms,

      linkFormRecords: linkFormRecords,

      registerView: function(name, viewTmpl) {
        viewTmpls[name] = viewTmpl;
      },

      getViewTmpl: function(name) {
        return viewTmpls[name];
      }
    }
 
  });
