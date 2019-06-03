/*
 * Copyright (c) 2019. JQY and/or its organisation. All rights reserved.
 */

package net.gmcc.dg.acr.modules.reward.basedata.service.convert.ftp;

import net.gmcc.dg.acr.modules.reward.basedata.dao.FtpRecordDOMapper;
import net.gmcc.dg.acr.modules.reward.basedata.dao.entity.dborm.FtpRecordDO;
import net.gmcc.dg.acr.modules.reward.basedata.dao.entity.dborm.FtpTaskDO;
import net.gmcc.dg.acr.modules.reward.basedata.service.convert.helper.ftp.FtpHelper;
import net.gmcc.dg.common.exception.BusinessRuntimeException;
import net.gmcc.dg.common.utils.CommonFunctions;
import net.gmcc.dg.common.utils.DateUtil;
import net.gmcc.dg.common.utils.FileDigestUtil;
import net.gmcc.dg.common.utils.SpringUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * FTP子任务的实际执行者
 *
 * @project:acr
 * @fully_qualified_name: net.gmcc.dg.acr.modules.reward.basedata.service.convert.ftp.FtpTaskExecuter
 * @author: JQY
 * @create: 2019-05-09 10:26 Via IntelliJ IDEA
 **/
public class FtpTaskExecuter implements Callable {

    private BigDecimal ftpTaskId;

    private String hostName;

    private int port;

    private String userName;

    private String account;

    private String password;

    private BigDecimal taskExecuteId;

    private String savePath;

    private List<String> fileList =new ArrayList<>();

    private FtpHelper ftpHelper =null;

    private FtpRecordDOMapper ftpRecordDAO=SpringUtils.getBean(FtpRecordDOMapper.class);

    /**
     * 目标文件列表的分割符
     */
    private String partitionSymbol;
    /**
     * 目标文件列表的分割符
     */
    private String actualSavePath;

    /**
     * 所使用的摘要算法
     */
    private String digestAlgorithm;

    /**
     * 因为不适用spring注解而手工获取的spring变量
     */
    private  Environment springEnv = SpringUtils.getBean(StandardEnvironment.class);

    private DataSourceTransactionManager transactionManager=SpringUtils.getBean(DataSourceTransactionManager.class);

    private static final Logger logger = LoggerFactory.getLogger(FtpTaskExecuter.class);

    private String errorMessagePrefix ="FTP子任务执行失败。";

    private String logPrefix="\r\nFTP子任务\r\n";

    public FtpTaskExecuter(FtpTaskDO subTask,BigDecimal taskExecuteId) {
        if(null!=subTask){
            //读取配置文件
            this.partitionSymbol=springEnv.getProperty("basedata.ftp.partitionSymbol");
            this.actualSavePath=springEnv.getProperty("basedata.ftp.realPath");
            this.digestAlgorithm=springEnv.getProperty("basedata.digestAlgorithm");
            //初始化任务
            this.taskExecuteId=taskExecuteId;
            this.ftpTaskId=subTask.getId();
            this.hostName = subTask.getFtpIp();
            this.port = Integer.parseInt(subTask.getFtpPort());
            this.userName = subTask.getFtpUsername();
            this.account = subTask.getFtpAccount();
            this.password = subTask.getFtpPassword();
            String targetFileList=subTask.getTargetFileList();
            if(null!=targetFileList&&!"".equals(targetFileList)){
                if(targetFileList.contains(partitionSymbol)){
                    String[] fileArray=subTask.getTargetFileList().split(partitionSymbol);
                    for (int i = 0; i < fileArray.length; i++) {
                        if(!"".equals( fileArray[i].trim())){
                            this.fileList.add(fileArray[i]);
                        }
                    }
                }else {
                    this.fileList.add(targetFileList);
                }
            }
            this.savePath=subTask.getSavePath()==null?"":subTask.getSavePath().trim();
            this.logPrefix="\r\n执行编号【" + this.taskExecuteId + "】\r\nFTP子任务编号【"+this.ftpTaskId+"】\r\n任务详情：从【"+this.hostName+":"+this.port+"】获取【"+subTask.getTargetFileList()+"】\r\n";
            this.errorMessagePrefix=this.logPrefix+"FTP子任务执行失败。\r\n";
        }
    }

    @Override
    public String toString() {
        return "FtpTaskExecuter{" +
                "ftpTaskId=" + ftpTaskId +
                ", hostName='" + hostName + '\'' +
                ", port=" + port +
                ", userName='" + userName + '\'' +
                ", account='" + account + '\'' +
                ", password='" + password + '\'' +
                ", taskExecuteId=" + taskExecuteId +
                ", savePath='" + savePath + '\'' +
                ", fileList=" + fileList +
                ", ftpHelper=" + ftpHelper +
                ", ftpRecordDAO=" + ftpRecordDAO +
                ", partitionSymbol='" + partitionSymbol + '\'' +
                ", actualSavePath='" + actualSavePath + '\'' +
                ", digestAlgorithm='" + digestAlgorithm + '\'' +
                ", errorMessagePrefix='" + errorMessagePrefix + '\'' +
                '}';
    }
    /**
     *
     * @param
     * @return void
     * @author JQY
     * @date 2019/5/13 15:37
     */
    public void executeTask(){
        ftpHelper=FtpHelper.connectFtp(this.hostName,null,this.port,this.userName,this.password,this.account);
        Date now=new Date();
        final String dateTimePattern = "yyyyMMdd";
        String currentFile="";
//        //开启事务
//        DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
//            // 事物隔离级别，开启新事务
//        transactionDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
//            // 获得事务状态
//        TransactionStatus transactionStatus = null;
        try {
//            transactionStatus=this.transactionManager.getTransaction(transactionDefinition);
            if(null==ftpHelper){
                logger.error(errorMessagePrefix +"原因：未能与FTP服务器【"+hostName+":"+port+"】建立连接。\r\n");
                return;
            }
            for (int i = 0; i < fileList.size(); i++) {
                currentFile=fileList.get(i).trim();
                String currentFileName=currentFile.substring(0,currentFile.lastIndexOf("."));
                String currentFileType=currentFile.substring(currentFile.lastIndexOf("."));
                //实际存储名=文件名+年月日+唯一序列号
                String actualSaveName=currentFileName.concat(DateUtil.DateToStringByFormat(now,dateTimePattern)).concat(CommonFunctions.generateUniqueNumber()).concat(currentFileType);
                boolean saveFileFlag=ftpHelper.downloadFile(this.savePath.concat(currentFile),actualSavePath.concat(actualSaveName));
                if(saveFileFlag){
                    File savedFile=new File(actualSavePath,actualSaveName);
                    if(savedFile.exists()&&savedFile.isFile()&&savedFile.canRead()){
                        /**
                         * 刚保存的文件的哈希值
                         */
                        String hashValue= FileDigestUtil.getFileDigest(this.digestAlgorithm,savedFile,logger);
                        if(null!=ftpRecordDAO.getByHashValue(hashValue)){
                            logger.info(this.logPrefix+"文件【"+currentFile+"】（哈希值【"+hashValue+"】）未被修改，不予更新。\r\n");
                            if(!savedFile.delete()){
                                //不予更新但文件删除失败，应通知管理员
                                logger.info(this.logPrefix+"文件【"+actualSavePath+actualSaveName+"】删除失败，请手工删除。\r\n");
                            }
                            continue;
                        }
                        //添加FTP获取记录
                        FtpRecordDO saveRecord=new FtpRecordDO();
                        saveRecord.setFileHash(hashValue);
                        saveRecord.setFileName(currentFileName);
                        saveRecord.setFileType(currentFileType);
                        saveRecord.setFtpTaskId(this.ftpTaskId);
                        saveRecord.setRecordTime(now);
                        saveRecord.setSaveName(actualSaveName);
                        saveRecord.setSourceIp(this.hostName.substring(0,this.hostName.length()>15?15:this.hostName.length()));
                        saveRecord.setSourcePath(this.savePath);
                        saveRecord.setTaskExeRecordId(this.taskExecuteId);
                        int daoResult=this.ftpRecordDAO.insertSelective(saveRecord);
                        if(1!=daoResult){
                            throw new BusinessRuntimeException("原因：FTP记录插入异常，更新了【"+daoResult+"】条记录。\r\n");
                        }
                    }else {
                        logger.warn(this.logPrefix+"服务器文件【"+actualSavePath+actualSaveName+"】无法读取，不予更新。\r\n");
                        if(!savedFile.delete()){
                            //不予更新但文件删除失败，应通知管理员
                            logger.info(this.logPrefix+"文件【"+actualSavePath+actualSaveName+"】删除失败，请手工删除。\r\n");
                        }
                    }
                }else {
                    logger.warn(this.logPrefix+"文件【"+currentFile+"】未能成功保存，任务执行编号【"+this.taskExecuteId+"】。\r\n");
                }
            }
        } catch (BusinessRuntimeException e){
            //数据库操作异常
            logger.error(errorMessagePrefix +e.getMsg());
//            this.transactionManager.rollback(transactionStatus);
            throw e;
        }catch (IOException e) {
            logger.error(errorMessagePrefix +"原因：从FTP服务器【"+hostName+":"+port+"】下载文件【" +currentFile+
                    "】时发生IO异常。\r\n",e);
        }catch (Exception e){
            logger.error(errorMessagePrefix,e);
        }finally {
//            this.transactionManager.commit(transactionStatus);
            if(null!=ftpHelper){
                ftpHelper.closeServer();
            }
        }
    }


    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    @Override
    public Object call() throws Exception {
        logger.info("\r\n执行编号【" + this.taskExecuteId + "】\r\n【"+this.ftpTaskId+"】号FTP子任务开始执行。\r\n");
        this.executeTask();
        logger.info("\r\n执行编号【" + this.taskExecuteId + "】\r\n【"+this.ftpTaskId+"】号FTP子任务执行结束。\r\n");
        return null;
    }
}
