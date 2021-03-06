package com.jinhe.tss.framework.component.log;

import java.util.List;

import org.springframework.stereotype.Component;

import com.jinhe.tss.cache.Cacheable;
import com.jinhe.tss.cache.JCache;
import com.jinhe.tss.cache.Pool;
import com.jinhe.tss.cache.extension.workqueue.OutputRecordsManager;
import com.jinhe.tss.framework.component.param.ParamConfig;

/**
 * 跟业务操作相关的日志记录器
 * 
 */
@Component("BusinessLogger")
public class BusinessLogger extends OutputRecordsManager implements IBusinessLogger{
   
    private Pool apool;
 
    public BusinessLogger(){
        apool = JCache.getInstance().getTaskPool();
    }

    protected void excuteTask(List<Object> temp) {
        // apool池中有可能混入了其他Task
    	Cacheable item = null;
        while( item == null || !(item.getValue() instanceof LogOutputTask) ) {
        	if(item != null) {
        		apool.removeObject(item.getKey());
        	}
        	
        	item = apool.checkOut(0);
        	break;
        }
        
        LogOutputTask task = (LogOutputTask) item.getValue();
        task.fill(temp);
        item.update(task);
        
        log.debug("正在执行业务日志输出，本次共记录【" + temp.size() +  "】条日志。");
        tpool.excute(apool, item);
    }

    public void output(Log dto) {
        super.output(dto);
    }
    
    /**
     * 日志缓冲池最多可存日志条数的参数
     */
    static final String LOG_FLUSH_MAX_SIZE = "log_flush_max_size";
    
    protected int getMaxSize() {
        try{
            String configValue = ParamConfig.getAttribute(LOG_FLUSH_MAX_SIZE);
            return Integer.parseInt(configValue);
        } catch(Exception e) {
            return super.getMaxSize(); 
        }
    }
}
