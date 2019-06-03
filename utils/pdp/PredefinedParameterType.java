/*
 * Copyright (c) 2019. JQY and/or its organisation. All rights reserved.
 */

package net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.pdp;

/**
 * 预定义参数类型
 *
 * @project:acr
 * @fully_qualified_name: net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.pdp.PredefinedParameterType
 * @author: JQY
 * @create: 2019/5/29 Via IntelliJ IDEA
 **/
public enum PredefinedParameterType {
    /**
     * 预定义参数类型
     *
     * DATE_AND_TIME：日期时间类的预定义参数
     */
    DATE_AND_TIME("net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.pdp.DateTimeTranslator");

    /**
     * 全限定类名
     */
    private String className;

    PredefinedParameterType(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
