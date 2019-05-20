/*
 * Copyright (c) 2019. JQY and/or its organisation. All rights reserved.
 */

package net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.task;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.gmcc.dg.acr.modules.reward.basedata.dao.TaskExecuteRecordDOMapper;
import net.gmcc.dg.acr.modules.reward.basedata.dao.entity.dborm.TaskExecuteRecordDO;
import net.gmcc.dg.common.utils.SpringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.ListenableFutureTask;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 任务控制中心，负责管理并记录每个任务的执行情况
 *
 * 默认以多线程方式启动任务
 *
 * @project:acr
 * @fully_qualified_name: net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.task.TaskCenter
 * @author: JQY
 * @create: 2019-05-06 14:32 Via IntelliJ IDEA
 **/
public class TaskCenter {

    private final String logName="存储在系统日志中";
    private static final Logger logger = LoggerFactory.getLogger(TaskCenter.class);
    private Lock lock = new ReentrantLock();
    /**
     * 是否在任务中心所在的线程上执行任务。
     */
    private boolean executeInMainThread=false;
    /**
     * 核心线程池
     */
    private ThreadPoolTaskExecutor mainThreadPool = new ThreadPoolTaskExecutor();
    /**
     * 所有任务的子任务共用一个线程池。
     */
    private ThreadPoolTaskExecutor subTaskThreadPool = new ThreadPoolTaskExecutor();

    /**
     * 任务执行记录DAO
     */
    private TaskExecuteRecordDOMapper taskExecuteRecordDAO=SpringUtils.getBean(TaskExecuteRecordDOMapper.class);

    public TaskCenter(){
        //此处可修改为读取配置文件中的线程池设置
        ThreadFactory mainThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("TaskCenter-主任务线程-%d").build();
        this.mainThreadPool.setCorePoolSize(0);
        this.mainThreadPool.setMaxPoolSize(Integer.MAX_VALUE);
        this.mainThreadPool.setKeepAliveSeconds(10);
        this.mainThreadPool.setQueueCapacity(-1);
        this.mainThreadPool.setThreadFactory(mainThreadFactory);
        this.mainThreadPool.initialize();
        ThreadFactory subTaskThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("TaskCenter-子任务线程-%d").build();
        this.subTaskThreadPool.setCorePoolSize(5);
        this.subTaskThreadPool.setMaxPoolSize(20);
        this.subTaskThreadPool.setKeepAliveSeconds(5);
        this.subTaskThreadPool.setQueueCapacity(10);
        this.subTaskThreadPool.setThreadFactory(subTaskThreadFactory);
        this.subTaskThreadPool.initialize();
    }
    public TaskCenter(ThreadPoolTaskExecutor mainThreadPool,ThreadPoolTaskExecutor subTaskThreadPool){
        this.mainThreadPool=mainThreadPool;
        this.subTaskThreadPool =subTaskThreadPool;
    }

    /**
     * 启动任务
     *
     * @param task 要执行的任务
     * @return void
     * @author JQY
     * @date 2019/5/6 17:28
     */
    public void startTask(@NotNull Task task) throws Exception {
        try {
            BigDecimal taskExecuteId = this.getExecutePermission(task);
            if (taskExecuteId.compareTo(BigDecimal.valueOf(0)) == 1) {
                //获得执行许可。
                TaskResource taskResource=null;
                if(task.isMultiThreadExecute()){
                    taskResource=new TaskResource(taskExecuteId,this.subTaskThreadPool,task.getClass());
                }else {
                    taskResource=new TaskResource(taskExecuteId,null,task.getClass());
                }
                task.setTaskResource(taskResource);
                if(this.executeInMainThread){
                    this.getListenableCallback(taskExecuteId).onSuccess(task.call());
                }else{
                    ListenableFutureTask<TaskResult> listenableFutureTask = new ListenableFutureTask<>(task);
                    listenableFutureTask.addCallback(this.getListenableCallback(taskExecuteId));
                    this.mainThreadPool.submit(listenableFutureTask);
                    int activeCount=0;
                    do {
                        activeCount=this.mainThreadPool.getActiveCount();
                    }while (activeCount<=0);
                    while (activeCount>0){
                        //只要还有任务没做完就等着。
                        Thread.sleep(3000);
                        activeCount=this.mainThreadPool.getActiveCount();
                    }
                }
            }
        }catch (PermissionException e){
            logger.error("\r\n任务未能获得执行许可！TaskType:【"+task.getTaskType().name()+"】",e);
        } catch (RejectedExecutionException e){
            logger.error("\r\n尝试多线程执行任务失败！TaskType:【"+task.getTaskType().name()+"】",e);
        }catch (Exception e){
            logger.error("\r\n任务执行失败！TaskType:【"+task.getTaskType().name()+"】",e);
            throw e;
        }finally {
        }
    }

    /**
     * 允许在控制中心所在的线程上执行任务
     *
     * 调用此方法后的下一个任务将在主线程上执行
     * @param
     * @return void
     * @author JQY
     * @date 2019/5/10 16:34
     */
    public void allowExecuteTaskInMainThread(){
        if(!this.executeInMainThread){
            this.executeInMainThread=true;
        }
    }

    /**
     * 禁止在控制中心所在的线程上执行任务
     * 调用此方法后的下一个任务将在子线程上执行
     * @param
     * @return void
     * @author JQY
     * @date 2019/5/10 16:34
     */
    public void forbidExecuteTaskInMainThread(){
        if(this.executeInMainThread){
            this.executeInMainThread=false;
        }
    }

    /**
     * 查找是否有正在执行的任务，可以选择通过currentTask对象获取当前任务的相关信息（执行编号、任务名称、开始时间、日志名）
     *
     * @param taskType    任务类型
     * @param currentTask 当前正在执行的任务，可空
     * @return java.lang.Boolean
     * @author JQY
     * @date 2019/5/6 11:44
     */
    public Boolean isRunning(TaskType taskType, TaskExecuteRecordDO currentTask) {
        TaskExecuteRecordDO latestTaskExecuteRecord = taskExecuteRecordDAO.getLatestTaskExecuteRecord(taskType.toValue());
        if (null!=latestTaskExecuteRecord&&latestTaskExecuteRecord.getStatus().equals(TaskStatus.RUNNING.toValue())) {
            if(null!=currentTask){
                TaskExecuteRecordDO.copy(latestTaskExecuteRecord,currentTask);
            }
            return true;
        }
        return false;
    }

    /**
     * 申请执行任务的唯一途径
     * 若许可，则会返回执行编号，若不允许执行，则返回-1
     * <p>
     * 会申请锁来保持同步
     * <p>
     * 如果要应对高并发场景，请修改逻辑以减小持有锁的时间
     *
     * @param task 要执行的任务
     * @return java.math.BigDecimal
     * @author JQY
     * @date 2019/5/6 14:50
     */
    private BigDecimal getExecutePermission(@NotNull Task task) throws Exception {
        Short times = 5;
        boolean getLock = false;
        TaskType taskType=task.getTaskType();
        final String errorMsg=taskType.name()+"任务未能获得许可。";
        final String exceptionPrefix=errorMsg+"原因：";
        for (int i = 0; i < times; ++i) {
            if (lock.tryLock(3, TimeUnit.SECONDS)) {
                getLock = true;
                break;
            }
        }

        if (getLock) {
            try {
                TaskExecuteRecordDO currentTask = new TaskExecuteRecordDO();
                Set<TaskType> exclusiveTaskTypeSet=EnumSet.noneOf(TaskType.class);
                if(null!=task.listExclusiveTaskType()){
                    exclusiveTaskTypeSet.addAll(task.listExclusiveTaskType());
                }
                if(!exclusiveTaskTypeSet.contains(taskType)){
                    exclusiveTaskTypeSet.add(taskType);
                }
                Iterator<TaskType> item=exclusiveTaskTypeSet.iterator();
                //检查互斥逻辑许可，默认检查和自己的互斥
                while (item.hasNext()){
                    if (this.isRunning(item.next(), currentTask)) {
                        throw new PermissionException("存在正在执行的"+taskType+"任务，执行ID为：【"+currentTask.getId()+"】。");
                    }
                }
                //检查业务逻辑许可
                if(!task.checkVocationalExecutePermission()){
                    throw new PermissionException(taskType+"任务未能获取业务逻辑许可。");
                }

                //申请新任务
                Date now=new Date();
                TaskExecuteRecordDO taskExecuteRecord=new TaskExecuteRecordDO();
                taskExecuteRecord.setName(taskType.name()+"任务");
                taskExecuteRecord.setTaskType(taskType.toValue());
                //todo:获取定时任务详情ID
                taskExecuteRecord.setTaskInfoId(BigDecimal.ZERO);
                taskExecuteRecord.setStatus(TaskStatus.RUNNING.toValue());
                taskExecuteRecord.setStratTime(now);
                taskExecuteRecord.setRecordTime(now);
                taskExecuteRecord.setLogName(logName);
                int result=taskExecuteRecordDAO.insertSelective(taskExecuteRecord);
                if(1==result){
                    taskExecuteRecord=taskExecuteRecordDAO.getLatestTaskExecuteRecord(taskType.toValue());
                    return null==taskExecuteRecord?BigDecimal.valueOf(-1):taskExecuteRecord.getId();
                }else {
                    throw new PermissionException("执行记录写入失败，本次写入："+result+"条记录。");
                }

            } catch (Exception e) {
                throw new PermissionException(exceptionPrefix+e.getMessage(),e);
            } finally {
                lock.unlock();
            }
        } else {
            throw new PermissionException(exceptionPrefix+"有其它任务正在申请执行许可。");
        }
    }

    /**
     * 获取任务回调对象
     * @param taskExecuteId 执行记录编号
     * @return org.springframework.util.concurrent.ListenableFutureCallback<net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.task.TaskResult>
     * @author JQY
     * @date 2019/5/15 12:04
     */
    private ListenableFutureCallback<TaskResult> getListenableCallback(BigDecimal taskExecuteId){
        return new TaskCallBack(taskExecuteId) ;
    }
}
