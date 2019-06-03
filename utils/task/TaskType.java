/*
 * Copyright (c) 2019. JQY and/or its organisation. All rights reserved.
 */

package net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.task;

/**
 * 任务类别枚举常量
 *
 * @project:acr
 * @fully_qualified_name: net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.task.TaskType
 * @author: JQY
 * @create: 2019-05-06 17:25 Via IntelliJ IDEA
 **/
public enum TaskType {
    /**
     * FTP：FTP任务。
     * SDS：SDS任务。
     * MDS：MDS任务。
     */
    FTP(Short.valueOf("1")),SDS(Short.valueOf("2")),MDS(Short.valueOf("3"));
    private Short value;

    TaskType(Short num) {
        this.value = num;
    }

    public Short toValue() {
        return value;
    }

    public static TaskType getTaskType(Short num){
        switch (num){
            case 1:{
                return FTP;
            }case 2: {
                return SDS;
            }case 3:{
                return MDS;
            }
            default:{
                return null;
            }
        }
    }
}
