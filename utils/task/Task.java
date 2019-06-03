/*
 * Copyright (c) 2019. JQY and/or its organisation. All rights reserved.
 */

package net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.task;

import org.slf4j.Logger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 任务中心可以管控的任务
 *
 * @project:acr
 * @fully_qualified_name: net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.task.Task
 * @author: JQY
 * @create: 2019-05-10 15:53 Via IntelliJ IDEA
 **/
public abstract class Task implements Manageable {

    /**
     * 任务执行所需要的基础资源
     */
    protected TaskResource taskResource = null;

    public void setTaskResource(TaskResource taskResource) {
        this.taskResource = taskResource;
    }

    /**
     * 开始工作
     * <p>
     * 建议：如果核心池已溢出，请采用单线程方式工作，以免因任务被拒绝而造成业务异常。
     *
     * @param taskExecuteId
     * @return net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.task.TaskStatus 任务执行结果 （是否有错误）
     * @author JQY
     * @date 2019/5/15 10:57
     */
    protected abstract TaskResult startWork(final BigDecimal taskExecuteId) throws Exception;

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public TaskResult call() {
        TaskResult taskResult = null;
        try {
            if (this.taskResource == null || this.taskResource.getTaskExecuteId() == null) {
                throw new PermissionException("taskExecuteId为空。");
            }
            if (this.taskResource.getLogger() != null) {
                this.taskResource.getLogger().info("\r\n执行编号【" + this.taskResource.getTaskExecuteId() + "】\r\n【" + this.getTaskType().name() + "】任务开始执行。\r\n");
            }
            taskResult = this.startWork(this.taskResource.getTaskExecuteId());
            if (this.taskResource.getLogger() != null) {
                this.taskResource.getLogger().info("\r\n执行编号【" + this.taskResource.getTaskExecuteId() + "】\r\n【" + this.getTaskType().name() + "】任务执行结束。\r\n");
            }
        } catch (PermissionException e) {
            if (this.taskResource.getLogger() != null) {
                this.taskResource.getLogger().error("\r\n【" + this.getTaskType().name() + "】任务未能获得执行许可！\r\n", e);
            }
            taskResult = new TaskResult(TaskStatus.EXIT, e);
        } catch (RejectedExecutionException e) {
            if (this.taskResource.getLogger() != null) {
                this.taskResource.getLogger().error("\r\n执行编号【" + this.taskResource.getTaskExecuteId() + "】\r\n【" + this.getTaskType().name() + "】任务尝试多线程执行任务失败！\r\n", e);
            }
            taskResult = new TaskResult(TaskStatus.EXIT, e);
        } catch (Exception e) {
            if (this.taskResource.getLogger() != null) {
                this.taskResource.getLogger().error("\r\n执行编号【" + this.taskResource.getTaskExecuteId() + "】\r\n【" + this.getTaskType().name() + "】任务执行失败！\r\n", e);
            }
            taskResult = new TaskResult(TaskStatus.EXIT, e);
        } finally {
            return taskResult;
        }
    }
}
