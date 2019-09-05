angular.module('os.administrative.containertask')
  .controller('ContainerTaskListCtrl', function($scope, $state, currentUser, ContainerTask, Util, ListPagerOpts) {

    var pagerOpts, ctx;

    function init() {
      ctx = $scope.ctx = {allowEdits: currentUser.admin || currentUser.instituteAdmin};

      pagerOpts = ctx.pagerOpts = new ListPagerOpts({listSizeGetter: getTasksCount});
      ctx.filterOpts = {maxResults: pagerOpts.recordsPerPage + 1};
      loadTasks(ctx.filterOpts);
      Util.filter($scope, 'ctx.filterOpts', loadTasks);
    }

    function loadTasks(filterOpts) {
      ContainerTask.query(filterOpts).then(
        function(tasks) {
          pagerOpts.refreshOpts(tasks);
          ctx.tasks = tasks;
        }
      );
    };

    function getTasksCount() {
      return ContainerTask.getCount(ctx.filterOpts);
    }

    $scope.showEditTask = function(task) {
      $state.go('container-task-addedit', {taskId: task.id});
    }

    $scope.archiveTask = function(task) {
      var opts = {
        title:      'container_task.confirm_archive_title',
        confirmMsg: 'container_task.confirm_archive_q',
        input:      task
      };

      Util.showConfirm(opts).then(
        function() {
          var toSave = angular.copy(task);
          toSave.activityStatus = 'Closed';
          toSave.$saveOrUpdate().then(
            function() {
              loadTasks(ctx.filterOpts);
            }
          );
        }
      );
    }

    init();
  });
