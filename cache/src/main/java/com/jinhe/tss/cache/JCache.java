/* ==================================================================   
 * Created [2007-1-3] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:jinpujun@hotmail.com
 * Copyright (c) Jon.King, 2012-2015 
 * ================================================================== 
 */
package com.jinhe.tss.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;

import com.jinhe.tss.cache.extension.threadpool.IThreadPool;
import com.jinhe.tss.cache.strategy.CacheConstants;
import com.jinhe.tss.cache.strategy.CacheStrategy;
import com.jinhe.tss.util.BeanUtil;
import com.jinhe.tss.util.XMLDocUtil;

/**
 * 缓存池管理类
 * 
 */
public class JCache {

	protected Logger log = Logger.getLogger(this.getClass());

	private static Map<String, Pool> pools = new HashMap<String, Pool>();

	/** 配置的缓冲池Code列表 */
	private static List<String> configedPoolCodes = new ArrayList<String>();

	private JCache() {
	}

	private static JCache cache = null;

	/**
	 * 获取一个缓存池管理类实例。
	 * singleton
	 * 
	 * @return
	 */
	public static JCache getInstance() {
		return getInstance(CacheConstants.STRATEGY_PATH);
	}

	public static JCache getInstance(String cacheConfigFile) {
		if (cache == null) {
			cache = new JCache();
			cache.initPools(cacheConfigFile);
		}
		return cache;
	}

	/**
	 * 根据缓存策略文件里的配置初始化各个缓存池。
	 */
	private void initPools(String cacheConfigFile) {
		try {
			Document doc = XMLDocUtil.createDoc(cacheConfigFile);
			List<Element> nodes = XMLDocUtil.selectNodes(doc, CacheConstants.STRATEGY_NODE_NAME);
			for (Element strategyNode : nodes) {
				Map<String, String> attrsMap = new HashMap<String, String>();
				CacheStrategy strategy = new CacheStrategy();
				for (Iterator<?> it = strategyNode.attributeIterator(); it.hasNext();) {
					Attribute attr = (Attribute) it.next();
					attrsMap.put(attr.getName(), attr.getValue());
				}
				for (Iterator<?> it = strategyNode.elementIterator(); it.hasNext();) {
					Element attrNode = (Element) it.next();
					attrsMap.put(attrNode.getName(), attrNode.getText());
				}

				BeanUtil.setDataToBean(strategy, attrsMap);
				
				String poolCode = strategy.getCode();
				configedPoolCodes.add(poolCode);
				pools.put(poolCode, strategy.getPoolInstance());
			}
		} catch (Exception e) {
			log.info("根据缓存策略配置文件初始化缓存池失败 " + e);
		}
	}

	/**
	 * 根据指定code值获取一个缓存池。  <br/>
	 * 如果获取不到，初始化一个临时的不可见的简单缓存池。
	 * 
	 * @param code
	 * @return
	 */
	public Pool getCachePool(String code) {
		Pool pool = pools.get(code);
		if (pool == null) {
			/* 如果是配置文件里配置的缓冲池获取不到，则先等待3秒钟，其有可能正在初始化。
			   不可随意生成一个新的普通池，否则像ThreadPool这样涉及到downCast的池将会出错 */
			if (configedPoolCodes.contains(code)) {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
				}
				
				// 再取一遍，如果还没生成则直接返回null
				return pools.get(code); 
			}

			log.info("找不到相应的缓存池，请确定code值：【" + code + "】是否正确，系统已经初始化一个临时的简单缓存池。");

			CacheStrategy strategy = new CacheStrategy();
			strategy.setCode(code);
			strategy.setName(code);
			strategy.setVisible(CacheConstants.FALSE);
			pools.put(code, pool = strategy.getPoolInstance());
		}
		return pool;
	}
	
	public IThreadPool getThreadPool() {
		return (IThreadPool) getCachePool(CacheConstants.THREAD_POOL);
	}
	
	public Pool getConnectionPool() {
		return getCachePool(CacheConstants.CONNECTION_POOL);
	}
	
	public Pool getTaskPool() {
		return getCachePool(CacheConstants.TASK_POOL);
	}

	/**
	 * 获取所有的池
	 * 
	 * @return
	 */
	public Set<Map.Entry<String, Pool>> listCachePools() {
		return pools.entrySet();
	}
}