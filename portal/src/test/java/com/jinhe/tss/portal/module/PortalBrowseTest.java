package com.jinhe.tss.portal.module;

import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.jinhe.tss.portal.PortalConstants;
import com.jinhe.tss.portal.TxSupportTest4Portal;
import com.jinhe.tss.portal.action.ComponentAction;
import com.jinhe.tss.portal.action.NavigatorAction;
import com.jinhe.tss.portal.action.PortalAction;
import com.jinhe.tss.portal.entity.Component;
import com.jinhe.tss.portal.entity.Structure;
import com.jinhe.tss.portal.entity.Theme;

/**
 * 测试门户动态浏览
 */
public class PortalBrowseTest extends TxSupportTest4Portal {
    
    @Autowired PortalAction portalAction;
    @Autowired ComponentAction elementAction;
    @Autowired NavigatorAction menuAction;
    
    Long portalId;
    Structure root;
    Structure page1;
    Structure page2;
    Structure section1;
    Structure portletInstance4;
    Theme defaultTheme;
    
    @Before
    public void setUp() throws Exception {
    	 super.setUp();
    	 
         Long parentId = PortalConstants.ROOT_ID;
         
         // 新建portal
         defaultTheme = new Theme();
         defaultTheme.setName("默认主题");
         
         root = new Structure();
         root.setParentId(parentId);
         root.setType(Structure.TYPE_PORTAL);
         root.setName("Jon的门户-1" + System.currentTimeMillis());
         root.setSupplement("<page><property><name>Jon的门户</name><description><![CDATA[]]></description></property><script><file><![CDATA[]]></file><code><![CDATA[]]></code></script><style><file><![CDATA[]]></file><code><![CDATA[]]></code></style></page>");
         root.setDescription("测试门户");
         root.setTheme(defaultTheme);
         root.setCode(System.currentTimeMillis() + "");
         portalAction.save(response, root); // create portal root
         
         portalId = root.getPortalId();
         
         defaultTheme = (Theme) portalService.getThemesByPortal(portalId).get(0);
         
         // 新建页面、版面
         page1 = createPageOrSection(root, "页面一", "page1", Structure.TYPE_PAGE);
         page2 = createPageOrSection(root, "页面二", "page2", Structure.TYPE_PAGE);
         section1 = createPageOrSection(page1, "版面一", "section1", Structure.TYPE_SECTION);
         Structure section2 = createPageOrSection(page2, "版面二", "section2", Structure.TYPE_SECTION);
         Component portlet = createTestPortlet();
         createPortletInstance(section1, "portletInstance1", "portletInstance1", portlet);
         createPortletInstance(section2, "portletInstance2", "portletInstance2", portlet);
         createPortletInstance(section2, "portletInstance3", "portletInstance3", portlet);
         portletInstance4 = createPortletInstance(section2, "portletInstance4", "portletInstance4", portlet);
    }
    
    @Test
    public void testPortalBrowse() {
        try {
        	request.addParameter("themeId", defaultTheme.getId() + "");
            portalAction.previewPortal(response, request, portalId);
        } 
        catch (Exception e) {
        	e.printStackTrace();
            assertFalse(e.getMessage(), true);
        }
        
        testPageBrowse();
        testGetPortalXML();
        
        // 测试门户流量查看
        portalAction.getFlowRate(response, portalId);
    	
    	// 测试门户缓存管理
        portalAction.cacheManage(response, portalId);
        portalAction.flushCache(response, portalId, defaultTheme.getId());
    }
    
    private void testPageBrowse() {
        try {
            request.addParameter("themeId", defaultTheme.getId() + "");
            request.addParameter("pageId", page1.getId() + "");
            portalAction.previewPortal(response, request, portalId);
        } 
        catch (Exception e) {
        	e.printStackTrace();
            assertFalse(e.getMessage(), true);
        }
        
        // 重复访问，以出发流量记录
        for(int i = 0; i < 50; i++) {
        	portalAction.previewPortal(response, request, portalId);
        }
        
        request.addParameter("pageId", section1.getId() + "");
        portalAction.previewPortal(response, request, portalId);
        
        request.addParameter("pageId", portletInstance4.getId() + "");
        portalAction.previewPortal(response, request, portalId);
 
        // 测试门户流量查看
        portalAction.getFlowRate(response, portalId);
    	
    	// 测试门户缓存管理
        portalAction.cacheManage(response, portalId);
        portalAction.flushCache(response, portalId, defaultTheme.getId());
    }
    
    private void testGetPortalXML() {
        try {
            request.addParameter("themeId", defaultTheme.getId() + "");
            portalAction.getPortalXML(response, request, portalId, section1.getId(), page1.getId());
        } 
        catch (Exception e) {
        	e.printStackTrace();
            assertFalse(e.getMessage(), true);
        }
    }
}
