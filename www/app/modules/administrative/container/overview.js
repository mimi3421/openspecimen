
angular.module('os.administrative.container.overview', ['os.administrative.models'])
  .controller('ContainerOverviewCtrl', function(
    $scope, $state, $modal, rootId, container,
    Container, ContainerLabelPrinter, DeleteUtil, Alerts, ApiUrls, Util) {

    function init() {
      $scope.ctx.showTree  = true;
      $scope.ctx.viewState = 'container-detail.overview';
      $scope.ctx.downloadUri = Container.url() + container.$id() + '/export-map';
    }

    function depth(container) {
      var nodes = 1;
      angular.forEach(container.childContainers,
        function(childContainer) {
          nodes += depth(childContainer);
        }
      );

      return nodes;
    }

    function defragment(container, aliquotsInSameContainer) {
      var alert = Alerts.info('container.defragment_rpt.initiated', {}, false);
      container.generateDefragReport(aliquotsInSameContainer).then(
        function(result) {
          Alerts.remove(alert);
          if (result.fileId) {
            Alerts.info('container.defragment_rpt.downloading');
            Util.downloadFile(ApiUrls.getBaseUrl() + 'storage-containers/defragment-report?fileId=' + result.fileId);
          } else {
            Alerts.info('container.defragment_rpt.will_be_emailed');
          }
        }
      );
    }

    $scope.printLabel = function() {
      ContainerLabelPrinter.printLabels({containerIds: [container.id]}, container.name + '.csv');
    }

    $scope.deleteContainer = function() {
      var toDelete = new Container({id: container.id, name: container.name});
      DeleteUtil.delete(toDelete, {
        onDeletion: function() {
          if (container.id == rootId) {
            //
            // current top-level container got deleted, navigate users to containers list
            //
            $state.go('container-list');
          } else {
            //
            // remove container and all of its loaded descendants from the hierarchy tree
            //
            var tree = $scope.ctx.containerTree;
            var idx = tree.indexOf(container);
            tree.splice(idx, depth(container));

            var parent = container.parent;
            idx = parent.childContainers.indexOf(container);
            parent.childContainers.splice(idx, 1);
            parent.hasChildren = (parent.childContainers.length > 0);
            $scope.selectContainer(parent);
          }
        },
        confirmDelete: 'container.confirm_delete',
        forceDelete: true
      });
    }

    $scope.defragment = function() {
      $modal.open({
        templateUrl: 'modules/administrative/container/defrag-mode.html',
        controller: function($scope, $modalInstance) {
          $scope.container = container;
          $scope.aliquotsInSameContainer = false;

          $scope.submit = function() {
            defragment(container, $scope.aliquotsInSameContainer);
            $modalInstance.close(true);
          }

          $scope.cancel = function() {
            $modalInstance.dismiss();
          }
        }
      });
    }

    init();
  });
