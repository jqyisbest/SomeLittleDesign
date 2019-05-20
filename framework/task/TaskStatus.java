/*
 * Copyright (c) 2019. JQY and/or its organisation. All rights reserved.
 */

package net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.task;

/**
 * 任务状态枚举常量
 *
 * @project:acr
 * @fully_qualified_name: net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.task.TaskStatus
 * @author: JQY
 * @create: 2019-05-06 17:28 Via IntelliJ IDEA
 **/
public enum TaskStatus {
    /**
     * RUNNING：正在执行，任务尚未完成。
     * FINISHED：未在执行，任务已完成。
     * EXIT：未在执行，任务未完成或存在错误。
     */
    RUNNING(Short.valueOf("-1")),FINISHED(Short.valueOf("0")),EXIT(Short.valueOf("1"));
    private Short value;

    TaskStatus(Short num) {
        this.value = num;
    }

    public Short toValue() {
        return value;
    }

    public static TaskStatus getTaskStatus(Short num){
        switch (num){
            case -1:{
                return RUNNING;
            }case 0: {
                return FINISHED;
            }case 1:{
                return EXIT;
            }
            default:{
                return null;
            }
        }
    }
}
