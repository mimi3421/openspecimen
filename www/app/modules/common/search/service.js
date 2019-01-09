angular.module('os.common.search.service', [])
  .factory('QuickSearchSvc', function($translate, $http, ApiUrls) {
    var entitySearchMap = {}

    function register(entityName, searchOpts) {
      if (entitySearchMap[entityName]) {
        return;
      }

      entitySearchMap[entityName] = searchOpts;
    }

    function search(term) {
      return $http.get(ApiUrls.getBaseUrl() + 'search', {params: {term: term}}).then(
        function(resp) {
          angular.forEach(resp.data, setMatchCaption);
          return resp.data;
        }
      );
    }

    function setMatchCaption(match) {
      var opts = entitySearchMap[match.entity];
      match.caption = $translate.instant(opts && opts.caption) + ' ' + match.value;
    }

    function getState(entity) {
      return entitySearchMap[entity] && entitySearchMap[entity].state;
    }

    return {
      register: register,

      search: search,

      getState: getState
    };
  });
