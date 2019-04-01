
angular.module('os.administrative.models.user', ['os.common.models'])
  .factory('User', function(osModel, $http, $translate, ApiUtil) {
    var User =
      osModel(
        'users',
        function(user) {
          user.roleModel =  osModel('rbac/subjects/' + user.$id() + '/roles');
          user.roleModel.prototype.$saveProps = saveRoleProps;
        }
      );

    function saveRoleProps() {
      var userRole = angular.copy(this);
      var all = $translate.instant('user.role.all');
      if (userRole.site == all) {
        delete userRole.site;
      } else {
        userRole.site = {name: userRole.site};
      }

      if (userRole.collectionProtocol == all) {
        delete userRole.collectionProtocol;
      } else {
        userRole.collectionProtocol = {shortTitle: userRole.collectionProtocol};
      }

      userRole.role = {name: userRole.role};
      delete userRole.isUpdateAllowed;
      return userRole;
    }

    User.prototype.getType = function() {
      return 'user';
    }

    User.prototype.getDisplayName = function() {
      return this.firstName + ' ' + this.lastName;
    }

    User.prototype.getInstitute = function() {
      return $http.get(User.url() + this.id + '/institute').then(
        function(result) {
          return result.data;
        }
      );
    }

    User.prototype.getRoles = function() {
      return this.roleModel.query();
    }

    User.prototype.newRole = function(role) {
      return new this.roleModel(role);
    }

    User.prototype.updateStatus = function(status) {
      return $http.put(User.url() + this.$id() + '/activity-status', {activityStatus: status})
        .then(User.modelRespTransform);
    }

    User.sendPasswordResetLink = function(user) {
      return $http.post(User.url() + 'forgot-password', user).then(ApiUtil.processResp);
    }

    User.resetPassword = function(passwordDetail) {
      return $http.post(User.url() + "reset-password", passwordDetail).then(ApiUtil.processResp);
    }

    User.changePassword = function(passwordDetail) {
      return $http.put(User.url() + "password", passwordDetail).then(ApiUtil.processResp);
    }

    User.getCurrentUser = function() {
      return $http.get(User.url() + 'current-user').then(User.modelRespTransform);
    }

    User.getUiState = function() {
      return $http.get(User.url() + 'current-user-ui-state').then(
        function(resp) {
          return resp.data;
        }
      );
    }

    User.saveUiState = function(uiState) {
      return $http.put(User.url() + 'current-user-ui-state', uiState).then(
        function(resp) {
          return resp.data;
        }
      );
    }

    User.signup = function(user) {
      return $http.post(User.url() + 'sign-up', user).then(ApiUtil.processResp);
    }

    User.activate = function(id) {
      return $http.put(User.url() + id + '/activity-status', {activityStatus: 'Active'}).then(User.modelRespTransform);
    }

    User.getCurrentUserRoles = function() {
      return $http.get(User.url() + 'current-user-roles').then(
        function(result) {
          return result.data;
        }
      );
    }

    User.broadcastAnnouncement = function(announcement) {
      return $http.post(User.url() + 'announcements', announcement).then(
        function(result) {
          return result.data;
        }
      );
    }

    User.bulkUpdate = function(detail) {
      return $http.put(User.url() + 'bulk-update', detail).then(User.modelArrayRespTransform);
    }

    User.bulkDelete = function(userIds) {
      return User.bulkUpdate({detail: {activityStatus: 'Disabled'}, ids: userIds});
    }

    return User;
  });

