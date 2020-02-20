//package com.dcy.api;
//
//import com.springcloud.entity.*;
//import com.springcloud.util.JsonUtil;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.flowable.engine.RuntimeService;
//import org.flowable.engine.TaskService;
//import org.flowable.engine.runtime.ProcessInstance;
//import org.flowable.identitylink.api.IdentityLink;
//import org.flowable.task.api.Task;
//import org.flowable.task.api.TaskQuery;
//import org.springframework.web.bind.annotation.*;
//
//import javax.annotation.Resource;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//import java.util.Map;
//
///**
// * 流程任务相关接口封装
// * @author: linjinp
// * @create: 2019-11-05 14:55
// **/
//@RestController
//@RequestMapping("/flowable/task/api")
//public class TaskApiController {
//
//    public static final Logger logger = LogManager.getLogger(TaskApiController.class);
//
//    @Resource
//    private RuntimeService runtimeService;
//
//    @Resource
//    private TaskService taskService;
//
//    /**
//     * 流程节点完成
//     * @param taskId   任务 Id，来自 ACT_RU_TASK
//     * @param assignee 设置审核人，替换
//     * @param comment 审核建议
//     * @param map      完成任务需要的条件参数
//     * @return
//     */
//    @PostMapping(value = "/flowTaskCompleted")
//    public String flowTaskCompleted(@RequestParam(value = "taskId") String taskId, @RequestParam(value = "assignee") String assignee, @RequestParam(value = "comment", defaultValue = "") String comment, @RequestBody Map<String, Object> map) {
//        if (taskService.createTaskQuery().taskId(taskId).singleResult().isSuspended()) {
//            return JsonUtil.toJSON(ErrorMsg.ERROR.setNewErrorMsg("任务处于挂起状态"));
//        }
//        if (assignee != null) {
//            map.put("sys_assignee", assignee);
//            // 设置审核人
//            taskService.setAssignee(taskId, assignee);
//        }
//        // 设置审核完成说明
//        taskService.addComment(taskId, null, "success");
//        taskService.addComment(taskId, null, "通过");
//        taskService.addComment(taskId, null, comment);
//        // 完成任务
//        taskService.complete(taskId, map);
//        logger.info("任务完成：" + taskId + " " + new Date());
//        return JsonUtil.toJSON(ErrorMsg.SUCCESS.setNewErrorMsg("通过"));
//    }
//
//    /**
//     * 获取用户任务节点列表
//     * @param flowableQueryEntity 查询实体类
//     * @return
//     */
//    @PostMapping(value = "/getAssigneeTaskList")
//    public String getAssigneeTaskList(@RequestBody FlowableQueryEntity flowableQueryEntity) {
//        // 流程定义Key 列表
//        List<String> processDefinitionKeyList = flowableQueryEntity.getProcessDefinitionKeyList();
//        // 租户
//        String tanantId = flowableQueryEntity.getTenantId();
//        // 开始时间
//        Date startTime = flowableQueryEntity.getStartTime();
//        // 结束时间
//        Date endTime = flowableQueryEntity.getEndTime();
//        // 审核人
//        String assignee = flowableQueryEntity.getAssignee();
//        // 页码
//        Integer page = flowableQueryEntity.getPage();
//        // 条数
//        Integer limit = flowableQueryEntity.getLimit();
//        // 查询过滤
//        TaskQuery taskQuery = taskService.createTaskQuery().taskCandidateOrAssigned(assignee);
//        if (processDefinitionKeyList != null && processDefinitionKeyList.size() > 0) {
//            taskQuery = taskQuery.processDefinitionKeyIn(processDefinitionKeyList);
//        }
//        if (startTime != null) {
//            taskQuery = taskQuery.taskCreatedAfter(startTime);
//        }
//        if (endTime != null) {
//            taskQuery = taskQuery.taskCreatedBefore(endTime);
//        }
//        if (tanantId != null && !"".equals(tanantId)) {
//            taskQuery = taskQuery.taskTenantId(tanantId);
//        }
//        // 获取总条数
//        Long total = taskQuery.count();
//        // 获取分页数据
//        List<Task> taskList = taskQuery.orderByTaskCreateTime().asc().listPage(PageEntity.startIndex(page ,limit), limit);
//        List<TaskVo> list = new ArrayList<>();
//        taskList.forEach(task -> {
//            TaskVo taskVo = new TaskVo(task);
//            // 获取流程定义信息
//            ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(taskVo.getProcessInstanceId()).singleResult();
//            taskVo.setProcessInstance(new ProcessInstanceVo(pi));
//            list.add(taskVo);
//        });
//        PageEntity pageEntity = new PageEntity(page, limit, total);
//        pageEntity.setData(list);
//        return JsonUtil.toJSON(ErrorMsg.SUCCESS.setNewData(pageEntity));
//    }
//
//    /**
//     * 流程参与人
//     *
//     * @param taskId
//     * @return
//     */
//    @GetMapping(value = "/getFlowAssignee/{taskId}")
//    public String getFlowAssignee(@PathVariable(value = "taskId") String taskId) {
//        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
//        String assignee = task.getAssignee();
//        return JsonUtil.toJSON(ErrorMsg.SUCCESS.setNewData(assignee));
//    }
//
//    /**
//     * 流程参与候选人
//     *
//     * @param taskId
//     * @return
//     */
//    @GetMapping(value = "/getFlowCandidate/{taskId}")
//    public String getFlowCandidate(@PathVariable(value = "taskId") String taskId) {
//        List<IdentityLink> identityLinkList = taskService.getIdentityLinksForTask(taskId);
//        List<String> list = new ArrayList<>();
//        identityLinkList.forEach(identityLink -> {
//            if ("candidate".equals(identityLink.getType()) && taskId.equals(identityLink.getTaskId())) {
//                list.add(identityLink.getUserId());
//            }
//        });
//        return JsonUtil.toJSON(ErrorMsg.SUCCESS.setNewData(list));
//    }
//
//    /**
//     * 流程参与候选组
//     *
//     * @param taskId
//     * @return
//     */
//    @GetMapping(value = "/getFlowGroup/{taskId}")
//    public String getFlowGroup(@PathVariable(value = "taskId") String taskId) {
//        List<IdentityLink> identityLinkList = taskService.getIdentityLinksForTask(taskId);
//        List<String> list = new ArrayList<>();
//        identityLinkList.forEach(identityLink -> {
//            if ("candidate".equals(identityLink.getType()) && taskId.equals(identityLink.getTaskId())) {
//                list.add(identityLink.getGroupId());
//            }
//        });
//        return JsonUtil.toJSON(ErrorMsg.SUCCESS.setNewData(list));
//    }
//
//    /**
//     * 任务节点是否挂起，true 挂起， false 未挂起
//     * @param taskId
//     * @return
//     */
//    @GetMapping(value = "/taskIsSuspended/{taskId}")
//    public String taskIsSuspended(@PathVariable(value = "taskId") String taskId) {
//        Boolean isSuspended = taskService.createTaskQuery().taskId(taskId).singleResult().isSuspended();
//        return JsonUtil.toJSON(ErrorMsg.SUCCESS.setNewData(isSuspended));
//    }
//}