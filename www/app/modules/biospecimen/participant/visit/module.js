
angular.module('os.biospecimen.visit', [ 
    'ui.router',
    'os.biospecimen.participant.specimen-tree',
    'os.biospecimen.extensions',
    'os.biospecimen.extensions.util',
    'os.biospecimen.visit.addedit',
    'os.biospecimen.visit.spr',
    'os.biospecimen.visit.detail',
    'os.biospecimen.visit.search'
  ])
  .config(function($stateProvider) {
    $stateProvider
      .state('visit', {
        url: '/visits/:visitId',
        controller: function($state, params) {
          $state.go('visit-detail.overview', params, {location: 'replace'});
        },
        resolve: {
          params: function($stateParams, Visit) {
            return Visit.getRouteIds($stateParams.visitId);
          }
        },
        parent: 'signed-in'
      })
      .state('visit-root', {
        url: '/visits?visitId&eventId',
        template: '<div ui-view></div>',
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

          storeSpr: function(cp, SettingUtil) {
            if (cp.storeSprEnabled == undefined || cp.storeSprEnabled == null) {
              return SettingUtil.getSetting('biospecimen', 'store_spr').then(
                function(setting) {
                  return setting.value == 'true';
                }
              );
            } else {
              return cp.storeSprEnabled;
            }
          },

          showVisitActivity: function(cp, CpConfigSvc) {
            return CpConfigSvc.getCommonCfg(cp.id, 'showVisitActivity').then(
              function(value) {
                return (value === null || value === undefined || value === '') ? true : (value == true);
              }
            );
          }
        },
        controller: function($scope, cpr, visit) {
          $scope.visit = $scope.object = visit;
          $scope.entityType = 'SpecimenCollectionGroup';
          $scope.extnState = 'visit-detail.extensions.'
        },
        abstract: true,
        parent: 'participant-root'
      })
      .state('visit-addedit', {
        url: '/addedit-visit?missedVisit&newVisit',
        templateProvider: function(PluginReg, $q) {
          return $q.when(PluginReg.getTmpls("visit-addedit", "page-body", "modules/biospecimen/participant/visit/addedit.html")).then(
            function(tmpls) {
              return '<div ng-include src="\'' + tmpls[0] + '\'"></div>';
            }
          );
        },
        resolve: {
          extensionCtxt: function(cp, Visit) {
            return Visit.getExtensionCtxt({cpId: cp.id});
          },
          latestVisit: function(cpr, visit) {
            //
            // required for lastest visit CD
            //
            return visit.id ? null : cpr.getLatestVisit();
          }
        },
        controller: 'AddEditVisitCtrl',
        parent: 'visit-root'
      })
      .state('visit-detail', {
        url: '/detail',
        templateUrl: 'modules/biospecimen/participant/visit/detail.html',
        resolve: {
          specimens: function(cpr, visit, Specimen) {
            var criteria = { visitId: visit.id, eventId: visit.eventId };
            return Specimen.listFor(cpr.id, criteria);
          }
        },
        controller: 'VisitDetailCtrl',
        parent: 'visit-root'
      })
      .state('visit-detail.overview', {
        url: '/overview',
        templateProvider: function(PluginReg, $q) {
          var defTmpl = "modules/biospecimen/participant/visit/overview.html";
          return $q.when(PluginReg.getTmpls("visit-detail", "overview", defTmpl)).then(
            function(tmpls) {
              return '<div ng-include src="\'' + tmpls[0] + '\'"></div>';
            }
          );
        },
        controller: function($scope, cpr, hasFieldsFn, showVisitActivity, osRightDrawerSvc, ExtensionsUtil) {
          ExtensionsUtil.createExtensionFieldMap($scope.visit);
          $scope.visitCtx = {
            obj: {cpr: cpr, visit: $scope.visit},
            inObjs: ['visit', 'calcVisit'],
            showEdit: hasFieldsFn(['visit'], []),
            showActivity: showVisitActivity
          };

          if (showVisitActivity) {
            osRightDrawerSvc.open();
          }

          $scope.toggleShowActivity = function() {
            $scope.visitCtx.showActivity = !$scope.visitCtx.showActivity;
          }
        },
        parent: 'visit-detail'
      })
      .state('visit-detail.extensions', {
        url: '/extensions',
        template: '<div ui-view></div>',
        controller: function($scope, visit, forms, records, ExtensionsUtil) {
          $scope.extnOpts = {
            update: $scope.specimenResource.updateOpts,
            entity: visit,
            isEntityActive: visit.activityStatus == 'Active'
          }

          ExtensionsUtil.linkFormRecords(forms, records);
        },
        resolve: {
          orderSpec: function(cp, CpConfigSvc) {
            return CpConfigSvc.getWorkflowData(cp.id, 'forms', {}).then(
              function(wf) {
                return [{type: 'SpecimenCollectionGroup', forms: wf['SpecimenCollectionGroup'] || []}];
              }
            );
          },
          forms: function(visit, orderSpec, ExtensionsUtil) {
            return visit.getForms().then(
              function(forms) {
                return ExtensionsUtil.sortForms(forms, orderSpec);
              } 
            ) 
          },
          records: function(visit) {
            return visit.getRecords();
          },
          viewOpts: function() {
            return {
              goBackFn: null,
              showSaveNext: true
            };
          }
        },
        abstract: true,
        parent: 'visit-detail'
      })
      .state('visit-detail.extensions.list', {
        url: '/list?formId&formCtxtId&recordId',
        templateUrl: 'modules/biospecimen/extensions/list.html',
        controller: 'FormsListCtrl',
        parent: 'visit-detail.extensions'
      })
      .state('visit-detail.extensions.addedit', {
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
        parent: 'visit-detail.extensions'
      })
      .state('visit-detail.spr', {
        url: '/spr',
        templateUrl: 'modules/biospecimen/participant/visit/spr.html',
        controller: 'VisitSprCtrl',
        parent: 'visit-detail'
      })

      .state('visit-search', {
        url: '/visit-search',
        templateUrl: 'modules/biospecimen/participant/visit/search-result.html',
          resolve: {
            visits: function(VisitSearchSvc) {
              return VisitSearchSvc.getVisits();
            },
            searchKey: function(VisitSearchSvc) {
              return VisitSearchSvc.getSearchKey();
            }
          },
          controller: 'VisitResultsView',
          parent: 'visit-root'
       });
  })

  .run(function(QuickSearchSvc, VisitSearchSvc) {
    var opts = {
      template: 'modules/biospecimen/participant/visit/quick-search.html',
      caption: 'entities.visit',
      order : 2,
      search: VisitSearchSvc.search
    };

    QuickSearchSvc.register('visit', opts);
  });
