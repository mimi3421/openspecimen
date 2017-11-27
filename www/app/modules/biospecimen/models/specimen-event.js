angular.module('os.biospecimen.models.specimenevent', ['os.common.models'])
  .factory('SpecimenEvent', function(osModel, $http, Form) {
    var SpecimenEvent = osModel('specimen-events');

    var sysEvents = [
      'SpecimenDisposalEvent',
      'SpecimenDistributedEvent',
      'SpecimenTransferEvent',
      'SpecimenShipmentShippedEvent',
      'SpecimenShipmentReceivedEvent',
      'SpecimenReturnEvent',
      'SpecimenChildrenEvent'
    ];
    
    SpecimenEvent.getEvents = function() {
      return Form.listForms('SpecimenEvent');
    }

    SpecimenEvent.save = function(formId, data) {
      return $http.post(SpecimenEvent.url() + '/' + formId, data);
    }

    SpecimenEvent.addSysEvent = function(eventName) {
      if (sysEvents.indexOf(eventName) == -1) {
        sysEvents.push(eventName);
      }
    }

    SpecimenEvent.isSysEvent = function(eventName) {
      return sysEvents.indexOf(eventName) != -1;
    }
    
    return SpecimenEvent;
  });
