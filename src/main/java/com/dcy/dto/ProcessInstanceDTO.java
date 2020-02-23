package com.dcy.dto;

import lombok.Data;
import org.flowable.engine.runtime.ProcessInstance;

import java.util.Date;
import java.util.Map;

/**
 * ProcessInstanceVo
 * @author: linjinp
 * @create: 2020-01-02 14:15
 **/
@Data
public class ProcessInstanceDTO {

    /**
     * 部署的流程 key，来自 ACT_RE_PROCDEF
     */
    private String processDefinitionKey;

    /**
     * 数据 Key，业务键，一般为表单数据的 ID，仅作为表单数据与流程实例关联的依据
     */
    private String businessKey;

    /**
     * 流程变量
     */
    private Map<String, Object> variables;

    /**
     * 用户 Id
     */
    private String userId;


}
