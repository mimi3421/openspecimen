
angular.module('os.biospecimen.specimenlist.specimensholder', [])
  .factory('SpecimensHolder', function() {
     this.specimens = undefined;
     this.extra = undefined;

     return {
       getSpecimens: function() {
         return this.specimens;
       },

       getExtra: function() {
         return this.extra;
       },

       setSpecimens: function(specimens, extra) {
         this.specimens = specimens;
         this.extra = extra;
       }
     }     
  });
