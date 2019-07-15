
angular.module('os.biospecimen.extensions')
  .directive('osFormData', function(ApiUrls) {
    return {
      restrict: 'E',

      templateUrl: 'modules/biospecimen/extensions/form-data.html',

      scope: {
        data: '='
      },

      link: function(scope, element, attrs) {
        scope.filesUrl = ApiUrls.getBaseUrl() + 'form-files';

        var longerCaptionFields  = 0, totalFields = 0;
        angular.forEach(scope.data.fields,
          function(field) {
            if (field.type == 'subForm') {
              return;
            }

            if (field.caption && field.caption.length >= 30) {
              ++longerCaptionFields;
            }

            ++totalFields;
          }
        );

        scope.verticalLayout = (longerCaptionFields * 100 / totalFields >= 30);
      }
    };
  });
