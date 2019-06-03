/*
 * Copyright (c) 2019. JQY and/or its organisation. All rights reserved.
 */

package net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.pdp;

import java.lang.reflect.Method;

/**
 * 预定义变量解析器
 * 具体解析方法应定义为
 *      String methodName(String param);
 *      或
 *      String methodName();
 *
 * @project:acr
 * @fully_qualified_name: net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.pdp.BaseTranslator
 * @author: JQY
 * @create: 2019/05/31 9:56 Via IntelliJ IDEA
 **/
public class BaseTranslator implements Translator{
    /**
     * 解析函数
     *
     * @param methodName 实际用于解析的函数名
     * @param params     解析参数
     * @return: java.lang.String
     * @author: JQY
     * @date: 2019/5/31 9:34
     */
    @Override
    public String translate(String methodName, String params) {
        try {

            Method method=this.getClass().getDeclaredMethod(methodName,String.class);
            method.setAccessible(true);
            int parameterCount=method.getParameterCount();
            Object result=null;
            if (0==parameterCount){
                result=method.invoke(this);
            }
            if (1==parameterCount){
                result=method.invoke(this,params);
            }
            return result==null?"没有相应的预定义参数，请联系管理员。":result.toString();
        } catch (Exception e) {
            return "没有相应的预定义参数，请联系管理员。";
        }
    }
}
