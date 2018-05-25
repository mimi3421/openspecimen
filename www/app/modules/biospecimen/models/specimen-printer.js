angular.module('os.biospecimen.models.specimenlabelprinter', ['os.common.models'])
  .factory('SpecimenLabelPrinter', function(osModel, $http, $q, Util, SettingUtil, Alerts) {
    var SpecimenLabelPrinter = osModel('specimen-label-printer');
 
    SpecimenLabelPrinter.getTokens = function() {
      return $http.get(SpecimenLabelPrinter.url() + 'tokens').then(
        function(resp) {
          return resp.data;
        }
      );
    }

    SpecimenLabelPrinter.printLabels = function(detail, outputFilename) {
      var printQ   = $http.post(SpecimenLabelPrinter.url(), detail);
      var settingQ = SettingUtil.getSetting('biospecimen', 'download_labels_print_file');
      return $q.all([printQ, settingQ]).then(
        function(resps) {
          var job = resps[0].data;
          if (resps[1].value == 'true' || resps[1].value == true) {
            var url = SpecimenLabelPrinter.url() + 'output-file?jobId=' + job.id;
            if (outputFilename) {
              outputFilename = outputFilename.replace(/\/|\\/g, '_');
              url += '&filename=' + outputFilename;
            }

            Util.downloadFile(url);
            Alerts.info("specimens.labels_print_download");
          } else {
            Alerts.success("specimens.labels_print_job_created", {jobId: job.id});
          }

          return job;
        }
      );
    };

    return SpecimenLabelPrinter;
  });
