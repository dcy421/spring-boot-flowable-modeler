package com.dcy.api;

import com.dcy.common.model.ResponseData;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 流程任务相关接口封装
 *
 * @author: linjinp
 * @create: 2019-11-05 14:55
 **/
@Slf4j
@RestController
@RequestMapping("/flowable/task/api")
public class TaskApiController {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;


    /**
     * 任务节点是否挂起，true 挂起， false 未挂起
     *
     * @param taskId
     * @return
     */
    @GetMapping(value = "/taskIsSuspended/{taskId}")
    public ResponseData<Boolean> taskIsSuspended(@PathVariable(value = "taskId") String taskId) {
        Boolean isSuspended = taskService.createTaskQuery().taskId(taskId).singleResult().isSuspended();
        return ResponseData.success(isSuspended);
    }
}