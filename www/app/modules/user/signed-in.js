angular.module('openspecimen')
  .controller('SignedInCtrl', function(
     $scope, $rootScope, $state, $timeout, currentUser, userUiState,
     AuthService, Alerts, AuthorizationService, SettingUtil) {

     function init() {
       $scope.alerts = Alerts.messages;
       $rootScope.currentUser = currentUser;
       var ctx = $scope.userCtx = {
         hasPhiAccess: AuthorizationService.hasPhiAccess(),
         state: userUiState,
         showNewStuff: true
       }

       var revision = ui.os.global.appProps.build_commit_revision;
       if (userUiState && userUiState.notesRead == revision) {
         ctx.showNewStuff = false;
       }

       setSetting('training', 'training_url', 'trainingUrl');
       setSetting('training', 'help_link',    'helpUrl');
       setSetting('training', 'forum_link',   'forumUrl');
     }

     function setSetting(module, settingName, propName) {
       SettingUtil.getSetting(module, settingName).then(
         function(setting) {
           $scope.userCtx[propName] = setting.value;
         }
       );
     }

     $scope.userCreateUpdateOpts = {resource: 'User', operations: ['Create', 'Update']};
     $scope.cpReadOpts = {resource: 'CollectionProtocol', operations: ['Read']};
     $scope.containerReadOpts = {resource: 'StorageContainer', operations: ['Read']};
     $scope.orderReadOpts = {resource: 'Order', operations: ['Read']};
     $scope.shipmentReadOpts = {resource: 'ShippingAndTracking', operations: ['Read']};
     $scope.scheduledJobReadOpts = {resource: 'ScheduledJob', operations: ['Read']};
     $scope.dpReadOpts = {resource: 'DistributionProtocol', operations: ['Read']};

     $scope.returnToAccount = function() {
       AuthService.impersonate(null);

       //
       // delayed reloading of state is present to ensure
       // the cookie is removed from the store
       //
       $timeout(function() { $state.go('home', {}, {reload: true}) }, 500);
     }

     init();
  });
