/*
 * Copyright (c) 2019. JQY and/or its organisation. All rights reserved.
 */

package net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;

import java.math.BigDecimal;
import java.util.concurrent.Callable;

/**
 * 任务执行所需要的资源
 *
 * @project:acr
 * @fully_qualified_name: net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.task.TaskResource
 * @author: JQY
 * @create: 2019-05-14 10:07 Via IntelliJ IDEA
 **/
public class TaskResource {
    private BigDecimal taskExecuteId;
    private TaskCallBack callBack;
    /**
     * spring
     */
    private ThreadPoolTaskExecutor threadPoolTaskExecutor =null;
    private Logger logger=null;

    public TaskResource(BigDecimal taskExecuteId, ThreadPoolTaskExecutor threadPoolExecutor, Class loggerClass) {
        this.taskExecuteId = taskExecuteId;
        this.threadPoolTaskExecutor = threadPoolExecutor;
        this.logger = LoggerFactory.getLogger(loggerClass);
    }

    public BigDecimal getTaskExecuteId() {
        return taskExecuteId;
    }

    public Logger getLogger() {

        return this.logger;
    }

    public int getCorePoolSize(){
        return this.threadPoolTaskExecutor.getCorePoolSize();
    }

    public int getActiveCount(){
        return this.threadPoolTaskExecutor.getActiveCount();
    }

    public ListenableFuture submitListenable(Callable subTask) throws Exception{
        return this.threadPoolTaskExecutor.submitListenable(subTask);
    }
}
