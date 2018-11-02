angular.module('os.administrative.models')
  .factory('ContainerLabelPrinter', function(osModel, $http, $q, Util, SettingUtil, Alerts) {
    var ContainerLabelPrinter = osModel('container-label-printer');
 
    ContainerLabelPrinter.getTokens = function() {
      return $http.get(ContainerLabelPrinter.url() + 'tokens').then(
        function(resp) {
          return resp.data;
        }
      );
    }

    ContainerLabelPrinter.printLabels = function(detail, outputFilename) {
      var printQ   = $http.post(ContainerLabelPrinter.url(), detail);
      var settingQ = SettingUtil.getSetting('administrative', 'download_labels_print_file');
      return $q.all([printQ, settingQ]).then(
        function(resps) {
          var job = resps[0].data;
          if (resps[1].value == 'true' || resps[1].value == true) {
            var url = ContainerLabelPrinter.url() + 'output-file?jobId=' + job.id;
            if (outputFilename) {
              outputFilename = outputFilename.replace(/\/|\\/g, '_');
              url += '&filename=' + outputFilename;
            }

            Util.downloadFile(url);
            Alerts.info("container.labels_print_download");
          } else {
            Alerts.success("container.labels_print_job_created", {jobId: job.id});
          }

          return job;
        }
      );
    };

    return ContainerLabelPrinter;
  });
