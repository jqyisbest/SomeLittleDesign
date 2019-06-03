/*
 * Copyright (c) 2019. JQY and/or its organisation. All rights reserved.
 */

package net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.pdp;

/**
 * 预定义变量解析器
 *
 * @project:acr
 * @fully_qualified_name: net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.pdp.Translator
 * @author: JQY
 * @create: 2019/5/30 Via IntelliJ IDEA
 **/
public interface Translator {

    /**
     *  解析函数
     * @param methodName 实际用于解析的函数名
     * @param params 解析参数
     * @return: java.lang.String
     * @author: JQY
     * @date: 2019/5/31 9:34
     */
    String translate(String methodName,String params);
}
