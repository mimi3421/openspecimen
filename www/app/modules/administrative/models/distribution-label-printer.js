angular.module('os.administrative.models.order')
  .factory('DistributionLabelPrinter', function(osModel, $http, $q, Util, SettingUtil, Alerts) {
    var DistributionLabelPrinter = osModel('distribution-label-printer');
 
    DistributionLabelPrinter.getTokens = function() {
      return $http.get(DistributionLabelPrinter.url() + 'tokens').then(
        function(resp) {
          return resp.data;
        }
      );
    }

    DistributionLabelPrinter.printLabels = function(detail, outputFilename) {
      var printQ   = $http.post(DistributionLabelPrinter.url(), detail);
      var settingQ = SettingUtil.getSetting('administrative', 'download_labels_print_file');
      return $q.all([printQ, settingQ]).then(
        function(resps) {
          var job = resps[0].data;
          if (resps[1].value == 'true' || resps[1].value == true) {
            var url = DistributionLabelPrinter.url() + 'output-file?jobId=' + job.id;
            if (outputFilename) {
              outputFilename = outputFilename.replace(/\/|\\/g, '_');
              url += '&filename=' + outputFilename;
            }

            Util.downloadFile(url);
            Alerts.info("orders.labels_print_download");
          } else {
            Alerts.success("orders.labels_print_job_created", {jobId: job.id});
          }

          return job;
        }
      );
    };

    return DistributionLabelPrinter;
  });
