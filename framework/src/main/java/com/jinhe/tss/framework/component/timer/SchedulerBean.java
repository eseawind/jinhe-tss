package com.jinhe.tss.framework.component.timer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.stereotype.Component;

import com.jinhe.tss.framework.Config;
import com.jinhe.tss.framework.component.param.Param;
import com.jinhe.tss.framework.component.param.ParamConstants;
import com.jinhe.tss.framework.component.param.ParamManager;
import com.jinhe.tss.framework.exception.BusinessException;
import com.jinhe.tss.util.BeanUtil;
import com.jinhe.tss.util.EasyUtils;

/** 
 * 定时器调度。
 * 
 * 新增或删除一个job失败,不影响其它job的生成和删除。
 * 
 */
@Component
public class SchedulerBean {
    
	protected Logger log = Logger.getLogger(this.getClass());
	
	public static final String TIMER_PARAM_CODE = "TIMER_PARAM_CODE";
	
	static long initCyclelife = 1000 * 180; // 3分钟
	
    private static Scheduler scheduler;
    
    private static Map<String, String> configsMap;
    
    public SchedulerBean() {
    	this(initCyclelife);
    }
 
    public SchedulerBean(final long initCyclelife) {
    	// 根据配置决定是否启用定时Job
    	if( !Config.TRUE.equals(Config.getAttribute(Config.ENABLE_JOB)) ) {
    		return;
    	}
    	
    	log.info("SchedulerBean is starting....." + initCyclelife);
    	
    	try {
			scheduler = StdSchedulerFactory.getDefaultScheduler();
			scheduler.start();
		} catch (SchedulerException e) {
            throw new BusinessException("初始化定时策略出错!", e);
        } 
    	
    	configsMap = new HashMap<String, String>();
        
        new Thread() {
        	public void run() {
	            try {
	                while (true) {
                    	/* 定时检查，如果定时配置有改动，则刷新定时器 （第一次启动时，还需要等其他IOC池里的Bean就位（因依赖ParamManager））*/
                        sleep(initCyclelife); 
                        
                        init();
	                }
	            } catch (InterruptedException e) {
	            	throw new BusinessException("初始化SchedulerBean时出错！", e);
	            }
            }
        }.start(); 
    }
    
    public void init() {
        List<Param> list = null;
        try {
        	list = ParamManager.getComboParam(TIMER_PARAM_CODE);
        } catch(Exception e) {
        	log.error("定时任务配置有误", e);
        }
        if( EasyUtils.isNullOrEmpty(list) ) {
        	return;
        }
        
        log.debug("SchedulerBean init begin...");
        
        List<String> jobCodes = new ArrayList<String>();
		for(Param param : list) {
			if(ParamConstants.TRUE.equals(param.getDisabled())) {
				continue; // 停用的定时配置不要
			}
			
			String code  = param.getText();
			String value = param.getValue();
			
			String jobName = "Job-" + code;
			jobCodes.add(code);
			
			if( value.equals(configsMap.get(code)) ) {
				continue; // 如果已经生成且没做过修过，则不变
			} else if(configsMap.containsKey(code)) {
				deleteJob(code); // 如果已经存在，且value发生了变化，则先删除旧的Job，重新生成新的Job
			}
			
			// 新增或修过过的定时配置，需要重新生成定时Job
			String configs[] = EasyUtils.split(value, "|"); // jobClassName | timeDescr | customizeConfig
			Class<?> jobClazz = BeanUtil.createClassByName(configs[0].trim());
			JobDetail jobDetail = new JobDetail(jobName, Scheduler.DEFAULT_GROUP, jobClazz);
			jobDetail.getJobDataMap().put(jobName, configs[2].trim());
			
			String triggerName = "Trigger-" + code;
			Trigger trigger;
			try {
				String scheduleTime = configs[1].trim();
				trigger = new CronTrigger(triggerName, Scheduler.DEFAULT_GROUP, scheduleTime); // 第三个参数为定时时间
				scheduler.scheduleJob(jobDetail, trigger);
				
				log.info(" scheduler.scheduleJob: " + jobName + " successed. scheduleTime=" + scheduleTime );
				
				configsMap.put(code, value);
			} 
			catch (Exception e) {
				log.error("初始化定时Job【" + jobName + "】失败, config = " + value, e);
			}  
		}
		
		Set<String> deleteJobCodes = new HashSet<String>(configsMap.keySet());
		deleteJobCodes.removeAll(jobCodes);
		for(String code : deleteJobCodes) {
			deleteJob(code);
		}
        
        log.debug("SchedulerBean init end.");
    }
    
    private void deleteJob(String code) {
    	String jobName = "Job-" + code;
    	try {
			scheduler.deleteJob(jobName, Scheduler.DEFAULT_GROUP);
			configsMap.remove(code);
			log.info(" scheduler.deleteJob: " + jobName + " successed." );
		} 
		catch (SchedulerException e) {
			log.error("删除定时Job：" + jobName + "失败", e);
		}
    }
}

