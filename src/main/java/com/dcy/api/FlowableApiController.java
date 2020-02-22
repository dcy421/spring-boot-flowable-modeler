package com.dcy.api;

import cn.hutool.core.util.StrUtil;
import com.dcy.common.model.ResponseData;
import com.dcy.dto.ProcessInstanceDTO;
import com.dcy.dto.TaskDTO;
import com.dcy.entity.ProcessInstanceVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.flowable.engine.*;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;
import org.flowable.ui.modeler.serviceapi.ModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author：dcy
 * @Description:
 * @Date: 2020-02-21 10:22
 */
@RestController
@RequestMapping("/flowable/api")
@Api(value = "FlowableApiController", tags = {"流程api操作接口"})
public class FlowableApiController {

    public static final Logger logger = LogManager.getLogger(FlowableApiController.class);

    // 流程引擎
    @Autowired
    private ProcessEngine processEngine;

    // 用户以及组管理服务
    @Autowired
    private IdentityService identityService;

    // 模型服务
    @Autowired
    private ModelService modelService;

    // 部署服务
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private ManagementService managementService;
    // 流程实例服务
    @Autowired
    private RuntimeService runtimeService;

    // 流程节点任务服务
    @Autowired
    private TaskService taskService;

    // 历史数据服务
    @Autowired
    private HistoryService historyService;

    @ApiOperation(value = "启动流程", notes = "启动流程")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "processInstanceDTO", value = "流程id", dataType = "ProcessInstanceDTO", paramType = "body", required = true)
    })
    @PostMapping("/start")
    public ResponseData<ProcessInstanceVo> start(@RequestBody ProcessInstanceDTO processInstanceDTO) {
        // 设置发起人
        identityService.setAuthenticatedUserId(processInstanceDTO.getUserId());
        // 根据流程 ID 启动流程
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processInstanceDTO.getProcessDefinitionKey(), processInstanceDTO.getBusinessKey(), processInstanceDTO.getProcessVariables());
        logger.info("流程启动成功：" + processInstance.getId() + " " + new Date());
        return ResponseData.success(new ProcessInstanceVo(processInstance));
    }

    @ApiOperation(value = "签收任务", notes = "签收任务")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "taskDTO", value = "任务对象", dataType = "TaskDTO", paramType = "body", required = true)
    })
    @PostMapping("/claim")
    public ResponseData<String> claim(@RequestBody TaskDTO taskDTO) {
        // 签收任务
        taskService.claim(taskDTO.getTaskId(), taskDTO.getAssignee());
        return ResponseData.success();
    }

    @ApiOperation(value = "完成任务", notes = "完成任务")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "taskDTO", value = "任务对象", dataType = "TaskDTO", paramType = "body", required = true)
    })
    @PostMapping("/complete")
    public ResponseData<String> task(@RequestBody TaskDTO taskDTO) {
        boolean suspended = taskService.createTaskQuery().taskId(taskDTO.getTaskId()).singleResult().isSuspended();
        if (suspended) {
            return ResponseData.error("任务已挂起，无法完成");
        }
        if (StrUtil.isNotBlank(taskDTO.getProcessInstanceId()) && StrUtil.isNotBlank(taskDTO.getComment())) {
            // 保存意见
            taskService.addComment(taskDTO.getTaskId(), taskDTO.getProcessInstanceId(), taskDTO.getComment());
        }
        if (StrUtil.isNotBlank(taskDTO.getAssignee())) {
            // 设置审核人
            taskService.setAssignee(taskDTO.getTaskId(), taskDTO.getAssignee());
        }
        // 完成任务
        taskService.complete(taskDTO.getTaskId(), taskDTO.getVariables());
        return ResponseData.success();
    }


    @ApiOperation(value = "根据用户获取代表列表流程实例ids", notes = "根据用户获取代表列表流程实例ids")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userId", value = "用户id", dataType = "String", paramType = "path", required = true)
    })
    @GetMapping("/getRunProInsIdList/{userId}")
    public ResponseData<List<String>> getRunList(@PathVariable(value = "userId") String userId) {
        // =============== 已签收和未签收同时查询 ===============
        List<String> result = taskService.createTaskQuery().taskCandidateOrAssigned(userId).active().list().stream().map(TaskInfo::getProcessInstanceId).collect(Collectors.toList());
        return ResponseData.success(result);
    }


    @ApiOperation(value = "更具用户Id获取历史任务流程实例ids", notes = "更具用户Id获取历史任务流程实例ids")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userId", value = "用户id", dataType = "String", paramType = "query", required = true)
    })
    @GetMapping("/getHisProInsIdList/{userId}")
    public ResponseData<List<String>> getHisList(@PathVariable(value = "userId") String userId) {
        List<String> result = historyService.createHistoricTaskInstanceQuery().taskAssignee(userId).finished()
                .orderByHistoricTaskInstanceEndTime().desc().list().stream().map(TaskInfo::getProcessInstanceId).collect(Collectors.toList());
        return ResponseData.success(result);
    }


    @ApiOperation(value = "根据流程实例id删除流程实例", notes = "根据流程实例id删除流程实例")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "processId", value = "流程实例id", dataType = "String", paramType = "path", required = true)
    })
    @GetMapping("/deleteProcess/{processId}")
    public void deleteProcess(@PathVariable(value = "processId") String processId) {
        runtimeService.deleteProcessInstance(processId, "删除流程");
    }


    @ApiOperation(value = "获取正在运行的数据 Id 列表", notes = "获取正在运行的数据 Id 列表")
    @GetMapping("/getRuntimeDataId")
    public List<String> getRuntimeDataId() {
        List<String> idList = new ArrayList<>();
        // 获取正在执行的任务列表
        List<Execution> list = runtimeService.createExecutionQuery().onlyProcessInstanceExecutions().list();
        list.forEach(execution -> {
            // 根据任务获取流程实例
            // 获取流程实例种的业务 key
            ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(execution.getProcessInstanceId()).singleResult();
            idList.add(pi.getBusinessKey());
        });
        return idList;
    }


    @ApiOperation(value = "根据用户，获取需要审核的业务键 business_key 列表", notes = "根据用户，获取需要审核的业务键 business_key 列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userId", value = "流程实例id", dataType = "String", paramType = "path", required = true)
    })
    @RequestMapping(value = "/getRuntimeBusinessKeyByUser/{userId}", method = RequestMethod.GET)
    public List<Map<String, Object>> getRuntimeBusinessKeyByUser(@PathVariable(value = "userId") String userId) {
        List<Map<String, Object>> idList = new ArrayList<>();
        // 根据用户获取正在进行的任务
        List<Task> tasks = taskService.createTaskQuery().taskAssignee(userId).list();
        tasks.forEach(task -> {
            Map<String, Object> data = new HashMap<>();
            // 根据任务获取流程实例
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
            // 获取流程实例中的业务键
            data.put("businessKey", processInstance.getBusinessKey());
            // 获取任务 Id
            data.put("taskId", task.getId());
            // 流程定义名称
            data.put("processInstanceName", processInstance.getProcessDefinitionName());
            // 流程开始时间
            data.put("startTime", processInstance.getStartTime());
            idList.add(data);
        });
        return idList;
    }


    @ApiOperation(value = "根据流程实例id判断流程是否结束", notes = "根据流程实例id判断流程是否结束,true 结束，false 未结束")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "processInstanceId", value = "流程实例id", dataType = "String", paramType = "path", required = true)
    })
    @RequestMapping(value = "/checkProcessInstanceFinish/{processInstanceId}", method = RequestMethod.GET)
    public ResponseData<Boolean> checkProcessInstanceFinish(@PathVariable(value = "processInstanceId") String processInstanceId) {
        boolean isFinish = false;
        // 根据流程 ID 获取未完成的流程中是否存在此流程
        long count = historyService.createHistoricProcessInstanceQuery().unfinished().processInstanceId(processInstanceId).count();
        // 不存在说明没有结束
        if (count == 0) {
            isFinish = true;
        }
        return ResponseData.success(isFinish);
    }


}

