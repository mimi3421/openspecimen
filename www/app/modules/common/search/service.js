angular.module('os.common.search.service', [])
  .factory('QuickSearchSvc', function($translate, $q) {
    var entitySearchMap = {}

    function register(entityName, searchOpts) {
      if (entitySearchMap[entityName]) {
        return;
      }

      entitySearchMap[entityName] = searchOpts;

    }

    function getTemplate(entity) {
      var opts = entitySearchMap[entity];
      return !!opts ? opts.template : null;
    }

    function search(entity, searchData) {
      var opts = entitySearchMap[entity];
      opts.search(searchData);
    }

    function getEntities() {
      var result = {entities: [], qs: []};

      angular.forEach(entitySearchMap, function(value, key) {
        var entity = {name: key, caption: value.caption, order: value.order};

        var q = $q.defer();
        $translate(value.caption).then(
          function(caption) {
            entity.caption = caption;
            q.resolve(caption);
          }
        );

        result.entities.push(entity);
        result.qs.push(q.promise);
      });

      result.entities = result.entities.sort(function(a, b) {return (a.order > b.order) - (b.order > a.order);});
      return result;
    }

    return {
      register: register,

      getEntities: getEntities,

      getTemplate: getTemplate,

      search: search
    };
  })
