<#assign Long = statics["java.lang.Long"] />
<#assign Integer = statics["java.lang.Integer"] />
<#assign manager = statics["com.jinhe.tss.portal.engine.FreemarkerParser"] />
<#assign Global  = statics["com.jinhe.tss.framework.Global"]/>
<#assign Environment = statics["com.jinhe.tss.framework.sso.Environment"] />

<#assign menuService = Global.getContext().getBean("NavigatorService")/>
<#assign articleService = Global.getContext().getBean("RemoteArticleService")/>

<#assign defaultPageUrl  = "/tss/index.portal"/>
<#assign articleListUrl  = "/tss/articleList.portal"/>
<#assign articlePageUrl  = "/tss/articlePage.portal"/>
<#assign searchResultUrl = "/tss/searchResult.portal"/>
<#assign afterLoginUrl   = "/tss/afterLogin.portal"/>

<#macro getMenuXML menuId>
	<#assign data = menuService.getNavigatorXML(menuId) />
	<#assign doc  = manager.translateValue(data) />
	<#assign menu = doc.Menu />
</#macro>  

<#macro getArticleListXML channelId, page=1, pagesize=5>
	<#assign data = articleService.getArticleListByChannel(channelId, page, pagesize, true) />
	<#assign doc  = manager.translateValue(data)/>
	<#assign articleList = doc.Response.ArticleList.rss/>
    <#assign channelName = articleList.channelName?default('')/>
	<#assign totalPageNum = articleList.totalPageNum/>
	<#assign totalRows = articleList.totalRows/>
	<#assign currentPage = articleList.currentPage/>
</#macro> 

<#macro getArticleXML articleId>
	<#assign data = articleService.getArticleXML(articleId) />
	<#assign doc  = manager.translateValue(data) />
	<#assign article = doc.Response.ArticleInfo.rss.Article/>
</#macro> 

<#macro getChannelTree4Portlet channelId>
	<#assign data = articleService.getChannelTree4Portlet(channelId)/>
	<#assign doc  = manager.translateValue(data) />
</#macro> 