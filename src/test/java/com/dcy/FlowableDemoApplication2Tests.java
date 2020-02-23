package com.dcy;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.engine.*;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ModelQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.ui.modeler.service.FlowableModelQueryService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipInputStream;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class FlowableDemoApplication2Tests {

    /**
     * 1）查询部署和流程定义。
     * 2）挂起或者激活流程部署或者流程定义。大多数是处理一些静态信息。
     */
    @Autowired
    private RepositoryService repositoryService;
    /**
     * 1）用来获取和保存流程变量。
     * 2）查询流程实例和当前执行位置。
     * 3）流程实例需要等待一个外部触发，从而继续进行。
     */
    @Autowired
    private RuntimeService runtimeService;
    /**
     * 1）查询分配给某人或一组的任务。
     * 2）产生独立的任务，不依赖于任何流程实例的任务。
     * 3）操纵用户分配任务。
     * 4）完成任务。
     */
    @Autowired
    private TaskService taskService;
    /**
     * 1）查询各种历史数据
     */
    @Autowired
    private HistoryService historyService;
    /**
     * 1）查询数据库表信息和表的元数据。
     * 2）管理各种 jobs 任务，例如定时器、延迟、挂起
     */
    @Autowired
    private ManagementService managementService;
    /**
     * 1）管理组和用户的身份认证信息
     */
    @Autowired
    private IdentityService identityService;

    @Autowired
    private FlowableModelQueryService modelQueryService;

    @Test
    public void contextLoads() {
    }


    @Test
    public void geModelList() {
        ModelQuery modelQuery = repositoryService.createModelQuery().latestVersion().orderByLastUpdateTime().desc();
        modelQuery.list().stream().forEach(model -> {
            System.out.println(JSON.toJSONString(model));
        });

    }

}
