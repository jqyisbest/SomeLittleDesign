/*
 * Copyright (c) 2019. JQY and/or its organisation. All rights reserved.
 */

package net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.task;

import EDU.oswego.cs.dl.util.concurrent.Takable;
import net.gmcc.dg.acr.modules.reward.basedata.dao.TaskExecuteRecordDOMapper;
import net.gmcc.dg.acr.modules.reward.basedata.dao.entity.dborm.TaskExecuteRecordDO;
import net.gmcc.dg.common.utils.SpringUtils;
import org.apache.poi.ss.formula.functions.T;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 任务执行后的回调
 *
 * @project:acr
 * @fully_qualified_name: net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.task.TaskCallBack
 * @author: JQY
 * @create: 2019-05-15 08:29 Via IntelliJ IDEA
 **/
public class TaskCallBack implements ListenableFutureCallback<TaskResult> {

    private static final Logger logger=LoggerFactory.getLogger(TaskCallBack.class);

    private BigDecimal taskExecuteId;

    /**
     * 任务执行记录DAO
     */
    private TaskExecuteRecordDOMapper taskExecuteRecordDAO= SpringUtils.getBean(TaskExecuteRecordDOMapper.class);

    public TaskCallBack(BigDecimal taskExecuteId) {
        this.taskExecuteId = taskExecuteId;
    }

    @Override
    public void onFailure(Throwable throwable) {
        TaskResult taskResult=new TaskResult(TaskStatus.EXIT,null);
        this.writeTaksExecuteRecord(taskResult);
    }

    @Override
    public void onSuccess(TaskResult taskResult) {
        this.writeTaksExecuteRecord(taskResult);
    }

    /**
     * 任务结束以后记录任务的完成情况
     * @param taskResult 执行结果
     * @return boolean
     * @author JQY
     * @date 2019/5/15 11:12
     */
    private boolean writeTaksExecuteRecord(TaskResult taskResult){
        try {
            TaskExecuteRecordDO executeRecord=taskExecuteRecordDAO.selectByPrimaryKey(taskExecuteId);
            if(null==executeRecord){
                throw new NullPointerException("不存在执行编号为【"+taskExecuteId+"】的任务执行记录。");
            }
            if(executeRecord.getStatus().equals(TaskStatus.RUNNING.toValue())){
                Date now=new Date();
                executeRecord.setStatus(taskResult.getTaskStatus().toValue());
                executeRecord.setEndTime(now);
                executeRecord.setRecordTime(now);
                int daoResult=this.taskExecuteRecordDAO.updateByPrimaryKeySelective(executeRecord);
                if(1!=daoResult){
                    throw new Exception("执行记录更新失败，影响行数【"+daoResult+"】，未回滚。");
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("执行编号为【"+taskExecuteId+"】的任务更新执行记录失败,任务执行结果【"+taskResult.toString()+"】。",e);
            return false;
        } finally {

        }
    }
}
