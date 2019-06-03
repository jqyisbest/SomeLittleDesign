/*
 * Copyright (c) 2019. JQY and/or its organisation. All rights reserved.
 */

package net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.pdp;

/**
 * 支持解析的预定义参数
 *
 * @project:acr
 * @fully_qualified_name: net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.pdp.PredefinedParameter
 * @author: JQY
 * @create: 2019/5/29 Via IntelliJ IDEA
 **/
public enum PredefinedParameter {
    /**
     * 与日期相关的参数，用1**表示
     *
     * TODAY：今天
     * 
     * YESTERDAY：昨天
     * 
     * LAST_YYYYMMDD_OF_CURRENT_MONTH：当前月的最后一天
     * 
     * LAST_DAY_OF_LAST_MONTH：上一月的最后一天
     */
    ANY_DAY("getDate"),TODAY("getToday"), YESTERDAY("getYesterday"), LAST_DAY_OF_CURRENT_MONTH("getLastDayOfCurrentMonth"), LAST_DAY_OF_LAST_MONTH("getLastDayOfLastMonth");

    private String methodName;

    PredefinedParameter(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodName() {
        return methodName;
    }
}
