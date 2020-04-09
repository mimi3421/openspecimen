
angular.module('os.biospecimen.participant')
  .factory('ParticipantsHolder', function() {
     this.participants = undefined;

     return {
       getParticipants: function() {
         return this.participants;
       },

       setParticipants: function(participants) {
         this.participants = participants;
       }
     }     
  });
