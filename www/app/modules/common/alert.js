
angular.module('openspecimen')
  .factory('Alerts', function($rootScope, $interval, $translate) {
    return {
      messages: [],

      add: function(text, type, timeout) {
        var self = this;

        var msg = {
          text: text,
          type: type, 
          timeout: timeout,
          close: function() {
            self.remove(this);
            $interval.cancel(msg.promise);
          }           
        };

        this.messages.push(msg);

        if (timeout === false) {
          return msg;
        }

        if (timeout === undefined || timeout === null) {
          var dispTime = undefined;
          if (type == 'danger') {
            dispTime = $rootScope.global.appProps.toast_disp_time;
          }

          timeout = (dispTime || 5) * 1000;
          timeout < 1000 && (timeout = 5000);
        }

        var promise = $interval(function() { msg.close() }, timeout, 1);
        msg.promise = promise;
        return msg;
      },

      success: function(code, params, timeout) {
        return this.add($translate.instant(code, params), 'success', timeout);
      },

      info: function(code, params, timeout) {
        return this.add($translate.instant(code, params), 'info', timeout);
      },

      warn: function(code, params, timeout) {
        return this.add($translate.instant(code, params), 'warning', timeout);
      },

      error: function(code, params, timeout) {
        return this.add($translate.instant(code, params), 'danger', timeout);
      },

      errorText: function(text, timeout) {
        var msg = text;
        if (text instanceof Array) {
          msg = text.join(", ");
        }
        return this.add(msg, 'danger', timeout);
      },

      clear: function() {
        this.messages.length = 0;
      },

      remove: function(msg) {
        var idx = this.messages.indexOf(msg);
        if (idx == -1) {
          return;
        }
   
        this.messages.splice(idx, 1);
      }
    };
  });
