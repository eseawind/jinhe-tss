package com.jinhe.tss.framework.component.timer;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.jinhe.tss.framework.Global;
import com.jinhe.tss.framework.component.log.IBusinessLogger;
import com.jinhe.tss.framework.component.log.Log;

public abstract class AbstractJob implements Job {
	
	protected Logger log = Logger.getLogger(this.getClass());
	
	IBusinessLogger businessLogger = (IBusinessLogger) Global.getBean("BusinessLogger");
	
    public void execute(JobExecutionContext context) throws JobExecutionException {
    	JobDetail jobDetail = context.getJobDetail();
    	String jobName = jobDetail.getName();
    	
    	JobDataMap dataMap = jobDetail.getJobDataMap();
        
        log.info("定时任务：（" + jobName + "）开始执行。");
        String resultMsg;
        
        Log excuteLog = null;
        
        try {
        	Long preTime = System.currentTimeMillis();
        	
        	excuteJob((String) dataMap.get(jobName));
        	
        	int methodExcuteTime = (int) (System.currentTimeMillis() - preTime);
        	
        	resultMsg = "定时任务：（" + jobName + "）执行完成。";
        	log.info(resultMsg);
        	
        	excuteLog = new Log(jobName + "-成功", resultMsg);
        	excuteLog.setMethodExcuteTime(methodExcuteTime);
        } 
        catch(Exception e) {
        	resultMsg = "定时任务：（" + jobName + "）执行出错了: " + e.getMessage();
        	log.error(resultMsg, e);
        	
        	excuteLog = new Log(jobName + "-失败", resultMsg);
        } 
        finally {
        	if(excuteLog != null) {
        		excuteLog.setOperateTable("定时任务");
                businessLogger.output(excuteLog);
        	}
        }
    }
    
    protected abstract void excuteJob(String jobConfig);
}
