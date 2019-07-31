angular.module('os.administrative.form.formctxts', ['os.administrative.models'])
  .controller('FormCtxtsCtrl', function($scope, $modalInstance, $translate, args, cpList, entities, Alerts) {

    var reload = false;

    function init() {
      $scope.showFormCtxts = true;
      $scope.extnEntities = entities.filter(function(e) { return e.allowEdits !== false; });
      $scope.form = args.form;
      $scope.cpList = cpList;

      var cpLevels = [
        'Participant', 'ParticipantExtension', 'SpecimenCollectionGroup', 'VisitExtension',
        'Specimen', 'SpecimenExtension', 'SpecimenEvent'
      ]

      var formCtxts = $scope.cpFormCtxts = args.formCtxts;
      angular.forEach(formCtxts,
        function(fc) {
          var cpLevel = cpLevels.indexOf(fc.level) != -1;
          if (cpLevel && (!fc.collectionProtocol.id || fc.collectionProtocol.id == -1)) {
            fc.collectionProtocol.shortTitle = $translate.instant('form.all');
          } else if (!cpLevel) {
            fc.collectionProtocol.shortTitle = $translate.instant('form.na');
          }

          for (var i = 0; i < entities.length; i++) {
            var entity = entities[i];
            if (entity.name == fc.level) {
              fc.level = entity;
              break;
            }
          }
        }
      );

      $scope.cpFormCtxt = {
        allProtocols: false,
        isMultiRecord: false,
        selectedCps: [],
        selectedEntity: undefined
      }
    }

    $scope.enableAttach = function(formCtxt) {
      if (!formCtxt.selectedEntity) {
        return false;
      }

      if (formCtxt.selectedEntity.allCps) {
        return true;
      }

      return formCtxt.allProtocols || (formCtxt.selectedCps && formCtxt.selectedCps.length > 0);
    }

    $scope.attach = function(formCtxt) {
      var cpIds = [];
      if (formCtxt.allProtocols || formCtxt.selectedEntity.allCps) {
        cpIds = [-1];
      } else {
        for (var i = 0; i < formCtxt.selectedCps.length; ++i) {
          cpIds.push(formCtxt.selectedCps[i].id);
        }
      }

      var multipleRecs = formCtxt.selectedEntity.multipleRecs;
      if (multipleRecs != false) {
        multipleRecs = formCtxt.isMultiRecord;
      }

      var formContext = {
        form: $scope.form,
        cpIds: cpIds,
        entity: formCtxt.selectedEntity.name,
        isMultiRecord: multipleRecs
      }

      formContext = $scope.form.newFormContext(formContext); 
      formContext.$saveOrUpdate().then(
        function(data) {
          Alerts.success("form.attached");
          $modalInstance.close(true);
        }
      );
    }

    $scope.confirmRemoveCtx = function(formCtx, $index) {
      $scope.showFormCtxts = false;
      $scope.removeCtxData = {ctx: formCtx, idx: $index};
    };

    $scope.removeCtx = function() {
      var cpId = $scope.removeCtxData.ctx.collectionProtocol.id || -1;
      var entity = $scope.removeCtxData.ctx.level;
      var formContext = $scope.form.newFormContext({form: $scope.form, cpId: cpId, entityType: entity.name});

      formContext.$remove().then(
        function() {
          $scope.cpFormCtxts.splice($scope.removeCtxData.idx, 1);
          $scope.showFormCtxts = true;
          Alerts.success("form.association_deleted", $scope.removeCtxData.ctx);
          $scope.removeCtxData = {};
          reload = true;
        }
      );
    };

    $scope.cancelRemoveCtx = function() {
      $scope.showFormCtxts = true;
      $scope.removeCtxData = {};
    };


    $scope.cancel = function() {
      $modalInstance.close(reload);
    }

    init();
  });
