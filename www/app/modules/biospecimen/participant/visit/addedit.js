
angular.module('os.biospecimen.visit.addedit', [])
  .controller('AddEditVisitCtrl', function(
    $scope, $state, $stateParams, cp, cpr, visit, latestVisit,
    extensionCtxt, hasDict, layout, onValueChangeCb, mrnAccessRestriction,
    ParticipantSpecimensViewState, PvManager, ExtensionsUtil) {

    function loadPvs() {
      $scope.visitStatuses = PvManager.getPvs('visit-status');
      $scope.cohorts = PvManager.getPvs('cohort');
    };

    function init() {
      var currVisit = $scope.currVisit = angular.copy(visit);
      angular.extend(currVisit, {cprId: cpr.id, cpTitle: cpr.cpTitle});

      $scope.visitCtx = {
        obj: {visit: $scope.currVisit, cpr: cpr, cp: cp},
        opts: {viewCtx: $scope, layout: layout, onValueChange: onValueChangeCb, mdInput: false},
        inObjs: ['visit'],
        mrnAccessRestriction: mrnAccessRestriction
      }

      if (!currVisit.id) {
        angular.extend(currVisit, {
          visitDate: new Date(),
          status: 'Complete',
          clinicalDiagnoses: latestVisit ? latestVisit.clinicalDiagnoses : currVisit.clinicalDiagnoses,
          site: getVisitSite(cpr, latestVisit, currVisit)
        });
        delete currVisit.anticipatedVisitDate;
      } else if (currVisit.status == 'Pending') {
        currVisit.visitDate = new Date();
      }

      if ($stateParams.missedVisit == 'true') {
        angular.extend(currVisit, {status: 'Missed Collection'});
      } else if ($stateParams.newVisit == 'true') {
        angular.extend(currVisit, {id: undefined, name: undefined, status: 'Complete', visitDate: new Date()});
        $scope.visit = currVisit;
      }

      $scope.deFormCtrl = {};
      $scope.extnOpts = ExtensionsUtil.getExtnOpts(currVisit, extensionCtxt);

      if (!hasDict) {
        loadPvs();
      }
    }

    function getVisitSite(cpr, latestVisit, currVisit) {
      var site = currVisit.site;
      if (!!site) {
        // site = site;
      } else if (latestVisit) {
        site = latestVisit.site;
      } else if (cpr.participant.pmis.length > 0) {
        site = cpr.participant.pmis[0].siteName;
      }

      return site;
    }

    $scope.saveVisit = function() {
      var formCtrl = $scope.deFormCtrl.ctrl;
      if (formCtrl && !formCtrl.validate()) {
        return;
      }

      if (formCtrl) {
        $scope.currVisit.extensionDetail = formCtrl.getFormData();
      }

      if ($scope.currVisit.eventLabel != visit.eventLabel) {
        $scope.currVisit.eventId = undefined;
      }

      $scope.currVisit.$saveOrUpdate().then(
        function(result) {
          ParticipantSpecimensViewState.specimensUpdated($scope);

          angular.extend($scope.visit, angular.extend({clinicalStatus: null, cohort: null}, result));
          $state.go('visit-detail.overview', {visitId: result.id, eventId: result.eventId});
        }
      );
    };

    init();
  });
