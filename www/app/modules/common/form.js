function osRequired($timeout) {
  return {
    require: '^form',
    restrict: 'A',
    link: function(scope, element, attrs, ctrl) {
      if (!ctrl._osFormEl) {
        var formEl = ctrl._osFormEl = element.closest('form');
        if (formEl.length == 0) {
          formEl = ctrl._osFormEl = element.closest('ng-form');
        }

        ctrl._noLabelForm = formEl[0].hasAttribute('os-no-label-form');
        if (!ctrl._noLabelForm) {
          ctrl._noLabelForm = formEl.hasClass('os-no-label-form');
        }
      }

      var setDirty = function() { ctrl[attrs.name] && ctrl[attrs.name].$setDirty(true); }
      var noHint = (attrs.noHint == true) || (attrs.noHint == 'true');

      if (element.hasClass('os-select-container')) {
        $timeout(
          function() {
            element.find('.ui-select-focusser, .ui-select-search').bind('blur', setDirty);

            if (!ctrl._noLabelForm && !noHint) {
              element.find('.ui-select-placeholder').addClass('os-italic').text('mandatory');
              element.find('.ui-select-match, .ui-select-search').addClass('os-italic').attr('placeholder', 'mandatory');
            }
          }
        );
      } else {
        element.bind('blur', setDirty);
        if (!ctrl._noLabelForm && !noHint) {
          $timeout(function() { element.addClass('os-italic').attr('placeholder', 'mandatory'); });
        }
      }
    }
  };
}

angular.module('os.common.form', [])
  .directive('osFormValidator', function(LocationChangeListener) {
    return {
      restrict: 'A',

      controller: function($scope) {
        this._cachedValues = {};

        this._formSubmitted = false;

        this._parentValidator = undefined;

        this._form = undefined;

        this.formSubmitted = function(val) {
          this._formSubmitted = val;
        };

        this.isFormSubmitted = function() {
          return this._formSubmitted;
        };

        this.setForm = function(form) {
          this._form = form;
        };

        this.getForm = function() {
          return this._form;
        };

        this.isValidForm = function() {
          return !this._form.$invalid;
        };

        this.isInteracted = function(field) {
          if (this._parentValidator && this._parentValidator.isFormSubmitted()) {
            return true;
          }
            
          return this.isFormSubmitted() || (field && field.$dirty);
        };

        this.setParentValidator = function(parentValidator) {
          this._parentValidator = parentValidator;
        };

        this.getCachedValues = function(group, prop, getter) {
          var key = group + "_" + prop;
          if (!this._cachedValues[key]) {
            this._cachedValues[key] = getter();
          }

          return this._cachedValues[key];
        }
      },

      link: function(scope, element, attrs, controller) {
        scope.$watch(attrs.osFormValidator, function(form) {
          controller.setForm(form);
        });

        scope.$watch(attrs.name + ".$dirty", function(dirty) {
          if (dirty) {
            LocationChangeListener.preventChange();
          } else {
            LocationChangeListener.allowChange();
          }
        });

        if (attrs.validator) {
          scope[attrs.validator] = controller;
        }

        if (attrs.parentValidator) {
          scope.$watch(attrs.parentValidator, function(parentValidator) {
            controller.setParentValidator(parentValidator);
          });
        }

        element.find("input, select, .os-select-container").keypress(
          function(e){
            if ( e.which == 13 ) { // Enter key = keycode 13
              return false;
            }
        });
      }
    };
  })

  .directive('osFormSubmit', function($document, Alerts, LocationChangeListener) {
    function onSubmit(scope, ctrl, attrs) {
      var form = ctrl.getForm();
      if (form.$pending) {
        var pendingWatch = scope.$watch(
          function() {
            return form.$pending;
          },

          function(pending) {
            if (!pending) {
              pendingWatch();
              onSubmit(scope, ctrl, attrs);
            }
          }
        );
      } else {
        ctrl.formSubmitted(true);
        if (ctrl.isValidForm()) {
          if (attrs.localForm) {
            LocationChangeListener.allowChange();
          }
          scope.$eval(attrs.osFormSubmit);
        } else {
          Alerts.error("common.form_validation_error");
        }
      }
    }

    var cnt = 0;

    return {
      restrict: 'A',
  
      require: '^osFormValidator',

      priority: 1,

      link: function(scope, element, attrs, ctrl) {
        if (attrs.mousedownClick == 'true') {
          element.bind('mousedown', function() {
            ++cnt;
            var eventName = "mouseup.formsubmit." + cnt;
            $document.on(eventName, function() {
              $document.unbind(eventName);
              onSubmit(scope, ctrl, attrs);
            });
          })
        } else {
          element.bind('click', function() {
            onSubmit(scope, ctrl, attrs);
          });
        }
      }
    };
  })

  .directive('osFormCancel', function($timeout, LocationChangeListener) {
    function onCancel(scope, cancelHandler) {
      LocationChangeListener.allowChange();
      scope.$eval(cancelHandler);
    }

    return {
      restrict: 'A',

      link: function(scope, element, attrs, ctrl) {
        element.bind('click', function() {
          $timeout(function() {
            onCancel(scope, attrs.osFormCancel);
          });
        });
      }
    };
  })

  .directive('osFieldError', function() {
    return {
      restrict: 'A',

      require: '^osFormValidator',

      scope: {
        field: '='
      },

      link: function(scope, element, attrs, ctrl) {
        scope.isInteracted = function() {
          return ctrl.isInteracted(scope.field);
        };
      },

      replace: 'true',

      template: 
        '<div>' +
        '  <div ng-if="isInteracted()" class="alert alert-danger os-form-err-msg" ' +
        '    ng-messages="field.$error" ng-messages-include="modules/common/error-messages.html"> ' +
        '  </div>' +
        '</div>'
    };
  })

  .directive('osFieldWarn', function() {
    return {
      restrict: 'A',

      scope: {
        message: "=osFieldWarn"
      },

      replace: 'true',

      template:
        '<div>' +
        '  <div class="alert alert-warning os-form-err-msg">' +
        '    <span translate="{{message}}"></span>' +
        '  </div>' +
        '</div>'
    };
  })

  .directive('required', osRequired)

  .directive('ngRequired', osRequired);
