
angular.module('os.biospecimen.participant')
  .controller('EmailFormsCtrl', function($scope, $state, cp, documents, forms, CollectionProtocolRegistration, Alerts) {

    var ctx;

    function init() {
      ctx = $scope.ctx = {
        cp: cp,
        forms: getForms(documents, forms),
        participants: [],
        addedPpids: [],
        noEmailAddresses: true,
        selectedForms: []
      }
    }

    function getForms(documents, forms) {
      var docs = documents.map(
        function(d) {
          return {group: 'Consent', type: 'econsentDocs', caption: d.title + ' ' + d.version, docVersionId: d.id};
        }
      );

      return docs.concat(forms.map(
        function(f) {
          return {group: 'Forms', type: 'deForms', caption: f.caption, formCtxtId: f.formCtxtId}
        }
      ));
    }

    $scope.addParticipants = function(ppids) {
      if (!ppids) {
        return false;
      }

      return CollectionProtocolRegistration.query({cpId: cp.id, ppids: ppids}).then(
        function(cprs) {
          angular.forEach(cprs,
            function(cpr) {
              if (ctx.addedPpids.indexOf(cpr.ppid) != -1) {
                return;
              }

              ctx.addedPpids.push(cpr.ppid);
              ctx.participants.push(cpr);
            }
          );

          ctx.noEmailAddresses = ctx.participants.every(
            function(cpr) {
              return !cpr.participant.emailAddress;
            }
          );

          return true;
        }
      );
    }

    $scope.removeParticipant = function(idx) {
      var p = ctx.participants.splice(idx, 1);
      ctx.addedPpids.splice(ctx.addedPpids.indexOf(p[0].ppid), 1);
    }

    $scope.passThrough = function() {
      return true;
    }

    $scope.emailFormLinks = function() {
      if (ctx.selectedForms.length == 0) {
        Alerts.error('participant.no_forms_selected');
        return;
      }

      var payload = {
        cpShortTitle: cp.shortTitle,
        ppids: ctx.participants.filter(
          function(cpr) {
            return !!cpr.participant.emailAddress;
          }
        ).map(
          function(cpr) {
            return cpr.ppid;
          }
        ),
        forms: ctx.selectedForms,
        notifyByEmail: true
      }

      CollectionProtocolRegistration.emailPdeLinks(payload).then(
        function(resp) {
          Alerts.success('participant.forms_emailed');
          $state.go('participant-list', {cpId: cp.id});
        }
      );
    }

    init();
  });
