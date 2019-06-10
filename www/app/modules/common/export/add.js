
angular.module('os.common.export')
  .controller('AddEditExportJobCtrl', function($scope, $state, $timeout, exportDetail, ExportJob, Alerts, Util) {
    function init() {
      $scope.exportDetail = exportDetail;
      exportDetail.type   = exportDetail.type || {};
      exportDetail.params = exportDetail.params || {};

      if (exportDetail.types && exportDetail.types.length > 0) {
        onTypeSelect(exportDetail.types[0]);
      }
    }

    function onTypeSelect(type) {
      exportDetail.type = type;
    }

    function warnNoInput(emptyInputAllowed, type) {
      return !emptyInputAllowed && !!type.$$input && !!type.$$input.var;
    }

    $scope.export = function(emptyInputAllowed) {
      var type = exportDetail.type;
      var job  = new ExportJob({objectType: type.type, params: type.params || {}});
      if (!!exportDetail.inputCsv) {
        job.params[type.$$input.var] = exportDetail.inputCsv;
      } else if (!exportDetail.inputCsv && warnNoInput(emptyInputAllowed, type)) {
        Util.showConfirm({
          title: 'export.confirm_export_all_title',
          confirmMsg: 'export.confirm_export_all_msg',
          input: type,
          ok: function() { $scope.export(true); },
          cancel: function() { }
        });

        return;
      }

      var msg = Alerts.info('export.initiated');
      job.$saveOrUpdate().then(
        function(savedJob) {
          Alerts.remove(msg);
          if (savedJob.status == 'COMPLETED') {
            Alerts.info('export.downloading_file');
            $timeout(function() { Util.downloadFile(savedJob.fileUrl()); }, 250);
          } else if (savedJob.status == 'FAILED') {
            Alerts.error('export.failed', savedJob);
          } else {
            Alerts.info('export.file_will_be_emailed', savedJob);
          }
          $state.go(exportDetail.onSuccess.state, exportDetail.onSuccess.params);
        }
      );
    }

    $scope.onTypeSelect = onTypeSelect;

    init();
  });
