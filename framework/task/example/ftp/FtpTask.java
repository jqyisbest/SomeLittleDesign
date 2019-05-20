/*
 * Copyright (c) 2019. JQY and/or its organisation. All rights reserved.
 */

package net.gmcc.dg.acr.modules.reward.basedata.service.convert.ftp;

import net.gmcc.dg.acr.modules.reward.basedata.dao.FtpTaskDOMapper;
import net.gmcc.dg.acr.modules.reward.basedata.dao.entity.dborm.FtpTaskDO;
import net.gmcc.dg.acr.modules.reward.basedata.dao.entity.query.impl.FtpTaskQuery;
import net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.task.Task;
import net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.task.TaskResult;
import net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.task.TaskStatus;
import net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.task.TaskType;
import net.gmcc.dg.common.exception.BusinessRuntimeException;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * FTP层：负责获取基础数据文件，为SDS层准备所需要的基础数据文件。
 *
 * @project:acr
 * @fully_qualified_name: net.gmcc.dg.acr.modules.reward.basedata.service.convert.ftp.FtpTask
 * @author: JQY
 * @create: 2019-05-06 09:34 Via IntelliJ IDEA
 **/
@Component
public class FtpTask extends Task {

    @Autowired
    private FtpTaskDOMapper ftpTaskListDAO;

    /**
     * @param taskExecuteId 当前任务执行编号
     * @return void
     * @author JQY
     * @date 2019/5/6 9:46
     */
    private void doWorkSingleThread(final BigDecimal taskExecuteId) throws Exception {
        //获取任务表
        FtpTaskQuery queryParameter = new FtpTaskQuery();
        queryParameter.setActive(Short.valueOf("1"));
        queryParameter.setDeleted(Short.valueOf("0"));
        List<FtpTaskDO> toDoItemList = ftpTaskListDAO.listByDeletedAndActive(queryParameter);
        //遍历
        FtpTaskExecuter ftpTaskExecuter = null;
        for (int i = 0; i < toDoItemList.size(); i++) {
            FtpTaskDO subTask = toDoItemList.get(i);
            ftpTaskExecuter = new FtpTaskExecuter(subTask, taskExecuteId);
            ftpTaskExecuter.executeTask();
        }
        return;
    }

    /**
     * 以多线程方式开始工作
     * <p>
     * 建议：如果核心池已溢出，请采用单线程方式工作，以免因任务被拒绝而造成业务异常。
     *
     * @param taskExecuteId 当前任务执行编号
     * @return void
     * @author JQY
     * @date 2019/5/10 10:14
     */
    @Override

    public TaskResult startWork(final BigDecimal taskExecuteId) throws Exception {
        TaskResult taskResult= new TaskResult(TaskStatus.FINISHED, null);
        try {
            if (this.taskResource.getActiveCount() > this.taskResource.getCorePoolSize()) {
                //如果当前核心池已溢出，则转入单线程方式。
                this.doWorkSingleThread(taskExecuteId);
                return taskResult;
            }

            //获取任务表
            FtpTaskQuery queryParameter = new FtpTaskQuery();
            queryParameter.setActive(Short.valueOf("1"));
            queryParameter.setDeleted(Short.valueOf("0"));
            List<FtpTaskDO> toDoItemList = ftpTaskListDAO.listByDeletedAndActive(queryParameter);
            //遍历
            for (int i = 0; i < toDoItemList.size(); i++) {
                FtpTaskDO subTask = toDoItemList.get(i);
                this.taskResource.submitListenable(new FtpTaskExecuter(subTask, taskExecuteId));
            }
            do {
                //确保所有子任务线程都启动了
            }while (this.taskResource.getActiveCount()<toDoItemList.size());
            while (this.taskResource.getActiveCount()>0){
                //只要还有子任务没有做完就等着。
                Thread.sleep(3000);
            }
            return taskResult;
        } catch (RejectedExecutionException e) {
            throw new RejectedExecutionException("线程池拒绝执行任务，任务执行记录编号【" + taskExecuteId + "】。");
        } catch (Exception e) {
            throw e;
        } finally {

        }
    }

    /**
     * 获取当前任务的任务类别
     *
     * @return net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.task.TaskType
     * @author JQY
     * @date 2019/5/6 17:30
     */
    @Override
    public TaskType getTaskType() {
        return TaskType.FTP;
    }

    /**
     * 检查本任务在业务逻辑层面上是否可以执行
     *
     * @return boolean
     * @author JQY
     * @date 2019/5/7 10:43
     */
    @Override
    public boolean checkVocationalExecutePermission() {
        return true;
    }

    /**
     * 获取互斥任务类型
     *
     * @return java.util.List<net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.task.TaskType>
     * @author JQY
     * @date 2019/5/7 11:00
     */
    @Override
    public Set<TaskType> listExclusiveTaskType() {
        return null;
    }

    /**
     * 本任务是否需要采用多线程方式执行
     *
     * @return boolean true：是；false：否
     * @author JQY
     * @date 2019/5/10 14:36
     */
    @Override
    public boolean isMultiThreadExecute() {
        return true;
    }

}
