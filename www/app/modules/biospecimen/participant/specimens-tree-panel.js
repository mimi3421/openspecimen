
angular.module('os.biospecimen.participant')
  .factory('ParticipantSpecimensViewState', function($q, CpConfigSvc, Specimen) {
    var State = function(cpr) {
      this.cpr = cpr;
      this.selectedSpmn = undefined;
      this.specimens = undefined;
    }

    function openSpecimenTree(specimens) {
      angular.forEach(specimens,
        function(specimen) {
          specimen.isOpened = true;
          openSpecimenTree(specimen.children);
        }
      );
    }

    State.prototype.getSpecimens = function() {
      if (this.specimens) {
        var q = $q.defer();
        q.resolve(this.specimens);
        return q.promise;
      }

      var that = this;
      return Specimen.listFor(this.cpr.id).then(
        function(specimens) {
          that.specimens = Specimen.flatten(specimens);
          openSpecimenTree(that.specimens);
          return that.specimens;
        }
      )
    }

    State.prototype.specimensUpdated = function() {
      this.specimens = undefined;
    }

    State.prototype.getDescTmpl = function() {
      var that = this;
      return CpConfigSvc.getWorkflowData(this.cpr.cpId, 'specimenTree', {}).then(
        function(data) {
          var tmpl = data.summaryDescTmpl || '';
          if (!!tmpl) {
            return tmpl;
          }

          return CpConfigSvc.getCommonCfg(that.cpr.cpId, 'specimenHeader').then(
            function(spmnHeader) {
              return (spmnHeader && spmnHeader.leftTitle) || '';
            }
          );
        }
      );
    }

    State.prototype.selectSpecimen = function(specimen) {
      this.selectedSpmn = specimen;
    }

    State.prototype.unselectSpecimen = function() {
      this.selectedSpmn = undefined;
    }

    State.specimensUpdated = function(scope, data) {
      scope.$emit('participantSpecimensUpdated', data);
    }

    return State;
  })
  .directive('osSpecimensTreePanel', function() {
    return {
      restrict: 'E',

      scope: {
        viewState: '='
      },

      templateUrl: 'modules/biospecimen/participant/specimens-tree-panel.html',

      link: function(scope, element, attrs) {
        scope.ctx = {
          descTmpl: '',
          specimens: [],
          viewState: scope.viewState
        },

        scope.viewState.getSpecimens().then(
          function(spmns) {
            scope.ctx.specimens = spmns;
          }
        );

        scope.viewState.getDescTmpl().then(
          function(tmpl) {
            scope.ctx.descTmpl = tmpl;
          }
        );

        scope.openSpecimenNode = function(specimen) {
          specimen.isOpened = true;
        }

        scope.closeSpecimenNode = function(specimen) {
          specimen.isOpened = false;
        }
      }
    }
  });
