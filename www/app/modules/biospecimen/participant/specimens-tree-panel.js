
angular.module('os.biospecimen.participant')
  .factory('ParticipantSpecimensViewState', function(
    $q, $document, $location, $timeout, $anchorScroll,
    CpConfigSvc, Specimen) {

    var State = function(cpr) {
      this.cpr = cpr;
      this.selectedSpmn = undefined;
      this.specimens = undefined;
    }

    function openSpecimenTree(specimens, openedNodesMap) {
      angular.forEach(specimens,
        function(specimen) {
          if (openedNodesMap) {
            var key = (specimen.id || 'u') + '-' + (specimen.reqId || 'u');
            specimen.isOpened = openedNodesMap[key];
          } else if (specimen.lineage == 'New') {
            specimen.isOpened = true;
          }
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
          openSpecimenTree(that.specimens, that.openedNodesMap);
          return that.specimens;
        }
      )
    }

    State.prototype.specimensUpdated = function() {
      var nodesMap = this.openedNodesMap = {};
      angular.forEach(this.specimens,
        function(specimen) {
          var key = (specimen.id || 'u') + '-' + (specimen.reqId || 'u');
          nodesMap[key] = specimen.isOpened;
        }
      );

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

    State.prototype.recordScrollPosition = function(element) {
      this.scrollTop = element.scrollTop();
    }

    State.prototype.scrollTo = function(selector, specimen) {
      var that = this;
      $timeout(function() {
        if (specimen && !that.useScrollTop) {
          $location.hash('specimen-' + (specimen.id || 'na') + '-' + (specimen.reqId || 'na'));
          $anchorScroll();
        } else if (selector && that.scrollTop != undefined) {
          $document.find(selector).scrollTop(that.scrollTop);
        }

        that.useScrollTop = false;
      }, 0, false);
    }

    State.specimensUpdated = function(scope, data) {
      scope.$emit('participantSpecimensUpdated', data);
    }

    return State;
  })
  .directive('osSpecimensTreePanel', function($document, $q) {
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
        };

        var containerSelector = '';
        if (attrs.scrollContainer) {
          containerSelector = '.' + attrs.scrollContainer;
        }

        var spmnsQ = scope.viewState.getSpecimens();
        var descQ  = scope.viewState.getDescTmpl();
        $q.all([spmnsQ, descQ]).then(
          function(resps) {
            scope.ctx.specimens = resps[0];
            scope.ctx.descTmpl  = resps[1];
            scope.viewState.scrollTo(containerSelector, scope.viewState.selectedSpmn);
          }
        );

        scope.openSpecimenNode = function(specimen) {
          specimen.isOpened = true;
        }

        scope.closeSpecimenNode = function(specimen) {
          specimen.isOpened = false;
        }

        scope.selectSpecimen = function(specimen) {
          scope.viewState.useScrollTop = true;
        }

        if (containerSelector) {
          $document.find(containerSelector)
            .on('scroll', function(event) {
              scope.viewState.recordScrollPosition(angular.element(event.target));
            }
          );
        }
      }
    }
  });
