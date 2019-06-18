
angular.module('os.administrative.user.list', ['os.administrative.models'])
  .controller('UserListCtrl', function(
    $scope, $state, $modal, $translate, currentUser,
    osRightDrawerSvc, User, ItemsHolder, PvManager,
    Util, DeleteUtil, CheckList, Alerts, ListPagerOpts) {

    var pagerOpts, filterOpts;
    var pvInit = false;

    function init() {
      pagerOpts = $scope.pagerOpts = new ListPagerOpts({listSizeGetter: getUsersCount});
      $scope.ctx = {
        exportDetail: {objectType: 'user'}
      };

      initPvsAndFilterOpts();
      loadUsers($scope.userFilterOpts);
      ItemsHolder.setItems('users', undefined);
    }
  
    function initPvsAndFilterOpts() {
      filterOpts = $scope.userFilterOpts = Util.filterOpts({includeStats: true, maxResults: pagerOpts.recordsPerPage + 1});
      $scope.$on('osRightDrawerOpen', function() {
        if (pvInit) {
          return;
        }

        loadActivityStatuses();
        loadUserTypes();
        Util.filter($scope, 'userFilterOpts', loadUsers);
        pvInit = true;
      });
    }
   
    function loadActivityStatuses() {
      PvManager.loadPvs('activity-status').then(
        function(result) {
          var statuses = [].concat(result);
          statuses.push('Locked');
          statuses.push('Expired');
          var idx = statuses.indexOf('Disabled');
          if (idx != -1) {
            statuses.splice(idx, 1);
          }

          idx = statuses.indexOf('Closed');
          if (idx != -1) {
            statuses[idx] = 'Archived';
          } else {
            statuses.push('Archived');
          }

          $scope.activityStatuses = statuses.sort();
        }
      );
    }

    function loadUserTypes() {
      $translate('user.types.NONE').then(
        function() {
          $scope.userTypes = ['SUPER', 'INSTITUTE', 'CONTACT', 'NONE'].map(
            function(type) {
              return {type: type, name: $translate.instant('user.types.' + type)};
            }
          );
        }
      );
    }

    function loadUsers(filterOpts) {
      if (!currentUser.admin) {
        filterOpts = filterOpts || {};
        filterOpts.institute = currentUser.instituteName;
      }

      User.query(filterOpts).then(function(result) {
        if (!$scope.users && result.length > 12) {
          //
          // Show search options when # of users are more than 12
          //
          osRightDrawerSvc.open();
        }

        $scope.users = result;
        pagerOpts.refreshOpts(result);
        $scope.ctx.checkList = new CheckList($scope.users);
      });
    };

    function getUsersCount() {
      return User.getCount($scope.userFilterOpts)
    }

    function updateStatus(prevStatuses, status, msgKey) {
      var users = $scope.ctx.checkList.getSelectedItems()
        .filter(function(u) { return prevStatuses.indexOf(u.activityStatus) != -1; });

      var usersMap = {};
      angular.forEach(users, function(u) { usersMap[u.id] = u; });

      User.bulkUpdate({detail: {activityStatus: status}, ids: Object.keys(usersMap)}).then(
        function(savedUsers) {
          Alerts.success(msgKey, {count: savedUsers.length});

          angular.forEach(savedUsers,
            function(su) {
              usersMap[su.id].activityStatus = su.activityStatus;
            }
          );

          $scope.ctx.checkList = new CheckList($scope.users);
        }
      );
    }

    function getUserIds(users) {
      return users.map(function(user) { return user.id; });
    }
    
    $scope.showUserOverview = function(user) {
      $state.go('user-detail.overview', {userId:user.id});
    };

    $scope.broadcastAnnouncement = function() {
      $modal.open({
        templateUrl: 'modules/administrative/user/announcement.html',
        controller: 'AnnouncementCtrl'
      }).result.then(
        function(announcement) {
          User.broadcastAnnouncement(announcement).then(
            function(resp) {
              Alerts.success('user.announcement.success');
            }
          );
        }
      );
    }

    $scope.deleteUsers = function() {
      var users = $scope.ctx.checkList.getSelectedItems();

      if (!currentUser.admin) {
        var admins = users.filter(function(user) { return !!user.admin; })
          .map(function(user) { return user.getDisplayName(); });

        if (admins.length > 0) {
          Alerts.error('user.admin_access_req', {adminUsers: admins});
          return;
        }
      }

      var opts = {
        confirmDelete: 'user.delete_users',
        successMessage: 'user.users_deleted',
        onBulkDeletion: function() {
          loadUsers($scope.userFilterOpts);
        }
      }

      DeleteUtil.bulkDelete({bulkDelete: User.bulkDelete}, getUserIds(users), opts);
    }

    $scope.editUsers = function() {
       var users = $scope.ctx.checkList.getSelectedItems();
       ItemsHolder.setItems('users', users);
       $state.go('user-bulk-edit');
    }

    $scope.unlockUsers = function() {
      updateStatus(['Locked'], 'Active', 'user.users_unlocked');
    }

    $scope.approveUsers = function() {
      updateStatus(['Pending'], 'Active', 'user.users_approved');
    }

    $scope.lockUsers = function() {
      updateStatus(['Active'], 'Locked', 'user.users_locked');
    }

    $scope.archiveUsers = function() {
      updateStatus(['Locked', 'Active', 'Expired'], 'Closed', 'user.users_archived');
    }

    $scope.reactivateUsers = function() {
      updateStatus(['Closed'], 'Active', 'user.users_reactivated');
    }

    $scope.pageSizeChanged = function() {
      filterOpts.maxResults = pagerOpts.recordsPerPage + 1;
    }

    init();
  });
