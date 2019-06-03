/*
 * Copyright (c) 2019. JQY and/or its organisation. All rights reserved.
 */

package net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.pdp;

import java.util.Calendar;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import net.gmcc.dg.common.utils.DateUtil;

/**
 * 日期时间参数解析器
 *
 * @project:acr
 * @fully_qualified_name: net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.pdp.DateTimeTranslator
 * @author: JQY
 * @create: 2019/05/31 9:31 Via IntelliJ IDEA
 **/
public class DateTimeTranslator extends BaseTranslator {
    /**
     * 日期时间的默认格式为 yyyyMMdd
     */
    private static final String DEFAULT_FORMAT = "yyyyMMdd";
    private Calendar calendar = Calendar.getInstance();

    /**
     *  根据离今天的偏移量计算所需日期
     *
     * @param paramJSON json格式的参数字符串 年偏移量、月偏移量、日偏移量、小时偏移量、分钟偏移量
     * @return: java.lang.String
     * @author: JQY
     * @date: 2019/6/3 14:55
     */
    private String getDate(String paramJSON) {
        int yearDeviation,monthDeviation,dayDeviation,hourDeviation,minuteDeviation=0;
        String format="";
        try{
            JSONObject param=JSON.parseObject(paramJSON);
            JSONArray deviationArray=JSON.parseArray(param.getString("d"));
            format=param.getString("s").trim();
            yearDeviation=deviationArray.getIntValue(0);
            monthDeviation=deviationArray.getIntValue(1);
            dayDeviation=deviationArray.getIntValue(2);
            hourDeviation=deviationArray.getIntValue(3);
            minuteDeviation=deviationArray.getIntValue(4);
            calendar.add(Calendar.YEAR,yearDeviation);
            calendar.add(Calendar.MONTH,monthDeviation);
            calendar.add(Calendar.DAY_OF_MONTH,dayDeviation);
            calendar.add(Calendar.HOUR_OF_DAY,hourDeviation);
            calendar.add(Calendar.MINUTE,minuteDeviation);
            return DateUtil.DateToStringByFormat(calendar.getTime(), format);
        }catch(Exception e){
            return "参数[" +paramJSON+ "]不合法导致出现异常,合法格式为：{'d':'[年份偏移量(当前请填0，后同),月份偏移量,日期偏移量,小时偏移量(24小时制,与日期偏移量的效果会叠加),分钟偏移量]','s':'形如yyyyMMdd的日期时间格式'}";
        }
    }

    /**
     * 返回当前日期时间
     * 
     * @param format 格式
     * @return: java.lang.String
     * @author: JQY
     * @date: 2019/5/31 10:18
     */
    private String getToday(String format) {
        format = format == null ? DEFAULT_FORMAT : "".equals(format.trim()) ? DEFAULT_FORMAT : format;
        return DateUtil.DateToStringByFormat(calendar.getTime(), format);
    }

}
