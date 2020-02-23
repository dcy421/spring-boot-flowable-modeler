package com.dcy.db.base.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class PageModel {

    @ApiModelProperty("当前页面")
    private int current = 1;

    @ApiModelProperty("每页多少条")
    private int size = 30;

    @ApiModelProperty("排序字段")
    private String sort;

    @ApiModelProperty("排序类型")
    private String order;
}
