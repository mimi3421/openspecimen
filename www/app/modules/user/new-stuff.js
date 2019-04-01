
angular.module('openspecimen')
  .controller('NewStuffCtrl', function($scope, $timeout, $document, $http, $sce, ApiUrls, User, SettingUtil) {
    var ctx, loginCtx;

    function init() {
      loginCtx = $scope.userCtx; // from the signed-in controller
      ctx = $scope.ctx = { loading: true, notes: null };
      SettingUtil.getSetting('training', 'release_notes').then(
        function(notesLink) {
          ctx.notesLink = notesLink.value;
        }
      );

      loadReleaseNotes();
    }

    function loadReleaseNotes() {
      $http.get(ApiUrls.getBaseUrl() + '/release-notes/latest-summary').then(
        function(resp) {
          ctx.notes = $sce.trustAsHtml(resp.data.notes);
          ctx.loading = false;
        }
      );
    }

    function hidePopover() {
      var popups = $document.find('div.popover');
      for (var i = 0; i < popups.length; ++i) {
        var popupEl = angular.element(popups[i]);
        if (popupEl.find('.os-new-stuff').length > 0) {
          popupEl.scope().$hide();
          break;
        }
      }
    }

    $scope.close = function() {
      loginCtx.state.notesRead = ui.os.global.appProps.build_commit_revision;
      User.saveUiState(loginCtx.state).then(
        function(savedState) {
          angular.extend(loginCtx, {state: savedState, showNewStuff: false});
          hidePopover();
        }
      );
    }

    init();
  });
