package com.jinhe.tss.framework.sso.appserver;

import java.util.Map;

import org.dom4j.Element;

import com.jinhe.tss.framework.component.param.ParamManager;
import com.jinhe.tss.framework.exception.BusinessException;
import com.jinhe.tss.util.BeanUtil;
import com.jinhe.tss.util.XMLDocUtil;

/** 
 * UM的appServer管理启用参数管理(ParamAppServerStorer)，以免增加应用就要修改appServer.xml文件。
 * 
 */
public class ParamAppServerStorer implements IAppServerStorer {
    
    public AppServer getAppServer(String appCode) {
        String appServerXML = null;
        try {
        	// 读取appServer配置不依赖Spring容器，因某些bean初始化本身就要用到AppServer
        	appServerXML = ParamManager.getValueNoSpring(appCode);
        }
        catch(Exception e) { }
        
        if(appServerXML == null) {
            throw new BusinessException("参数管理模块中尚没有应用Code为：" + appCode + " 的应用服务配置信息"); 
        }
        
        try{
            Element appServerNode = XMLDocUtil.dataXml2Doc(appServerXML).getRootElement();
            Map<String, String> attrsMap = XMLDocUtil.dataNode2Map(appServerNode);

            AppServer bean = new AppServer();
            BeanUtil.setDataToBean(bean, attrsMap);
            return bean;
        } 
        catch(Exception e) {
            throw new BusinessException("参数管理模块中应用Code为：" + appCode + " 的应用服务配置信息有误，请检查！");
        }
    }
}
