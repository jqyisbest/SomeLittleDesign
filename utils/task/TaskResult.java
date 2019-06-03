/*
 * Copyright (c) 2019. JQY and/or its organisation. All rights reserved.
 */

package net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.task;

/**
 * 任务执行结果
 *
 * @project:acr
 * @fully_qualified_name: net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.task.TaskResult
 * @author: JQY
 * @create: 2019-05-15 09:30 Via IntelliJ IDEA
 **/
public class TaskResult {
    private TaskStatus taskStatus;
    private Exception exception;

    public TaskResult(TaskStatus taskStatus,Exception e) {
        this.taskStatus = taskStatus;
        this.exception =e;
    }

    @Override
    public String toString() {
        return "TaskResult{" +
                "taskStatus=" + taskStatus.name() +
                ", exception=" + exception +
                '}';
    }

    public TaskStatus getTaskStatus() {
        return taskStatus;
    }

    public Exception getException(){
        return exception;
    }
}
