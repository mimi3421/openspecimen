angular.module('os.biospecimen.participant')
  .factory('ExportUtil', function($translate) {

    function addForms(exportTypes, group, entityType, input, forms) {
      angular.forEach(forms,
        function(form) {
          exportTypes.push({
            group: group,
            type: 'extensions',
            title: form.caption,
            '$$input': input,
            params: { entityType: entityType, formName: form.name }
          });
        });
      return exportTypes;
    }

    function msg(key) {
      return $translate.instant(key);
    }

    function getParticipantTypes(entityForms, cpId) {
      var group = $translate.instant('participant.title');
      var input = {'var': 'ppids', varName: 'participant.ppids', varDesc: 'participant.ppids_csv'};

      var exportTypes = [
        { group: group, type: 'cpr', title: msg('participant.list'), '$$input': input },
        { group: group, type: 'consent', title: msg('participant.consents'), '$$input': input }
      ];
      addForms(exportTypes, group, 'CommonParticipant', input, entityForms['CommonParticipant']);
      return addForms(exportTypes, group, 'Participant', input, entityForms['Participant']);
    } 

    function getVisitTypes(entityForms) {
      var group = $translate.instant('visits.title');
      var input = {'var': 'visitNames', varName: 'visits.names', varDesc: 'visits.names_csv'};

      var exportTypes = [
        { group: group, type: 'visit', title: msg('visits.visits_text_rpts'), '$$input': input },
        { group: group, type: 'visit', title: msg('visits.visits_pdf_rpts'), '$$input': input, params: { sprFileType: 'pdf' } }
      ];
      return addForms(exportTypes, group, 'SpecimenCollectionGroup', input, entityForms['SpecimenCollectionGroup']);
    }

    function getSpecimenTypes(cp, entityForms) {
      var group = $translate.instant('specimens.title');
      var input = {'var': 'specimenLabels', varName: 'specimens.labels', varDesc: 'specimens.labels_csv'};

      var exportTypes = [{ group: group, type: 'specimen', title: msg('specimens.list'), '$$input': input }]
      addForms(exportTypes, group, 'Specimen', input, entityForms['Specimen']);
      return addForms(exportTypes, group, 'SpecimenEvent', input, entityForms['SpecimenEvent']);
    }

    function getExportDetail(cp, allowedEntityTypes, forms) {
      var breadcrumbs, onSuccess;
      if (cp.id == -1) {
        breadcrumbs = [{state: 'cp-list', title: 'cp.list'}];
        onSuccess = {state: 'cp-list', params: {}};
      } else {
        breadcrumbs = [{state: 'cp-list-view', title: cp.shortTitle, params: '{cpId:' + cp.id + '}'}];
        onSuccess = {state: 'cp-list-view', params: {cpId: cp.id}};
      }

      var entityForms = {};
      angular.forEach(forms,
        function(form) {
          if (!entityForms[form.entityType]) {
            entityForms[form.entityType] = [];
          }

          entityForms[form.entityType].push(form);
        }
      );

      var exportTypes = [];
      if (!cp.specimenCentric && allowedEntityTypes.indexOf('Participant') >= 0) {
        exportTypes = exportTypes.concat(getParticipantTypes(entityForms, cp.id));
      }

      if (!cp.specimenCentric && allowedEntityTypes.indexOf('SpecimenCollectionGroup') >= 0) {
        exportTypes = exportTypes.concat(getVisitTypes(entityForms));
      }

      if (allowedEntityTypes.indexOf('Specimen') >= 0) {
        exportTypes = exportTypes.concat(getSpecimenTypes(cp, entityForms));
      }

      angular.forEach(exportTypes,
        function(exportType) {
          if (!exportType.params) {
            exportType.params = {};
          }

          exportType.params.cpId = cp.id;
        }
      );

      return {
        breadcrumbs: breadcrumbs,
        title: 'export.title',
        type: undefined,
        onSuccess: onSuccess,
        types: exportTypes,
        params: {
          cpId: cp.id
        }
      };
    }

    return {
      getExportDetail: function(cp, allowedEntityTypes, forms) {
        return $translate('common.none').then(
          function() {
            return getExportDetail(cp, allowedEntityTypes, forms);
          }
        );
      }
    }
  });
