package com.dcy.api;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.PageUtil;
import cn.hutool.core.util.StrUtil;
import com.dcy.common.model.ResponseData;
import com.dcy.db.base.model.PageModel;
import com.dcy.entity.ProcessDefinitionVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author：dcy
 * @Description:
 * @Date: 2020-02-23 11:15
 */
@Slf4j
@RestController
@RequestMapping("/process")
@Api(value = "ProcessApiController", tags = {"流程操作接口"})
public class ProcessApiController {

    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private ManagementService managementService;

    @ApiOperation(value = "获取流程列表", notes = "获取流程列表")
    @GetMapping(value = "/page")
    public ResponseData<Map<String, Object>> list(PageModel pageModel) {
        StringBuilder sql = new StringBuilder("SELECT * FROM " + managementService.getTableName(ProcessDefinition.class));
        if (StrUtil.isNotBlank(pageModel.getOrder()) && StrUtil.isNotBlank(pageModel.getSort())) {
            sql.append(" ORDER BY ")
                    .append(StrUtil.toUnderlineCase(pageModel.getSort()))
                    .append(" ")
                    .append(pageModel.getSort());
        }
        // 获取数据总条数
        int total = repositoryService.createNativeProcessDefinitionQuery().sql(sql.toString()).list().size();
        // 拼接分页
        sql.append(" LIMIT ")
                .append(PageUtil.getStart(pageModel.getCurrent(), pageModel.getSize()))
                .append(",")
                .append(pageModel.getSize());

        List<ProcessDefinitionVo> list = new ArrayList<>();
        repositoryService.createNativeProcessDefinitionQuery().sql(sql.toString()).list().stream().forEach(processDefinition -> {
            ProcessDefinitionVo processDefinitionVo = new ProcessDefinitionVo(processDefinition);
            Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(processDefinition.getDeploymentId()).singleResult();
            processDefinitionVo.setDeploymentDate(DateUtil.format(deployment.getDeploymentTime(), DatePattern.NORM_DATETIME_PATTERN));
            list.add(processDefinitionVo);
        });
        Map<String, Object> map = new HashMap<>();
        map.put("current", pageModel.getCurrent());
        map.put("pages", PageUtil.totalPage(total, pageModel.getSize()));
        map.put("records", list);
        map.put("size", pageModel.getSize());
        map.put("total", total);
        return ResponseData.success(map);
    }

    @ApiOperation(value = "根据流程定义id 操作挂起激活", notes = "根据流程定义id 操作挂起激活 true 挂起， false 未挂起")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "processDefinedId", value = "流程定义id", dataType = "String", paramType = "path", required = true)
    })
    @GetMapping(value = "/hangChange/{processDefinedId}")
    public ResponseData<String> hangChange(@PathVariable(value = "processDefinedId") String processDefinedId) {
        // 判断挂起状态，true 挂起， false 未挂起
        if (repositoryService.isProcessDefinitionSuspended(processDefinedId)) {
            // 激活
            repositoryService.activateProcessDefinitionById(processDefinedId);
        } else {
            // 挂起
            repositoryService.suspendProcessDefinitionById(processDefinedId);
        }
        return ResponseData.success();
    }

    @ApiOperation(value = "删除流程定义", notes = "删除流程定义")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "deploymentId", value = "流程定义id", dataType = "String", paramType = "path", required = true)
    })
    @DeleteMapping(value = "/delete/{deploymentId}")
    public ResponseData<String> deleteProcess(@PathVariable(value = "deploymentId") String deploymentId) {
        // 级联删除，实例，历史都会被删除
        repositoryService.deleteDeployment(deploymentId, true);
        return ResponseData.success();
    }
}
