angular.module('os.administrative.shipment', 
  [ 
    'ui.router',
    'os.administrative.shipment.list',
    'os.administrative.shipment.addedit',
    'os.administrative.shipment.detail',
    'os.administrative.shipment.receive'
  ])

  .config(function($stateProvider) {
    $stateProvider
      .state('shipment-root', {
        abstract: true,
        template: '<div ui-view></div>',
        controller: function($scope) {
          // Shipment Authorization Options
          $scope.shipmentResource = {
            createOpts: {resource: 'ShippingAndTracking', operations: ['Create']},
            updateOpts: {resource: 'ShippingAndTracking', operations: ['Update']},
            deleteOpts: {resource: 'ShippingAndTracking', operations: ['Delete']},
            importOpts: {resource: 'ShippingAndTracking', operations: ['Export Import']}
          }
        },
        parent: 'signed-in'
      })
      .state('shipment-list', {
        url: '/shipments?filters',
        templateUrl: 'modules/administrative/shipment/list.html',     
        controller: 'ShipmentListCtrl',
        parent: 'shipment-root'
      })
      .state('shipment-addedit', {
        url: '/shipment-addedit/:shipmentId?type',
        templateUrl: 'modules/administrative/shipment/addedit.html',
        resolve: {
          shipment: function($stateParams , Shipment) {
            if ($stateParams.shipmentId) {
              return Shipment.getById($stateParams.shipmentId);
            }

            var type = $stateParams.type;
            if (type != 'SPECIMEN' && type != 'CONTAINER') {
              type = 'SPECIMEN';
            }
            return new Shipment({id: '', status: 'Pending', type: type, shipmentSpmns: [], shipmentContainers: []});
          },

          shipmentItems: function(shipment) {
            var items = [];
            if (!shipment.id) {
              return items;
            }

            if (shipment.isSpecimenShipment()) {
              items = shipment.getSpecimens(0, 10000);
            } else {
              items = shipment.getContainers(0, 10000);
            }

            return items;
          },

          isEditAllowed: function(shipment, Util) {
            var editAllowed = !shipment.status || shipment.status == 'Pending';
            return Util.booleanPromise(editAllowed);
          }
        },
        controller: 'ShipmentAddEditCtrl',
        parent: 'shipment-root'
      })
      .state('shipment-import', {
        url: '/shipment-import?type',
        templateUrl: 'modules/common/import/add.html',
        controller: 'ImportObjectCtrl',
        resolve: {
          importDetail: function($stateParams) {
            return {
              breadcrumbs: [{state: 'shipment-list', title: 'shipments.list'}],
              objectType: ($stateParams.type || 'shipment'),
              csvType: 'MULTIPLE_ROWS_PER_OBJ',
              title: 'shipments.bulk_import',
              onSuccess: {state: 'shipment-list'}
            };
          }
        },
        parent: 'signed-in'
      })
      .state('shipment-import-jobs', {
        url: '/shipment-import-jobs',
        templateUrl: 'modules/common/import/list.html',
        controller: 'ImportJobsListCtrl',
        resolve: {
          importDetail: function() {
            return {
              breadcrumbs: [{state: 'shipment-list', title: 'shipments.list'}],
              title: 'shipments.bulk_import_jobs',
              objectTypes: ['shipment', 'containerShipment']
            }
          }
        },
        parent: 'signed-in'
      })
      .state('shipment-detail', {
        url: '/shipments/:shipmentId',
        templateUrl: 'modules/administrative/shipment/detail.html',
        resolve: {
          shipment: function($stateParams , Shipment) {
            return Shipment.getById($stateParams.shipmentId);
          }
        },
        controller: 'ShipmentDetailCtrl',
        parent: 'shipment-root'
      })
      .state('shipment-detail.overview', {
        url: '/overview',
        templateUrl: 'modules/administrative/shipment/overview.html',
        parent: 'shipment-detail'
      })
      .state('shipment-detail.specimens', {
        url: '/specimens',
        templateUrl: 'modules/administrative/shipment/specimens.html',
        parent: 'shipment-detail',
        controller: 'ShipmentSpecimensCtrl'
      })
      .state('shipment-detail.containers', {
        url: '/containers',
        templateUrl: 'modules/administrative/shipment/containers.html',
        parent: 'shipment-detail',
        controller: 'ShipmentContainersCtrl'
      })
      .state('shipment-receive', {
        url: '/shipments/:shipmentId/receive',
        templateUrl: 'modules/administrative/shipment/addedit.html',
        resolve: {
          shipment: function($stateParams , Shipment) {
            return Shipment.getById($stateParams.shipmentId);
          },
          
          isReceiveAllowed: function(shipment, Util) {
            return Util.booleanPromise(shipment.status == 'Shipped');
          },

          shipmentItems: function(isReceiveAllowed, shipment) {
            if (shipment.isSpecimenShipment()) {
              items = shipment.getSpecimens(0, 10000);
            } else {
              items = shipment.getContainers(0, 10000);
            }

            return items;
          },
        },
        controller: 'ShipmentReceiveCtrl',
        parent: 'shipment-root'
      })
  });
