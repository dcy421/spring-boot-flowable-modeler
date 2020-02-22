package com.dcy.api;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.dcy.common.model.ResponseData;
import com.dcy.dto.ProcessInstanceDTO;
import com.dcy.dto.TaskDTO;
import com.dcy.dto.TodoListDTO;
import com.dcy.entity.ProcessInstanceVo;
import com.google.common.collect.Lists;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.flowable.ui.modeler.domain.Model;
import org.flowable.ui.modeler.serviceapi.ModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    // 流程实例服务
    @Autowired
    private RuntimeService runtimeService;

    // 流程节点任务服务
    @Autowired
    private TaskService taskService;

    // 历史数据服务
    @Autowired
    private HistoryService historyService;


    @ApiOperation(value = "根据流程id部署流程", notes = "根据流程id部署流程")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "modelId", value = "流程id", dataType = "String", paramType = "path", required = true)
    })
    @GetMapping("/deploy/{modelId}")
    public ResponseData<String> deploy(@PathVariable(value = "modelId") String modelId) {
        // 根据模型 ID 获取模型
        Model modelData = modelService.getModel(modelId);
        byte[] bytes = modelService.getBpmnXML(modelData);
        if (bytes == null) {
            logger.error("模型数据为空，请先设计流程并成功保存，再进行发布");
        }

        BpmnModel model = modelService.getBpmnModel(modelData);
        if (model.getProcesses().size() == 0) {
            logger.error("数据模型不符要求，请至少设计一条主线流程");
        }
        byte[] bpmnBytes = new BpmnXMLConverter().convertToXML(model);
        String processName = modelData.getName() + ".bpmn20.xml";

        // 部署流程
        Deployment deploy = repositoryService.createDeployment()
                .name(modelData.getName())
                .addBytes(processName, bpmnBytes)
                .deploy();
        logger.info("流程部署成功：" + modelId + " " + new Date());
        return ResponseData.success(deploy.getId());
    }


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


    @ApiOperation(value = "根据任务id获取当前候选组", notes = "根据任务id获取当前候选组")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "taskId", value = "任务id", dataType = "String", paramType = "path", required = true)
    })
    @GetMapping("/taskInfo/{taskId}")
    public List<String> taskInfo(@PathVariable(value = "taskId") String taskId) {
        List<String> group = new ArrayList<>();
        List<IdentityLink> taskName = taskService.getIdentityLinksForTask(taskId);
        taskName.forEach(identityLink -> {
            group.add(identityLink.getGroupId());
        });
        return group;
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


    @ApiOperation(value = "获取代表列表", notes = "获取代表列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "todoListDTO", value = "任务对象", dataType = "TodoListDTO", paramType = "query", required = true)
    })
    @GetMapping("/getRunList")
    public ResponseData<List<String>> getRunList(TodoListDTO todoListDTO) {
        // =============== 已经签收的任务 ===============
        TaskQuery todoTaskQuery = taskService.createTaskQuery().taskAssignee(todoListDTO.getUserId()).active();

        // 设置查询条件
        if (StrUtil.isNotBlank(todoListDTO.getProcDefKey())) {
            todoTaskQuery.processDefinitionKey(todoListDTO.getProcDefKey());
        }
        if (todoListDTO.getBeginDate() != null) {
            todoTaskQuery.taskCreatedAfter(todoListDTO.getBeginDate());
        }
        if (todoListDTO.getEndDate() != null) {
            todoTaskQuery.taskCreatedBefore(todoListDTO.getEndDate());
        }

        // 查询列表
        List<String> result = todoTaskQuery.list().stream().map(TaskInfo::getProcessInstanceId).collect(Collectors.toList());

        // =============== 等待签收的任务 ===============
        TaskQuery toClaimQuery = taskService.createTaskQuery().taskCandidateUser(todoListDTO.getUserId()).active();
        // 设置查询条件
        if (StrUtil.isNotBlank(todoListDTO.getProcDefKey())) {
            toClaimQuery.processDefinitionKey(todoListDTO.getProcDefKey());
        }
        if (todoListDTO.getBeginDate() != null) {
            toClaimQuery.taskCreatedAfter(todoListDTO.getBeginDate());
        }
        if (todoListDTO.getEndDate() != null) {
            toClaimQuery.taskCreatedBefore(todoListDTO.getEndDate());
        }

        // 查询列表
        result.addAll(toClaimQuery.list().stream().map(TaskInfo::getProcessInstanceId).collect(Collectors.toList()));
        return ResponseData.success(result);
    }


    @ApiOperation(value = "获取历史任务", notes = "获取历史任务")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "todoListDTO", value = "任务对象", dataType = "TodoListDTO", paramType = "query", required = true)
    })
    @GetMapping("/getHisList")
    public ResponseData<List<String>> getHisList(TodoListDTO todoListDTO) {
        HistoricTaskInstanceQuery histTaskQuery = historyService.createHistoricTaskInstanceQuery().taskAssignee(todoListDTO.getUserId()).finished()
                .orderByHistoricTaskInstanceEndTime().desc();
        // 设置查询条件
        if (StrUtil.isNotBlank(todoListDTO.getProcDefKey())) {
            histTaskQuery.processDefinitionKey(todoListDTO.getProcDefKey());
        }
        if (todoListDTO.getBeginDate() != null) {
            histTaskQuery.taskCompletedAfter(todoListDTO.getBeginDate());
        }
        if (todoListDTO.getEndDate() != null) {
            histTaskQuery.taskCompletedBefore(todoListDTO.getEndDate());
        }
        // 查询列表
        List<String> result = histTaskQuery.list().stream().map(TaskInfo::getProcessInstanceId).collect(Collectors.toList());
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

