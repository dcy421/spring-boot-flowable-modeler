package com.dcy.dto;

import lombok.Data;

import java.util.Date;

@Data
public class TodoListDTO {

    private String userId;
    /**
     * 流程定义Key（流程定义标识）
     */
    private String procDefKey;
    /**
     * 开始查询日期
     */
    private Date beginDate;
    /**
     * 结束查询日期
     */
    private Date endDate;
}
