angular.module('openspecimen')
  .directive('osLeftDrawer', function($compile, osLeftDrawerSvc) {
    return {
      restrict: 'A',

      link: function(scope, element, attrs) {
        /*element.find('ul').addClass('os-menu-items');
        element.find('ul').on('click', function() {
          osNavDrawerSvc.toggle();
        });*/

        element.addClass('os-nav-drawer');

        var overlay = angular.element('<div/>').addClass('os-nav-drawer-overlay');
        element.after(overlay);
        overlay.on('click', function() {
          osLeftDrawerSvc.toggle();
        });

        element.removeAttr('os-nav-drawer');
        osLeftDrawerSvc.setDrawer(element);
        // $compile(element)(scope);

        scope.$on('$destroy', function() { osLeftDrawerSvc.reset(); });
      }
    };
  })
  .directive('osLeftDrawerToggle', function(osLeftDrawerSvc) {
    return {
      restrict: 'AC',
      link: function(scope, element, attrs) {
        element.on('click', function() {
          osLeftDrawerSvc.toggle();
        });
      }
    };
  })
  .factory('osLeftDrawerSvc', function() {
    var drawerEl = undefined;
    return {
      setDrawer: function(drawer) {
        drawerEl = drawer;
      },

      toggle: function() {
        drawerEl.toggleClass('active');
      },

      reset: function() {
        drawerEl = undefined;
      }
    }
  });
