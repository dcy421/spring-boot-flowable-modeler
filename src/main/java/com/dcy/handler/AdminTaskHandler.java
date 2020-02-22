package com.dcy.handler;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;

import java.util.Arrays;
import java.util.List;

public class AdminTaskHandler implements TaskListener {


    @Override
    public void notify(DelegateTask delegateTask) {
//        delegateTask.setAssignee("admin");
        List<String> strings = Arrays.asList(new String[]{"admin", "admin2", "admin3"});
        delegateTask.addCandidateUsers(strings);

        // 1.取得流程Id
        /*String flowId = task.getProcessDefinitionId();
        // 2.取得当前节点key
        String taskKey = task.getTaskDefinitionKey();
        // 3.取得当前任务的orgId
        String processInId = task.getProcessInstanceId();
        String orgId = "";
        String businessKey = task.getExecution().getProcessBusinessKey();
        // 常规申请
        if (businessKey.startsWith("d_apply")) {
            ApplyService applyService = SpringContextHolder.getBean(ApplyService.class);
            orgId = applyService.searchByProcInsId(processInId);
            // 养护申请
        } else if (businessKey.startsWith("d_maintain_task")) {
            MaintainTaskService maintainTaskService = SpringContextHolder.getBean(MaintainTaskService.class);
            orgId = maintainTaskService.searchByProcInsId(processInId);
        }
        // 如果orgId为空则，此时为申请阶段，去当前用户的orgId即可
        if (StringUtils.isBlank(orgId)) {
            User user = UserUtils.getUser();
            orgId = user.getCompany().getId();
        }
        // 4.节点用户配置
        TaskRoleService taskRoleService = SpringContextHolder.getBean(TaskRoleService.class);
        List<String> userList = taskRoleService.userList(orgId, flowId, taskKey);
        task.addCandidateUsers(userList);*/
    }
}
