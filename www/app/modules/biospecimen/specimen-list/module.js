angular.module('os.biospecimen.specimenlist', 
  [ 
    'os.biospecimen.specimenlist.name',
    'os.biospecimen.specimenlist.addedit',
    'os.biospecimen.specimenlist.specimensholder',
    'os.biospecimen.specimenlist.assignto'
  ])

  .config(function($stateProvider) {
    $stateProvider
      .state('jpc-base-path', {
        url: '/jpc-base-path',
        templateUrl: 'modules/biospecimen/specimen-list/jpc-base-path.html',
        controller: function($scope) {
        },
        parent: 'signed-in'
      })
      .state('specimen-lists', {
        url: '/specimen-lists',
        templateUrl: 'modules/biospecimen/specimen-list/lists.html',
        controller: 'SpecimenListsCtrl',
        resolve: {
          pagerOpts: function(ListPagerOpts) {
            return new ListPagerOpts({recordsPerPage: 50});
          },
          lists: function(SpecimenList, pagerOpts) {
            return SpecimenList.query({includeStats: true, maxResults: pagerOpts.recordsPerPage + 1});
          }
        },
        parent: 'signed-in'
      })
      .state('specimen-list-root', {
        url: '/specimen-lists/:listId',
        template: '<div ui-view></div>',
        resolve: {
          list: function($stateParams, SpecimenList) {
            if (!!$stateParams.listId || $stateParams.listId === 0) {
              return SpecimenList.getById($stateParams.listId, {includeSpecimens: false});
            }

            return new SpecimenList();
          }
        },
        abstract: true,
        parent: 'signed-in'
      })
      .state('specimen-list', {
        url: '/',
        params: {
          breadcrumbs: [
            {state: 'specimen-lists', params: {}, captionKey: 'specimen_list.lists'}
          ]
        },
        templateUrl: 'modules/biospecimen/specimen-list/specimens.html',
        controller: 'SpecimenListSpecimensCtrl',
        parent: 'specimen-list-root'
      })
      .state('specimen-list-addedit', {
        url: '/specimen-list/:listId/addedit',
        templateUrl: 'modules/biospecimen/specimen-list/addedit.html',
        controller: 'AddEditSpecimenListCtrl',
        resolve: {
          barcodingEnabled: function(CollectionProtocol) {
            return CollectionProtocol.getBarcodingEnabled();
          }
        },
        parent: 'specimen-list-root'
      });
  })

  .run(function(UrlResolver) {
    UrlResolver.regUrlState('specimen-list', 'specimen-list', 'listId');
  });
