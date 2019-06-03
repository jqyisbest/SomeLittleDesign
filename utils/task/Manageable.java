/*
 * Copyright (c) 2019. JQY and/or its organisation. All rights reserved.
 */

package net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.task;

import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.SuccessCallback;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 所有试图通过任务控制中心执行的任务都必须实现这个接口
 *
 * @project:acr
 * @fully_qualified_name: net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.task.Manageable
 * @author: JQY
 * @create: 2019-05-06 14:55 Via IntelliJ IDEA
 **/
public interface Manageable extends Callable<TaskResult> {

    /**
     * 获取当前任务的任务类别
     * @param 
     * @return net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.task.TaskType
     * @author JQY
     * @date 2019/5/6 17:30
     */
    TaskType getTaskType();

    /**
     * 检查本任务在业务逻辑层面上是否可以执行
     * @param
     * @return boolean
     * @author JQY
     * @date 2019/5/7 10:43
     */
    boolean checkVocationalExecutePermission();

    /**
     * 获取互斥任务类型
     * @param
     * @return java.util.List<net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.task.TaskType>
     * @author JQY
     * @date 2019/5/7 11:00
     */
    Set<TaskType> listExclusiveTaskType();

    /**
     * 本任务是否需要采用多线程方式执行
     * @param
     * @return boolean true：是；false：否
     * @author JQY
     * @date 2019/5/10 14:36
     */
    boolean isMultiThreadExecute();

}
