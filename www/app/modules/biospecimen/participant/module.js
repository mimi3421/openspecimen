
angular.module('os.biospecimen.participant', 
  [ 
    'ui.router',
    'os.biospecimen.common',
    'os.biospecimen.participant.root',
    'os.biospecimen.participant.list',
    'os.biospecimen.participant.detail',
    'os.biospecimen.participant.overview',
    'os.biospecimen.participant.visits',
    'os.biospecimen.participant.addedit',
    'os.biospecimen.participant.bulkregistration',
    'os.biospecimen.participant.newreg',
    'os.biospecimen.participant.collect-specimens',
    'os.biospecimen.participant.consents',
    'os.biospecimen.participant.search',
    'os.biospecimen.visit',
    'os.biospecimen.specimen',
    'os.biospecimen.extensions.list',
    'os.biospecimen.extensions.util',
    'os.biospecimen.extensions.addedit-record',
    'os.biospecimen.specimenkit'
  ])

  .config(function($stateProvider) {
    $stateProvider
      .state('cp-view', {
        url: '/cp-view/:cpId',
        template: '<div ui-view></div>',
        controller: function($scope, cp, cpViewCtx) {
          $scope.cp = cp;
          $scope.cpViewCtx = cpViewCtx;
          cpViewCtx.codingEnabled = $scope.global.appProps.cp_coding_enabled;

          var sites = cp.cpSites.map(function(cpSite) { return cpSite.siteName; });
          $scope.partRegOpts =        {cp: cp.shortTitle, sites: sites, resource: 'ParticipantPhi', operations: ['Create']};
          $scope.orderCreateOpts =    {cp: cp.shortTitle, sites: sites, resource: 'Order', operations: ['Create']};
          $scope.shipmentCreateOpts = {cp: cp.shortTitle, sites: sites, resource: 'ShippingAndTracking', operations: ['Create']};
          $scope.specimenUpdateOpts = {cp: cp.shortTitle, sites: sites, resource: 'VisitAndSpecimen', operations: ['Update']};
          $scope.specimenDeleteOpts = {cp: cp.shortTitle, sites: sites, resource: 'VisitAndSpecimen', operations: ['Delete']};
          $scope.cpReadOpts         = {resource: 'CollectionProtocol', operations: ['Read']};
        },
        resolve: {
          cp: function($stateParams, CollectionProtocol) {
            return CollectionProtocol.getById($stateParams.cpId);
          },

          cpViewCtx: function(cp, currentUser, AuthorizationService) {
            var participantEximAllowed = AuthorizationService.isAllowed({
              resource: 'ParticipantPhi',
              operations: ['Export Import'],
              cp: cp.shortTitle
            });

            var visitSpmnEximAllowed = AuthorizationService.isAllowed({
              resource: 'VisitAndSpecimen',
              operations: ['Export Import'],
              cp: cp.shortTitle
            });

            return {
              participantImportAllowed: participantEximAllowed,
              visitSpecimenImportAllowed: visitSpmnEximAllowed,
              participantExportAllowed: participantEximAllowed,
              visitSpecimenExportAllowed: visitSpmnEximAllowed
            }
          },

          listView: function(cp, CpConfigSvc) {
            var defListViewState = !cp.specimenCentric ? 'participant-list' : 'cp-specimens';
            return CpConfigSvc.getListView(cp.id, defListViewState);
          },

          twoStepReg: function(SettingUtil) {
            return SettingUtil.getSetting('biospecimen', 'two_step_patient_reg').then(
              function(setting) {
                return setting.value == 'true';
              }
            );
          },

          mrnAccessRestriction: function(SettingUtil) {
            return SettingUtil.getSetting('biospecimen', 'mrn_restriction_enabled').then(
              function(setting) {
                return setting.value == 'true';
              }
            );
          }
        },
        parent: 'signed-in',
        abstract: true
      })
      .state('cp-summary-view', {
        url: '/summary-view',
        controller: function($state, cp, summaryView) {
          $state.go(summaryView, {cpId: cp.id}, {location: 'replace'});
        },
        resolve: {
          summaryView: function(listView, CpConfigSvc) {
            var summaryView = listView;

            var summarySt = CpConfigSvc.getSummaryState();
            if (summarySt) {
              summaryView = summarySt;
            }

            return summaryView;
          }
        },
        parent: 'cp-view'
      })
      .state('cp-list-view', {
        url: '/list-view',
        controller: function($state, cp, listView) {
          $state.go(listView, {cpId: cp.id}, {location: 'replace'});
        },
        parent: 'cp-view'
      })
      .state('cp-list-view-root', {
        templateUrl: 'modules/biospecimen/participant/list-view.html',
        controller: function($scope, cp, cpViewCtx, defSopDoc, defSopUrl, spmnListCfg, Alerts) {
          var ctx = $scope.listViewCtx = {
            sopDocDownloadUrl: cp.getSopDocDownloadUrl(),
            spmnListCfg: spmnListCfg,
            showImport: (cpViewCtx.visitSpecimenImportAllowed || (!cp.specimenCentric && cpViewCtx.participantImportAllowed)),
            showExport: (cpViewCtx.visitSpecimenExportAllowed || (!cp.specimenCentric && cpViewCtx.participantExportAllowed))
          };

          ctx.sopDoc = cp.sopDocumentName;
          if (!ctx.sopDoc) {
            ctx.sopUrl = cp.sopDocumentUrl;
            if (!ctx.sopUrl) {
              ctx.sopDoc = defSopDoc.value;
              if (!ctx.sopDoc) {
                ctx.sopUrl = defSopUrl.value;
              }
            }
          }

          $scope.generateCpReport = function() {
            cp.generateReport().then(
              function() {
                Alerts.success('participant.report_gen_initiated', cp);
              }
            );
          }
        },
        resolve: {
          defSopDoc: function(cp, SettingUtil) {
            if (!!cp.sopDocumentName || !!cp.sopDocumentUrl) {
              return null;
            }

            return SettingUtil.getSetting('biospecimen', 'cp_sop_doc');
          },

          defSopUrl: function(cp, defSopDoc, SettingUtil) {
            if (!!cp.sopDocumentName || !!cp.sopDocumentUrl || !!defSopDoc.value) {
              return null;
            }

            return SettingUtil.getSetting('biospecimen', 'cp_sop_doc_url');
          },

          spmnListCfg: function(cp, CpConfigSvc) {
            return CpConfigSvc.getListConfig(cp, 'specimen-list-view');
          }
        },
        parent: 'cp-view',
        abstract: true
      })
      .state('participant-list', {
        url: '/participants?filters',
        templateUrl: 'modules/biospecimen/participant/list.html',
        controller: 'ParticipantListCtrl',
        resolve: {
          participantListCfg: function(cp, CpConfigSvc) {
            return CpConfigSvc.getListConfig(cp, 'participant-list-view');
          }
        },
        metaInfo: {
          button: {
            icon: 'fa-group',
            label: 'cp.view_participants'
          }
        },
        parent: 'cp-list-view-root'
      })
      .state('cp-specimens', {
        url: '/specimens?filters',
        templateUrl: 'modules/biospecimen/participant/specimens-list.html',
        controller: 'SpecimensListViewCtrl',
        resolve: {
          sdeConfigured: function($injector, cp, CpConfigSvc) {
            if (!$injector.has('sdeFieldsSvc')) {
              return false;
            }

            return CpConfigSvc.getWorkflowData(cp.id, 'sde').then(
              function(data) {
                return !!data.singlePatientSamples;
              }
            );
          }
        },
        metaInfo: {
          button: {
            icon: 'fa-flask',
            label: 'cp.view_specimens'
          }
        },
        parent: 'cp-list-view-root'
      })
      .state('import-cp-objs', {
        url: '/import-cp-objs',
        templateUrl: 'modules/common/import/add.html',
        controller: 'ImportObjectCtrl',
        resolve: {
          allowedEntityTypes: function(cp, cpViewCtx) {
            var entityTypes = [];
            if (!cp.specimenCentric && cpViewCtx.participantImportAllowed) {
              entityTypes = entityTypes.concat(['CommonParticipant', 'Participant']);
            }

            if (!cp.specimenCentric && cpViewCtx.visitSpecimenImportAllowed) {
              entityTypes.push('SpecimenCollectionGroup');
            }

            if (cpViewCtx.visitSpecimenImportAllowed) {
              entityTypes.push('Specimen');
              entityTypes.push('SpecimenEvent');
            }

            return entityTypes;
          },

          forms: function(cp, allowedEntityTypes) {
            return allowedEntityTypes.length > 0 ? cp.getForms(allowedEntityTypes) : [];
          },

          importDetail: function(cp, allowedEntityTypes, forms, ImportUtil) {
            return ImportUtil.getImportDetail(cp, allowedEntityTypes, forms);
          }
        },
        parent: 'cp-view'
      })
      .state('import-cp-jobs', {
        url: '/import-cp-jobs',
        templateUrl: 'modules/common/import/list.html',
        controller: 'ImportJobsListCtrl',
        resolve: {
          importDetail: function(cp) {
            return {
              breadcrumbs: [
                {state: 'cp-list-view', title:  cp.shortTitle,     params: '{cpId:' + cp.id + '}'}
              ],
              title: 'bulk_imports.jobs_list',
              objectTypes: [
                'cpr', 'participant', 'consent', 'visit',
                'specimen', 'specimenDerivative', 'specimenAliquot',
                'masterSpecimen', 'specimenDisposal', 'extensions'
              ],
              objectParams: {cpId: cp.id}
            }
          }
        },
        parent: 'cp-view'
      })
      .state('export-cp-objs', {
        url: '/export-cp-objs',
        templateUrl: 'modules/common/export/add.html',
        controller: 'AddEditExportJobCtrl',
        resolve: {
          allowedEntityTypes: function(cp, cpViewCtx) {
            var entityTypes = [];
            if (!cp.specimenCentric && cpViewCtx.participantExportAllowed) {
              entityTypes = entityTypes.concat(['CommonParticipant', 'Participant']);
            }

            if (!cp.specimenCentric && cpViewCtx.visitSpecimenExportAllowed) {
              entityTypes.push('SpecimenCollectionGroup');
            }

            if (cpViewCtx.visitSpecimenExportAllowed) {
              entityTypes.push('Specimen');
              entityTypes.push('SpecimenEvent');
            }

            return entityTypes;
          },

          forms: function(cp, allowedEntityTypes) {
            return allowedEntityTypes.length > 0 ? cp.getForms(allowedEntityTypes) : [];
          },

          exportDetail: function(cp, allowedEntityTypes, forms, ExportUtil) {
            return ExportUtil.getExportDetail(cp, allowedEntityTypes, forms);
          }
        },
        parent: 'cp-view'
      })
      .state('participant-root', {
        url: '/participants/:cprId',
        template: '<div ui-view></div>',
        resolve: {
          cpr: function($stateParams, cp, Participant, CollectionProtocolRegistration) {
            if (!!$stateParams.cprId && $stateParams.cprId > 0) {
              return CollectionProtocolRegistration.getById($stateParams.cprId);
            } 

            var participant = new Participant({source: 'OpenSpecimen'});
            return new CollectionProtocolRegistration({
              cpId: cp.id, cpShortTitle: cp.shortTitle,
              registrationDate: new Date(), participant: participant,
              specimenLabelFmt: cp.specimenLabelFmt,
              derivativeLabelFmt: cp.derivativeLabelFmt,
              aliquotLabelFmt: cp.aliquotLabelFmt
            });
          },

          hasSde: function($injector) {
            return $injector.has('sdeFieldsSvc');
          },

          sysDict: function($stateParams, hasSde, CpConfigSvc) {
            if (!hasSde) {
              return [];
            }

            return undefined || []; // CpConfigSvc.getDictionary(null);
          },

          cpDict: function($stateParams, hasSde, CpConfigSvc) {
            if (!hasSde) {
              return [];
            }

            return CpConfigSvc.getDictionary($stateParams.cpId, []);
          },

          layout: function($stateParams, hasSde, CpConfigSvc) {
            if (!hasSde) {
              return [];
            }

            return CpConfigSvc.getLayout($stateParams.cpId, []);
          },

          hasDict: function(hasSde, sysDict, cpDict) {
            return hasSde && (cpDict.length > 0 || sysDict.length > 0);
          },

          sysLookupFields: function(CpConfigSvc) {
            return CpConfigSvc.getWorkflowData(-1, 'participantLookup', []);
          },

          lookupFieldsCfg: function(hasSde, twoStepReg, sysLookupFields, cpDict) {
            var configured = true;
            if (!hasSde) {
              return {configured: twoStepReg, fields: []};
            }

            var cprFields = cpDict.filter(function(field) { return field.name.indexOf('cpr.') == 0; });
            var lookupFields = cprFields.filter(function(field) { return field.participantLookup == true; });
            if (lookupFields.length == 0) {
              configured = false;
              lookupFields = cprFields;
            }

            if (lookupFields.length > 0) {
              return {configured: configured, fields: lookupFields};
            }

            return {configured: sysLookupFields.length > 0, fields: sysLookupFields};
          },

          hasFieldsFn: function($injector, hasDict, sysDict, cpDict) {
            return function(inObjs, exObjs) {
              if (!hasDict) {
                return true;
              }

              return $injector.get('sdeFieldsSvc').commonFns()
                .hasFields(sysDict, cpDict, inObjs, exObjs);
            }
          },

          pendingSpmnsDispInterval: function(SettingUtil) {
            return SettingUtil.getSetting('biospecimen', 'pending_spmns_disp_interval');
          },

          barcodingEnabled: function(cp, SettingUtil) {
            if (cp.barcodingEnabled) {
              return true;
            }

            return SettingUtil.getSetting('biospecimen', 'enable_spmn_barcoding').then(
              function(setting) {
                return setting.value.toLowerCase() == 'true';
              }
            );
          },

          spmnBarcodesAutoGen: function(cp, SettingUtil) {
            if (!!cp.specimenBarcodeFmt) {
              return true;
            }

            return SettingUtil.getSetting('biospecimen', 'specimen_barcode_format').then(
              function(setting) {
                return !!setting.value;
              }
            );
          },

          headers: function($q, cp, CpConfigSvc) {
            var ph = CpConfigSvc.getCommonCfg(cp.id, 'participantHeader');
            var vh = CpConfigSvc.getCommonCfg(cp.id, 'visitHeader');
            var sh = CpConfigSvc.getCommonCfg(cp.id, 'specimenHeader');

            return $q.all([ph, vh, sh]).then(
              function(headers) {
                return {participant: headers[0], visit: headers[1], specimen: headers[2]};
              }
            );
          },

          participantSpmnsViewState: function(cp, cpr, pendingSpmnsDispInterval, ParticipantSpecimensViewState) {
            return new ParticipantSpecimensViewState(cp, cpr, +pendingSpmnsDispInterval.value);
          },

          aliquotQtyReq: function(SettingUtil) {
            return SettingUtil.getSetting('biospecimen', 'mandatory_aliquot_qty').then(
              function(resp) {
                return resp.value == 'true' || resp.value == true || resp.value == '1' || resp.value == 1;
              }
            );
          }
        },
        controller: 'ParticipantRootCtrl',
        parent: 'cp-view',
        abstract: true
      })
      .state('participant-addedit', {
        url: '/addedit-participant?twoStep',
        templateProvider: function($stateParams, $q, CpConfigSvc, PluginReg) {
          return $q.when(CpConfigSvc.getRegParticipantTmpl($stateParams.cpId, $stateParams.cprId)).then(
            function(tmpl) {
              var tmpls = PluginReg.getTmpls("participant-addedit", "page-body", tmpl); 
              return '<div ng-include src="\'' + tmpls[0] + '\'"></div>';
            }
          );
        },
        controllerProvider: function($stateParams, CpConfigSvc) {
          return CpConfigSvc.getRegParticipantCtrl($stateParams.cpId, $stateParams.cprId);
        },
        resolve: {
          extensionCtxt: function(cp, Participant) {
            return Participant.getExtensionCtxt({cpId: cp.id});
          },
          addPatientOnLookupFail: function(SettingUtil) {
            return SettingUtil.getSetting('biospecimen', 'add_patient_on_lookup_fail').then(
              function(setting) {
                return setting.value == 'true';
              }
            );
          },
          lockedFields: function(cpr, CpConfigSvc) {
            var participant = cpr.participant || {};
            return CpConfigSvc.getLockedParticipantFields(participant.source || 'OpenSpecimen');
          },
          firstCpEvent: function(cp, cpr, CollectionProtocolEvent) {
            if (!!cpr.id) {
              return null;
            }

            return CollectionProtocolEvent.listFor(cp.id).then(
              function(events) {
                return events.length > 0 ? events[0] : null;
              }
            );
          }
        },
        parent: 'participant-root'
      })
      .state('participant-bulkreg', {
        url: '/bulk-registration',
        templateUrl: 'modules/biospecimen/participant/bulk-registration.html',
        controller: 'BulkRegistrationCtrl',
        resolve: {
          events: function(cp, CollectionProtocolEvent) {
            return CollectionProtocolEvent.listFor(cp.id);
          }
        },
        parent: 'participant-root'
      })
      .state('participant-detail', {
        url: '/detail',
        templateUrl: 'modules/biospecimen/participant/detail.html',
        resolve: {
          visits: function($stateParams, Visit) {
            return Visit.listFor($stateParams.cprId, true);
          }
        },
        controller: 'ParticipantDetailCtrl',
        parent: 'participant-root'
      })
      .state('participant-newreg', {
        url: '/newreg',
        templateUrl: 'modules/biospecimen/participant/register-new.html',
        controller: 'RegisterNewCtrl',
        parent: 'participant-detail'
      })
      .state('participant-detail.overview', {
        url: '/overview',
        templateProvider: function($q, PluginReg) {
          var defTmpl  = 'modules/biospecimen/participant/overview.html';
          return $q.when(PluginReg.getTmpls('participant-detail', 'overview', defTmpl)).then(
            function(tmpls) {
              return '<div ng-include src="\'' + tmpls[0] + '\'"></div>';
            }
          );
        },
        resolve: {
          storePhi: function(SettingUtil) {
            return SettingUtil.getSetting('biospecimen', 'store_phi').then(
              function(setting) {
                return setting.value == 'true';
              }
            );
          },
          consents: function(hasDict, hasFieldsFn, cpr) {
            return (hasDict && hasFieldsFn(['consents'])) ? cpr.getConsents() : null;
          },
          visitsTab: function(cp, CpConfigSvc) {
            return CpConfigSvc.getWorkflowData(cp.id, 'visitsTab', {});
          }
        },
        controller: 'ParticipantOverviewCtrl',
        parent: 'participant-detail'
      })
      .state('participant-detail.consents', {
        url: '/consents',
        templateUrl: 'modules/biospecimen/participant/consents.html',
        resolve: {
          consent: function(cpr) {
            return cpr.getConsents();
          }
        },
        controller: 'ParticipantConsentsCtrl',
        parent: 'participant-detail'
      })
      .state('participant-detail.visits', {
        url: '/visits-summary?eventId&visitId',
        templateUrl: 'modules/biospecimen/participant/visits.html',
        controller: 'ParticipantVisitsTreeCtrl',
        resolve: {
          specimens: function($stateParams, Specimen) {
            if (!$stateParams.visitId && !$stateParams.eventId) {
              return [];
            }

            var visitDetail = {
              visitId: $stateParams.visitId, 
              eventId: $stateParams.eventId
            };
            return Specimen.listFor($stateParams.cprId, visitDetail);
          }
        },
        parent: 'participant-detail'
      })
      .state('participant-detail.specimens', {
        url: '/specimens',
        templateUrl: 'modules/biospecimen/participant/reg-specimens.html',
        controller: 'RegSpecimensCtrl',
        resolve: {
          specimens: function(cpr, Specimen) {
            return Specimen.listFor(cpr.id);
          }
        },
        parent: 'participant-detail'
      })
      .state('participant-detail.collect-specimens', {
        url: '/collect-specimens?visitId&eventId',
        template: '<div ui-view></div>',
        controller: function() { },
        resolve: {
          visit: function($stateParams, cp, cpr, Visit) {
            if (!!$stateParams.visitId && $stateParams.visitId > 0) {
              return Visit.getById($stateParams.visitId);
            } else if (!!$stateParams.eventId) {
              return Visit.getAnticipatedVisit($stateParams.eventId, cpr.registrationDate);
            } else if (cp.specimenCentric) {
              return new Visit({cpId: cp.id});
            }

            return null;
          },
          latestVisit: function($stateParams, cpr) {
            //
            // required for lastest visit clinical diagnosis.
            //
            return ($stateParams.visitId || !cpr || !cpr.id) ? null : cpr.getLatestVisit();
          },
          spmnCollFields: function($stateParams, hasSde, cp, CpConfigSvc) {
            if (!hasSde) {
              return {};
            }

            return CpConfigSvc.getWorkflowData(cp.id, 'specimenCollection').then(
              function(data) {
                return data || {};
              }
            );
          }
        },
        abstract: true,
        parent: 'participant-root'
      })
      .state('participant-detail.collect-specimens.tree', {
        url: '/tree',
        templateUrl: 'modules/biospecimen/participant/collect-specimens.html',
        controller: 'CollectSpecimensCtrl',
        parent: 'participant-detail.collect-specimens'
      })
      .state('participant-detail.collect-specimens.nth-step', {
        url: '/nth-step',
        templateUrl: 'modules/biospecimen/participant/collect-specimens-nth-step.html',
        controller: 'CollectSpecimensNthStepCtrl',
        parent: 'participant-detail.collect-specimens'
      })
      .state('participant-detail.extensions', {
        url: '/extensions',
        template: '<div ui-view></div>',
        controller: function($scope, cpr, forms, records, ExtensionsUtil) {
          $scope.extnOpts = {
            update: $scope.participantResource.updateOpts,
            isEntityActive: cpr.activityStatus == 'Active',
            entity: cpr
          }

          ExtensionsUtil.linkFormRecords(forms, records);
        },
        resolve: {
          orderSpec: function(cp, CpConfigSvc) {
            return CpConfigSvc.getWorkflowData(cp.id, 'forms', {}).then(
              function(wf) {
                return [
                  {type: 'CommonParticipant', forms: wf['CommonParticipant'] || []},
                  {type: 'Participant', forms: wf['Participant'] || []}
                ];
              }
            );
          },
          forms: function(cpr, orderSpec, ExtensionsUtil) {
            return cpr.getForms().then(
              function(forms) {
                return ExtensionsUtil.sortForms(forms, orderSpec);
              }
            )
          },
          records: function(cpr) {
            return cpr.getRecords();
          },
          viewOpts: function() {
            return {
              goBackFn: null,
              showSaveNext: true
            };
          }
        },
        abstract: true,
        parent: 'participant-detail'
      })
      .state('participant-detail.extensions.list', {
        url: '/list?formId&formCtxtId&recordId',
        templateUrl: 'modules/biospecimen/extensions/list.html',
        controller: 'FormsListCtrl', 
        parent: 'participant-detail.extensions'
      })
      .state('participant-detail.extensions.addedit', {
        url: '/addedit?formId&recordId&formCtxId',
        templateUrl: 'modules/biospecimen/extensions/addedit.html',
        resolve: {
          formDef: function($stateParams, Form) {
            return Form.getDefinition($stateParams.formId);
          },
          postSaveFilters: function() {
            return [];
          }
        },
        controller: 'FormRecordAddEditCtrl',
        parent: 'participant-detail.extensions'
      })
      .state('bulk-registrations', {
        url: '/bulk-registrations',
        templateProvider: function($stateParams, $q, CpConfigSvc) {
          return $q.when(CpConfigSvc.getBulkRegParticipantTmpl($stateParams.cpId, $stateParams.cprId)).then(
            function(tmpl) {
              return '<div ng-include src="\'' + tmpl + '\'"></div>';
            }
          );
        },
        controllerProvider: function($stateParams, CpConfigSvc) {
          return CpConfigSvc.getBulkRegParticipantCtrl($stateParams.cpId, $stateParams.cprId);
        },
        parent: 'participant-root'
      })
      .state('participant-search', {
        url: '/participant-search',
        templateUrl: 'modules/biospecimen/participant/search-result.html',
        resolve: {
          participants: function(ParticipantSearchSvc) {
            return ParticipantSearchSvc.getParticipants();
          },

          searchKey: function(ParticipantSearchSvc) {
            return ParticipantSearchSvc.getSearchKey();
          }
        },
        controller: 'ParticipantResultsView',
        parent: 'signed-in'
      });
  })

  .run(function(QuickSearchSvc, ParticipantSearchSvc) {
    var opts = {
      template: 'modules/biospecimen/participant/quick-search.html',
      caption: 'entities.participant',
      order : 1,
      search: ParticipantSearchSvc.search
    };

    QuickSearchSvc.register('participant', opts);
  });
