
angular.module('os.administrative.user.dropdown', ['os.administrative.models'])
  .directive('osUsers', function(AuthorizationService, User) {
    function loadUsers(scope, searchTerm, ctrl) {
      var opts = angular.extend({searchString : searchTerm}, scope.filterOpts || {});
      User.query(opts).then(
        function(result) {
          scope.users = result;
          loadSelectedUser(scope, result, ctrl);
        }
      );
    };

    function loadSelectedUser(scope, usersList, ctrl) {
      if (ctrl.onceLoaded) {
        return;
      }

      ctrl.onceLoaded = true;
      var selectProp = ctrl.attrs.selectProp;
      if (selectProp != 'id' || !scope.ngModel) {
        return;
      }

      var selectedUser = usersList.find(function(user) { return user[selectProp] == scope.ngModel; });
      if (selectedUser) {
        return;
      }

      User.getById(scope.ngModel).then(
        function(user) {
          usersList.push(user);
        }
      );
    }

    return {
      restrict: 'AE',

      scope: {
        ngModel: '=ngModel',
        placeholder: '@',
        onSelect: '=onSelect',
        filterOpts: '=',
        defaultList: '='
      },

      replace: true,

      controller: function($scope) {
        var ctrl = this;

        $scope.searchUsers = function(searchTerm) {
          if (!searchTerm && $scope.defaultList) {
            $scope.users = $scope.defaultList;
            loadSelectedUser($scope, $scope.users, ctrl);
            return;
          }

          loadUsers($scope, searchTerm, ctrl);
        };

        $scope.$watch('filterOpts', function(newVal, oldVal) {
          if (newVal == oldVal) {
            return;
          }

          loadUsers($scope, undefined, ctrl);
        });
      },
  
      link: function(scope, element, attrs, ctrl) {
        ctrl.attrs = attrs;

        if (!scope.ngModel && attrs.hasOwnProperty('defaultCurrentUser')) {
          var user = angular.copy(AuthorizationService.currentUser() || {});
          if (attrs.selectProp) {
            user = user[attrs.selectProp];
          }

          if (attrs.hasOwnProperty('multiple')) {
            scope.ngModel = [user];
          } else {
            scope.ngModel = user;
          }
        } else if (!scope.ngModel && attrs.hasOwnProperty('multiple')) {
          scope.ngModel = [];
        }
      },

      template: function(tElem, tAttrs) {
        var bodyAppend = angular.isDefined(tAttrs.appendToBody) ? tAttrs.appendToBody : "true";
        var tabable = angular.isDefined(tAttrs.osTabable) ? tAttrs.osTabable : "false";

        var loopExpr = 'user in users';
        if (angular.isDefined(tAttrs.selectProp)) {
          loopExpr = 'user.' + tAttrs.selectProp + ' as ' + loopExpr;
        }

        return angular.isDefined(tAttrs.multiple) ?
              '<div>' +
                '<ui-select multiple ng-model="$parent.ngModel" reset-search-input="true"' +
                  ' append-to-body="' + bodyAppend + '" os-tabable="' + tabable + '">' +
                  '<ui-select-match placeholder="{{$parent.placeholder}}">' +
                    '{{$item.lastName}}, {{$item.firstName}}' +
                  '</ui-select-match>' +
                  '<ui-select-choices repeat="' + loopExpr +'" refresh="searchUsers($select.search)" refresh-delay="750">' +
                    '<span ng-bind-html="user.lastName + \', \' + user.firstName | highlight: $select.search"></span>' +
                  '</ui-select-choices>' +
                '</ui-select>' +
              '</div>'

              :

              '<div>' +
                '<ui-select ng-model="$parent.ngModel" reset-search-input="true"' + 
                  ' append-to-body="' + bodyAppend + '" os-tabable="' + tabable + '">' +
                  '<ui-select-match placeholder="{{$parent.placeholder}}" allow-clear="'+ (tAttrs.required == undefined) +'">' +
                    '{{$select.selected.lastName}}, {{$select.selected.firstName}}' +
                  '</ui-select-match>' +
                  '<ui-select-choices repeat="' + loopExpr +'" refresh="searchUsers($select.search)" refresh-delay="750">' +
                    '<span ng-bind-html="user.lastName + \', \' + user.firstName | highlight: $select.search"></span>' +
                  '</ui-select-choices>' + 
                '</ui-select>' +
              '</div>';
      }
    };
  });
