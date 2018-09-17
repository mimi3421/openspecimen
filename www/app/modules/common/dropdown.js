angular.module('openspecimen')
  .directive('osSelect', function($parse) {
    function linker(scope, element, attrs) {
      if (!attrs.onChange && (attrs.required !== undefined)) {
        return;
      }

      scope.$watch(attrs.ngModel,
        function(newVal, oldVal) {
          if (newVal != oldVal) {
            if (newVal == undefined) {
              $parse(attrs.ngModel).assign(scope, null);
            }

            if (attrs.onChange) {
              scope.$eval(attrs.onChange)(newVal);
            }
          }
        }
      );
    }

    function getItemDisplayValue(item, tAttrs) {
      var result = item;
      if (!!tAttrs.displayProp) {
        result += '.' + tAttrs.displayProp;
      }
      return result;
    }  

    return {
      restrict: 'E',
      compile: function(tElem, tAttrs) {
        var multiple = angular.isDefined(tAttrs.multiple);
        var uiSelect = angular.element(multiple ? '<ui-select os-fix-dd multiple/>' : '<ui-select os-fix-dd />')
          .attr('ng-model', tAttrs.ngModel)
          .attr('ng-disabled', tAttrs.ngDisabled)
          .attr('ng-required', tAttrs.ngRequired)
          .attr('reset-search-input', true)
          .attr('append-to-body', tAttrs.appendToBody == true || tAttrs.appendToBody == 'true')
          .attr('os-tabable', !!tAttrs.osTabable);
    
        if (tAttrs.ngInit) {
          uiSelect.attr('ng-init', tAttrs.ngInit);
        }

        if (tAttrs.onSelect) {
          uiSelect.attr('on-select', tAttrs.onSelect);
        }

        if (tAttrs.onRemove) {
          uiSelect.attr('on-remove', tAttrs.onRemove);
        }

        if (tAttrs.title) {
           uiSelect.attr('title', tAttrs.title);
        }

        var uiSelectMatch = angular.element('<ui-select-match/>')
          .attr('placeholder', tAttrs.placeholder)
          .attr('allow-clear', tAttrs.required == undefined);
        
        var searchItem = getItemDisplayValue('item', tAttrs);
        var uiSelectChoices = angular.element('<ui-select-choices/>');

        if (tAttrs.selectProp) {
          uiSelectChoices.attr('repeat', "item." + tAttrs.selectProp + " as item in " + tAttrs.list + " | filter: $select.search");
        } else {
          uiSelectChoices.attr('repeat', "item in " + tAttrs.list + " | filter: $select.search");
        }

        if (tAttrs.groupBy) {
          uiSelectChoices.attr('group-by', tAttrs.groupBy);
        }

        uiSelectChoices.append('<span ng-bind-html="' + searchItem + ' | highlight: $select.search"></span>');

        if (multiple) {
          uiSelectMatch.append('{{' + getItemDisplayValue('$item', tAttrs) + '}}');
        } else {
          uiSelectMatch.append('{{' + getItemDisplayValue('$select.selected', tAttrs) + '}}');
        }

        if (angular.isDefined(tAttrs.refresh)) {
          uiSelectChoices.attr({
            'refresh': tAttrs.refresh + '($select.search, ' + tAttrs.refreshArgs + ')',
            'refresh-delay': tAttrs.refreshDelay || 750
          });
        }
            
        uiSelect.append(uiSelectMatch).append(uiSelectChoices);

        var selectContainer = angular.element("<div/>")
          .addClass("os-select-container")
          .append(uiSelect);

        tElem.replaceWith(selectContainer);
        return linker;
      }
    };
  })
  .directive('osFixDd', function($timeout) {
    return {
      restrict: 'A',

      link: function(scope, element, attrs) {
        var dd = null;

        var ddCont = new MutationObserver(
          function(ddContMutations) {
            if (!ddContMutations.some(function(r) { return r.attributeName == 'class' })) {
              return;
            }

            //
            // we are interested when 'direction-up' class is added to dropdown container
            //
            if (element.attr('class').indexOf('direction-up') != -1 && !dd) {
              var ddEl = element.find('.ui-select-dropdown');
              dd = new MutationObserver(
                function(ddMutations) {
                  if (!ddMutations.some(function(r) { return r.addedNodes.length != 0 || r.removedNodes.length != 0; })) {
                    return;
                  }

                  //
                  // the dropdown should be offset to the top by its height
                  // so that the bottom appears stuck to the search input
                  //
                  ddEl[0].style.top = -1 * ddEl.outerHeight() + 'px';
                }
              );

              dd.observe(ddEl[0], {childList: true, subtree: true});
            } else if (element.attr('class').indexOf('direction-up') == -1 && dd) {
              dd.disconnect();
              dd = null;
            }
          }
        );

        ddCont.observe(element[0], {attributes: true});
        element.on('remove', function() { ddCont.disconnect(); });
      }
    }
  })

  .directive('dropdownToggle', function() {
    /* Copied from angular-ui-bootstrap to make the dropdown-toggle work for class as well */

    return {
      require: '?^dropdown',
      restrict: 'AC',
      link: function(scope, element, attrs, dropdownCtrl) {
        if (!dropdownCtrl) {
          return;
        }

        element.addClass('dropdown-toggle');

        dropdownCtrl.toggleElement = element;

        var toggleDropdown = function(event) {
          event.preventDefault();

          if (!element.hasClass('disabled') && !attrs.disabled) {
            scope.$apply(function() {
              dropdownCtrl.toggle();
            });
          }
        };

        element.bind('click', toggleDropdown);

        // WAI-ARIA
        element.attr({ 'aria-haspopup': true, 'aria-expanded': false });
        scope.$watch(dropdownCtrl.isOpen, function( isOpen ) {
          element.attr('aria-expanded', !!isOpen);
        });

        scope.$on('$destroy', function() {
          element.unbind('click', toggleDropdown);
        });
      }
    };
  });
