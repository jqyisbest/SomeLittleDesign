/*
 * Copyright (c) 2019. JQY and/or its organisation. All rights reserved.
 */

package net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.pdp;

import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

import net.gmcc.dg.common.utils.SpringUtils;

/**
 * 预定义变量解析
 *
 * @project:acr
 * @fully_qualified_name: net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.pdp.PDPTranslator
 * @author: JQY
 * @create: 2019/05/29 17:03 Via IntelliJ IDEA
 **/
public class PDPTranslator {

    /**
     * 启动参数
     */
    private Object parameter;

    /**
     * 预定义参数的标识字符
     */
    private static String identificationString;

    /**
     * 预定义参数的分割字符
     */
    private static String partitionSymbol;

    private PDPTranslator() {

    }

    /**
     *  带启动参数的构造函数
     *  如果在解析的过程中需要一些运行时的状态，可以通过这个方法穿递
     *  并将parameter.toString()附加在普通参数的后面，格式如下：
     *  普通参数 或者 启动参数 或者 普通参数partitionSymbol启动参数
     * @param parameter 启动参数
     * @return:
     * @author: JQY
     * @date: 2019/6/17 8:31
     */
    private PDPTranslator(Object parameter) {
        this.parameter=parameter;
    }

    private static void initialize(){
        Environment springEnv = SpringUtils.getBean(StandardEnvironment.class);
        // 读取配置文件
        PDPTranslator.identificationString = springEnv.getProperty("basedata.predefinedParameter.identificationString");
        PDPTranslator.partitionSymbol = springEnv.getProperty("basedata.predefinedParameter.partitionSymbol");
    }

    public static PDPTranslator getInstance() {
        initialize();
        if (null == PDPTranslator.identificationString || "".equals(PDPTranslator.identificationString)) {
            return null;
        }
        if (null == PDPTranslator.partitionSymbol || "".equals(PDPTranslator.partitionSymbol)) {
            return null;
        }
        return new PDPTranslator();
    }

    public static PDPTranslator getInstance(Object parameter) {
        initialize();
        if (null == PDPTranslator.identificationString || "".equals(PDPTranslator.identificationString)) {
            return null;
        }
        if (null == PDPTranslator.partitionSymbol) {
            return null;
        }
        return new PDPTranslator(parameter);
    }

    /**
     * 解析含预定义参数的字符串
     * 
     * @param sourceString 含待解析预定义参数的源字符串
     * @return: java.lang.String 返回已解析的目标字符串
     * @author: JQY
     * @date: 2019/5/30 8:51
     */
    public String translate(String sourceString) {
        if (null == sourceString || "".equals(sourceString.trim())) {
            return "待解析字符串为空。";
        }
        String resultString = sourceString;
        int lastIndex = 0;
        int startIndex = 0;
        int endIndex = 0;
        // 待解析预定义参数
        String targetParameter = "";
        while (true) {
            startIndex = sourceString.indexOf(PDPTranslator.identificationString, lastIndex);
            endIndex = sourceString.indexOf(PDPTranslator.identificationString, startIndex + 1);
            if (-1 == startIndex || -1 == endIndex) {
                // 没发现开始符号或者没发现结尾符号
                break;
            }
            targetParameter = sourceString.substring(startIndex + 1, endIndex);
            String[] detailParams =
                targetParameter.split(this.addBackslashToRegExChar(PDPTranslator.partitionSymbol, 1), 3);
            if (null != detailParams && detailParams.length == 3) {
                String predefinedParameterType = detailParams[0];
                String predefinedParameter = detailParams[1];
                String params="";
                if (this.parameter==null){
                    params=detailParams[2];
                }else if ("".equals(detailParams[2])){
                    params=this.parameter.toString();
                }else {
                    params=detailParams[2]+PDPTranslator.partitionSymbol+this.parameter.toString();
                }
                int isLegal=-1;
                for (PredefinedParameterType parameterType:PredefinedParameterType.values()) {
                    if (parameterType.name().equals(predefinedParameterType)){
                        ++isLegal;
                        break;
                    }
                }
                for (PredefinedParameter parameter:PredefinedParameter.values()) {
                    if (parameter.name().equals(predefinedParameter)){
                        ++isLegal;
                        break;
                    }
                }

                if (1==isLegal){
                    try {
                        String className = PredefinedParameterType.valueOf(predefinedParameterType).getClassName();
                        String methodName = PredefinedParameter.valueOf(predefinedParameter).getMethodName();
                        // 预定义变量的实际值
                        String realValue = "";
                        // 预定义变量类型的解析器类
                        Class clazz = Class.forName(className);
                        // 预定义变量类型的解析器对象
                        Object obj = clazz.newInstance();
                        if (obj instanceof BaseTranslator) {
                            realValue = ((BaseTranslator)obj).translate(methodName, params);
                        } else {
                            realValue = "请检查是否存在继承自BaseTranslator类的【" + predefinedParameterType + "】解析器。";
                        }
                        // 用结果值替换掉源字符串中的预定义参数
                        resultString = resultString.replaceAll(this.addBackslashToRegExChar(
                                PDPTranslator.identificationString + targetParameter + PDPTranslator.identificationString, 1),
                                realValue);
                    }
                    catch (IllegalArgumentException e) {
                        return "【" + PDPTranslator.identificationString + targetParameter
                                + PDPTranslator.identificationString + "】不是预定义参数。";
                    }
                    catch (ClassNotFoundException e) {
                        return "未能找到预定义参数类型【" + predefinedParameterType + "】的解析器。";
                    }
                    catch (IllegalAccessException e) {
                        return "未能生成预定义参数类型的解析器【IllegalAccessException】。";
                    }
                    catch (InstantiationException e) {
                        return "未能生成预定义参数类型的解析器【InstantiationException】。";
                    }
                }
            }
            lastIndex = endIndex + 1;
        }
        return resultString;
    }

    /**
     * 将源字符串中的所有RegEx's meta character ".$|()[{^?*+\\" 的前面加上n个反斜杠'\'
     *
     * @param sourceStr 源字符串
     * @param n 反斜杠的个数,最多5个
     * @return: java.lang.String
     * @author: JQY
     * @date: 2019/5/31 13:37
     */
    private String addBackslashToRegExChar(String sourceStr, int n) {
        if (null == sourceStr) {
            return "";
        }
        if (0 >= n || n > 5) {
            return sourceStr;
        }
        char[] sourceChar = sourceStr.toCharArray();
        char[] targetChar = new char[sourceChar.length * (n + 1)];
        int i = 0, j = i;
        while (i < sourceChar.length) {
            if (".$|()[{^?*+\\".indexOf(sourceChar[i]) != -1) {
                for (int k = 0; k < n; ++k) {
                    targetChar[j++] = '\\';
                }
            }
            targetChar[j++] = sourceChar[i++];
        }
        return new String(targetChar, 0, j);
    }
}
