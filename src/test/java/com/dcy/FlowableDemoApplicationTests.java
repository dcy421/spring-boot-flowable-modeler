package com.dcy;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.*;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
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
public class FlowableDemoApplicationTests {

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

    @Test
    public void contextLoads() {
    }


    @Test
    public void deployFlow() {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("flowable/test-flowable.zip");
        ZipInputStream zipInputStream = new ZipInputStream(in);
        Deployment deployment = repositoryService//获取流程定义和部署对象相关的Service
                .createDeployment()//创建部署对象
                .addZipInputStream(zipInputStream)//使用zip方式部署，将approve.bpmn和approve.png压缩成zip格式的文件
                .deploy();//完成部署
        System.out.println("部署ID：" + deployment.getId());
        System.out.println("部署时间：" + deployment.getDeploymentTime());
    }

    /**
     * 获取部署list
     */
    @Test
    public void deployFlowList() {
        repositoryService.createDeploymentQuery().list().stream().forEach(deployment -> {
            System.out.println(deployment);
        });
    }

    /**
     * 启动流程
     */
    @Test
    public void strartFlow() {
        String processKey = "student-leave2-key";
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processKey);
        log.info("流程实例ID -- {}", processInstance.getId());
        log.info("流程定义ID -- {}", processInstance.getProcessDefinitionId());
    }


    /**
     * 查询我的个人任务列表
     */
    @Test
    public void findMyTaskList() {
        String userId = "admin";
        TaskQuery taskQuery = taskService
                .createTaskQuery()
                .taskAssignee(userId);
        List<Task> list = taskQuery
                .list();
        TaskQuery taskQuery1 = taskService.createTaskQuery()
                .taskCandidateUser(userId);

        List<Task> list1 = taskQuery1
                .list();
        list.addAll(list1);
        if (list.size() > 0) {
            for (Task task : list) {
                System.out.println("task id=" + task.getId());
                System.out.println("name=" + task.getName());
                System.out.println("assinee=" + task.getAssignee());
                System.out.println("executionId=" + task.getExecutionId());
                System.out.println("=====================================");
            }
        }
    }

    @Test
    public void findMyTaskList2() {
        String userId = "admin2";
        List<Task> list = taskService.createNativeTaskQuery().sql("SELECT DISTINCT\n" +
                "\tRES.* \n" +
                "FROM\n" +
                "\tACT_RU_TASK RES\n" +
                "\tLEFT JOIN ACT_RU_IDENTITYLINK I ON I.TASK_ID_ = RES.ID_ \n" +
                "WHERE\n" +
                "\tRES.ASSIGNEE_ = '"+userId+"' OR ( RES.ASSIGNEE_ IS NULL AND I.USER_ID_ = '"+userId+"' )").list();
        for (Task task : list) {
            System.out.println("task id=" + task.getId());
            System.out.println("name=" + task.getName());
            System.out.println("assinee=" + task.getAssignee());
            System.out.println("executionId=" + task.getExecutionId());
            System.out.println("=====================================");
        }
    }

    /**
     * 查看组任务
     * 个人认领任务后，组任务不再存在，若将个人任务回退到组任务，又可以看到组任务
     */
    @Test
    public void findMyGroupTask() {
        String group = "teacher";
        List<Task> list = taskService
                .createTaskQuery()
                .taskCandidateGroup(group)
                .list();
        if (list != null && list.size() > 0) {
            for (Task task : list) {
                System.out.println("task id=" + task.getId());
                System.out.println("name=" + task.getName());
                System.out.println("assinee=" + task.getAssignee());
                System.out.println("executionId=" + task.getExecutionId());
                System.out.println("=====================================");
            }
        }
    }

    /**
     * 查看组任务成员列表
     */
    @Test
    public void findGroupUser() {
        String taskId = "20008";
        List<IdentityLink> list = taskService
                .getIdentityLinksForTask(taskId);// 获取列表
        if (list != null && list.size() > 0) {
            for (IdentityLink il : list) {
                System.out.println("用户ID：" + il.getUserId());
            }
        }
    }

    /**
     * 查询组任务成员历史列表
     */
    @Test
    public void findHisGroupUser() {
        String taskId = "20008";
        List<HistoricIdentityLink> list = historyService
                .getHistoricIdentityLinksForTask(taskId);
        if (list != null && list.size() > 0) {
            for (HistoricIdentityLink il : list) {
                System.out.println("用户ID：" + il.getUserId());
            }
        }
    }

    /**
     * 将组任务分配给个人任务，拾取任务
     * 注意：认领任务的时候，可以是组任务成员中的人，也可以不是组任务成员的人，此时通过Type的类型为participant来指定任务的办理人
     * 由1个人去完成任务
     */
    @Test
    public void claim() {
        String taskId = "90c0835f-53ab-11ea-803b-84ef180dd117";//任务ID
        String userId = "user1";//分配的办理人
        taskService.claim(taskId, userId);
    }

    /**
     * 完成我的个人任务
     */
    @Test
    public void complete() {
        String taskId = "17940c77-53ac-11ea-a73e-84ef180dd117";
        taskService.complete(taskId);
        System.out.println("完成任务");
    }

    @Test
    public void completeVar() {
        String taskId = "03bbffe4-1b2a-11ea-970a-04d4c48d288f";
        Map<String, Object> map = new HashMap<>();
        map.put("userIds", "tea");
        taskService.complete(taskId, map);
        System.out.println("完成任务");
    }

    /**
     * 可以分配个人任务回退到组任务，（前提：之前是个组任务）
     */
    @Test
    public void setAssigneeTask() {
        String taskId = "20008";//任务ID
        taskService.setAssignee(taskId, null);
        taskService.complete(taskId);
    }

    /**
     * 添加组成员
     */
    @Test
    public void addGroupUser() {
        String taskId = "20008";
        String userId = "ken";
        taskService.addCandidateUser(taskId, userId);
    }

    /**
     * 删除组成员
     */
    @Test
    public void deleteGroupUser() {
        String taskId = "2509";
        String userId = "ken";
        taskService
                .deleteCandidateUser(taskId, userId);
    }


    /**
     * userTask --- {"taskName":"学生请假","taskId":"sid-59052D77-0C0C-429D-871D-34EC1F4803E2"}
     * userTask --- {"taskName":"老师审批","taskId":"sid-AC63C52A-3D99-4BF3-A01E-689AF85C5982"}
     */
    @Test
    public void getUserTaskList() {
        String flowId = "student-leave-key:1:67930616-53a1-11ea-bfcf-84ef180dd117";
        List<Map<String, String>> userTaskList = new ArrayList<Map<String, String>>();
        BpmnModel process = repositoryService.getBpmnModel(flowId);
        if (process != null) {
            Collection<FlowElement> flowElements = process.getMainProcess().getFlowElements();
            for (FlowElement flowElement : flowElements) {
                if (flowElement instanceof UserTask) {
                    Map<String, String> map = new HashMap<String, String>();
                    map.put("taskId", flowElement.getId());
                    map.put("taskName", flowElement.getName());
                    userTaskList.add(map);
                }
            }
        }

        userTaskList.stream().forEach(stringStringMap -> {
            log.info("userTask --- {}", JSON.toJSONString(stringStringMap));
        });
    }

    @Test
    public void getFlowList() {
        repositoryService.createProcessDefinitionQuery().latestVersion().active().list().stream().forEach(processDefinition -> {
            log.info("流程定义ID -- {}", processDefinition.getId());
            log.info("流程定义名称 -- {}", processDefinition.getName());
        });

    }
    //=====================用户和用户组操作============================

    @Test
    public void addUser() {
        for (int i = 1; i < 10; i++) {
            User user = identityService.newUser("user" + i);
            user.setFirstName("用户" + i);
            user.setPassword("123456");
            user.setEmail("157864564@qq.com");
            identityService.saveUser(user);
        }

    }

    @Test
    public void delUser() {
        identityService.deleteUser("pri");
    }


    @Test
    public void addGroup() {
        Group group = identityService.newGroup("teacher");
        group.setName("老师组");
        identityService.saveGroup(group);
        Group group2 = identityService.newGroup("headmaster");
        group2.setName("校长组");
        identityService.saveGroup(group2);
    }

    @Test
    public void delGroup() {
        identityService.deleteGroup("108-school");
    }

    /**
     * 测试添加用户和组关联关系
     */
    @Test
    public void testSaveMembership() {
        identityService.createMembership("user1", "teacher");
        identityService.createMembership("user2", "teacher");
        identityService.createMembership("user3", "headmaster");
        identityService.createMembership("user4", "headmaster");
    }

    /**
     * 测试删除用户和组关联关系
     */
    @Test
    public void testDeleteMembership() {
        identityService.deleteMembership("lisi", "test");
    }
}
