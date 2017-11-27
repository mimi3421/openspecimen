
angular.module('os.biospecimen.participant.overview', ['os.biospecimen.models'])
  .controller('ParticipantOverviewCtrl', function(
    $scope, $state, $stateParams, hasFieldsFn, storePhi, cp, cpr, consents, visits,
    Visit, CollectSpecimensSvc, ExtensionsUtil, Util, Alerts) {

    function init() {
      $scope.occurredVisits    = Visit.completedVisits(visits);
      $scope.anticipatedVisits = Visit.anticipatedVisits(visits);
      $scope.missedVisits      = Visit.missedVisits(visits);

      ExtensionsUtil.createExtensionFieldMap($scope.cpr.participant);
      $scope.partCtx = {
        obj: {cpr: $scope.cpr, consents: createCodedResps(consents)},
        inObjs: ['cpr', 'consents'],
        showEdit: hasFieldsFn(['cpr'], []),
        auditObjs: [
          {objectId: cpr.id, objectName: 'collection_protocol_registration'},
          {objectId: cpr.participant.id, objectName: 'participant'}
        ],
        showAnonymize: storePhi
      }
    }

    function createCodedResps(consents) {
      angular.forEach(consents && consents.responses,
        function(resp) {
          if (resp.code) {
            consents[resp.code] = resp.response;
          }
        }
      );

      return consents;
    }

    $scope.isOtherProtocol = function(other) {
      return other.cpShortTitle != $scope.cpr.cpShortTitle;
    }

    $scope.anonymize = function() {
      Util.showConfirm({
        title: "participant.anonymize",
        confirmMsg: "participant.confirm_anonymize",
        isWarning: true,
        ok: function() {
          $scope.cpr.anonymize().then(
            function(savedCpr) {
              angular.extend($scope.cpr, savedCpr);
              ExtensionsUtil.createExtensionFieldMap($scope.cpr.participant);
              Alerts.success("participant.anonymized_successfully");
            }
          )
        }
      });
    }

    $scope.collect = function(visit) {
      var retSt = {state: $state.current, params: $stateParams};
      CollectSpecimensSvc.collectVisit(retSt, cp, cpr.id, visit);
    }

    $scope.collectPending = function(visit) {
      var retSt = {state: $state.current, params: $stateParams};
      CollectSpecimensSvc.collectPending(retSt, cp, cpr.id, visit);
    }

    init();
  });
