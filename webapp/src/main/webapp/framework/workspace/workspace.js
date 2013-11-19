/* 自定义标签名（不含命名空间） */
WS_NAMESPACE     = "WorkSpace";
WS_TAG_PAGE      = "Page";
WS_TAG_PAGE_STEP = "PageStep";
WS_TAG_TAB       = "Tab";
WS_TAG_TAB_BOX   = "TabBox";
WS_TAG_PHASE     = "Phase";
WS_TAG_PHASE_BOX = "PhaseBox";
WS_TAG_ICON      = "Icon";
WS_TAG_NOBR      = "nobr";
WS_TAG_DIV       = "div";
WS_TAG_IMG       = "img";
WS_TAG_TABLE     = "table";

/* 点击Tab到展现内容的时间差(ms) */
_TIMEOUT_TAB_CLICK = 100;
_TIMEOUT_TAB_CLICK_NAME = "ontabclick";

/* 点击Phase到展现内容的时间差(ms) */
_TIMEOUT_PHASE_CLICK = 100;
_TIMEOUT_PHASE_CLICK_NAME = "onphaseclick";

/* onresize的时间差(ms)  */
_TIMEOUT_RESIZE = 20;
_TIMEOUT_RESIZE_NAME = "onresize";

/* 文字信息 */
_INFO_CLOSE = "关闭";

/* 尺寸 */
_SIZE_TAB_WIDTH = 100;
_SIZE_TAB_MARGIN_LEFT = 0;
_SIZE_PHASE_HEIGHT = 73;
_SIZE_PHASE_MARGIN_TOP = 0;
_SIZE_IMG = 14;

/* 样式名 */
CSS_CLASS_NO_CLASS = "";
CSS_CLASS_BOX_HAS_TAB = "hasTab";
CSS_CLASS_TAB_BOX_HAS_TAB = "hasTab";
CSS_CLASS_TAB_ACTIVE   = "active";
CSS_CLASS_PHASE_ACTIVE = "active";
CSS_CLASS_RIGHT_BOX = "rightBox";

/* ***********************************************************************************************
 *	对象名称：Page
 *	职责：负责管理单个子页面的显示、隐藏等控制
 * ***********************************************************************************************/
function Page(obj) {	
	this.object = obj;
	this.id = obj.id;		
	this.isActive = ( obj.currentStyle.display == "none" ? false : true ); // currentStyle: style 和 runtimeStyle 的结合
}

/* Page隐藏  */
Page.prototype.hide = function() {
	this.object.style.display = "none"; // 隐藏对象。与style.visibility = "hidden"不同，其不为被隐藏的对象保留其物理空间
	this.isActive = false;
}

/* Page显示 */
Page.prototype.show = function() {
	this.object.style.display = "block"; // 此元素将显示为块级元素，此元素前后会带有换行符
	this.object.scrollTop = 0;
	this.object.scrollLeft = 0;
	this.isActive = true;
}

Page.prototype.toString = function() {
	var str = [];
	str[str.length] = "[Page 对象]";
	str[str.length] = "id = \"" + this.id + "\"";
	str[str.length] = "visible = \"" + this.isActive + "\"";
	return str.join("\r\n");
}

/*
 *	根据id获取Page实例
 *	参数：	string:id     
 */
Page.getInstance = function(id) {
	return _display.pages[id];
}

/* ***********************************************************************************************
 *	对象名称：Tab
 *	职责：负责生成水平标签页
 * ***********************************************************************************************/
function Tab(label, phasesParams, callback) {
	this.isActive = false;
	this.label = label;
	this.callback = callback;
	this.link ;
	
	this.phases = {};
	this.phasesParams = phasesParams;  
	
	this.object = Element.createNSElement(WS_TAG_TAB, WS_NAMESPACE);
	this.uniqueID = this.object.uniqueID;
	 
	var closeIcon = Element.createNSElement(WS_TAG_ICON, WS_NAMESPACE);
	closeIcon.title = _INFO_CLOSE;
	closeIcon._tab = this;		
	this.object.appendChild(closeIcon);
	
	var div = Element.createNSElement(WS_TAG_DIV);
	div.innerText = this.label;
	div.title = this.label;
	div.noWrap = true; // 不换行
	div._target = this.object;
	this.object.appendChild(div);

	this.createContextMenu();
	
	var oThis = this;
	closeIcon.onclick = this.object.ondblclick = function() {
		oThis.close();
	};	
	this.object.onclick = function() {
		if (!oThis.isActive) {
			oThis.click();
		}		
	};	
}

/* 创建右键菜单  */
Tab.prototype.createContextMenu = function() {
	if( window.Menu != null ) {
		var oThis = this;
		var menu = new Menu();
		
		// 新增一个关闭操作菜单项
		var item = {
			label: _INFO_CLOSE,
			callback: function() { oThis.close(); }
		}
		menu.addItem(item);
		menu.attachTo(this.object, "contextmenu");;
	}
}

/* 关闭标签 */
Tab.prototype.close = function() {
	if( this.link && this == _display.getActiveTab() ) {
		this.hideLink();
	}
	this.dispose();

	// 执行Tab页上定义的回调方法
	this.execCallBack("onTabClose");

	delCacheData(this.SID);

	var firstTab = _display.getFirstTab();
	_display.switchToTab(firstTab);
}

/* 释放标签实例  */
Tab.prototype.dispose = function() {
	if(this == _display.getActiveTab()) {
		this.clearPhases();
	}

	delete _display.tabs[this.uniqueID];

	Element.removeNode(this.object);
	
	Reminder.del(this.SID); // 解除提醒

	this.label = null;
	this.object = null;
	this.uniqueID = null;
	this.link = null;
	this.phases = {};
	this.phasesParams = null;
}

/* 点击标签 */
Tab.prototype.click = function() {
	_display.inactiveAllTabs();
	this.active();

	// 执行Tab页上定义的回调方法
	var params = {};
	this.execCallBack("onTabChange", params);

	if( this.link ) {
		this.showLink();
		this.refreshPhases();
	}
}

/* 显示关联子页面  */
Tab.prototype.showLink = function() {
	_display.showPage(this.link);
}

/* 关闭（隐藏）关联子页面 */
Tab.prototype.hideLink = function() {
	_display.hidePage(this.link);
}

/* 高亮标签 */
Tab.prototype.active = function() {
	this.object.className = CSS_CLASS_TAB_ACTIVE;
	this.isActive = true;
}

/* 低亮标签  */
Tab.prototype.inactive = function() {
	this.object.className = CSS_CLASS_NO_CLASS;
	this.isActive = false;
}

/* 将标签与Page对象关联 */
Tab.prototype.linkTo = function(pageInstance) {
	this.link = pageInstance;
}

/*
 *	将标签插入指定容器
 *	参数：	object:container		HTML容器对象
 */
Tab.prototype.dockTo = function(container) {
	container.appendChild(this.object);
}

/* 以文本方式输出对象信息 */
Tab.prototype.toString = function() {
	var str = [];
	str[str.length] = "[Tab 对象]";
	str[str.length] = "uniqueID = \"" + this.uniqueID+ "\"";
	str[str.length] = "label = \"" + this.label+ "\"";
	return str.join("\r\n");
}

/*
 *	切换到指定Tab页
 *	参数：Phase:phase       Phase实例
		  或者string:pageId     Page实例id
 */
Tab.prototype.switchToPhase = function(phase) {
	if( phase == null ) return;

	switch( typeof(phase) ) {
		case "object":
			phase.click();
			break;
		case "string":                    
			var temp = this.getPhaseByPage(phase);
			if( temp) {
				temp.click();
			}
			break;        
	}			
}

/* 刷新纵向标签  */
Tab.prototype.refreshPhases = function() {
	this.clearPhases();
	this.createPhases();

	if(this.phasesParams) {
		_display.showRightBox();
	}
}

/* 清除纵向标签  */
Tab.prototype.clearPhases = function() {
	for(var item in this.phases) {
		var phase = this.phases[item];
		phase.dispose();
	}
	_display.phaseBox.innerHTML = "";
}

/*
 *	创建纵向标签
 *	参数：	object:phases	纵向标签配置
 */
Tab.prototype.createPhases = function() {
	if( this.phasesParams == null ) return;
	
	for(var i=0; i < this.phasesParams.length; i++) {
		var param = this.phasesParams[i];
		var label = param.label;
		var pageId = param.page;
		var phase = new Phase(label);
		var page = Page.getInstance(pageId);

		phase.linkTo(page);
		phase.dockTo(_display.phaseBox);
		if(pageId == this.link.id) {
			phase.active();
		}

		this.phases[phase.uniqueID] = phase;
	}
}

/* 低亮所有Phase标签 */
Tab.prototype.inactiveAllPhases = function() {
	for(var item in this.phases) {
		var curPhase = this.phases[item];
		curPhase.inactive();
	}
}

/* 获取激活纵向标签 */
Tab.prototype.getActivePhase = function() {
	for(var item in this.phases) {
		var curPhase = this.phases[item];
		if( curPhase.isActive ) {
			return curPhase;
		}
	}
}

/* 根据pageId获取纵向标签 */
Tab.prototype.getPhaseByPage = function(pageId) {
	for(var item in this.phases) {
		var curPhase = this.phases[item];
		if(pageId == curPhase.link.id) {
			return curPhase;
		}
	}
}

/* 激活上一个纵向标签  */
Tab.prototype.prevPhase = function() {       
	var tempPhases = [];
	var activePhaseIndex = null;
	for(var item in this.phases) {
		var curPhase = this.phases[item];
		if( curPhase.isActive ) {
			activePhaseIndex = tempPhases.length;
		}
		tempPhases[tempPhases.length] = curPhase;
	}
	
	var phase;
	if(0 == activePhaseIndex) { // 当前激活的是第一个Phase，即到顶了
		phase = tempPhases[activePhaseIndex];
	} else {
		phase = tempPhases[activePhaseIndex - 1];
	}
	this.switchToPhase(phase);
}

/* 激活下一个纵向标签 */
Tab.prototype.nextPhase = function() {
	var tempPhases = [];
	var activePhaseIndex;
	for(var item in this.phases) {
		var curPhase = this.phases[item];
		if( curPhase.isActive ) {
			activePhaseIndex = tempPhases.length;
		}
		tempPhases[tempPhases.length] = curPhase;
	}
	
	var phase;
	if(tempPhases.length - 1 == activePhaseIndex) { // 当前激活的是最后一个Phase，即到末尾了
		phase = tempPhases[activePhaseIndex];
	} else {
		phase = tempPhases[activePhaseIndex + 1];
	}
	this.switchToPhase(phase);
}

/*
 *	执行回调函数
 *	参数：  string:eventName        事件名称
			object:params           回调函数可用参数
 */
Tab.prototype.execCallBack = function(eventName, params) {
	if( this.callback != null) {
		Public.executeCommand(this.callback[eventName], params);
	}
}

/*
 *	切换到指定Tab页
 *	参数：Phase:phase       Phase实例
		或string:pageId     Page实例id
 */
Tab.prototype.closePhase = function(pageId) {
	var phase = this.getPhaseByPage(pageId);
	if( phase ) {
		phase.dispose();
	}
}

/*
 *	根据id获取Tab实例
 *	参数：  string:uniqueID 
 */
Tab.getInstance = function(uniqueID) {
	return _display.tabs[uniqueID];
}


/* ***********************************************************************************************
 *	对象名称：Phase
 *	职责：负责生成右侧纵向标签页
 * ***********************************************************************************************/
function Phase(label) {
	this.label = label;
	this.link;
	this.isActive = false;
	
	this.object = Element.createNSElement(WS_TAG_PHASE, WS_NAMESPACE);
	this.uniqueID = this.object.uniqueID;
	
	var div = Element.createNSElement(WS_TAG_DIV);
	div.innerText = this.label;
	div.title = this.label;
	div.noWrap = true;
	div._target = this.object;
	
	this.object.appendChild(div);       
	
	var oThis = this;
	this.object.onclick = function() {
		if (!oThis.isActive) {
			oThis.click();
		}		
	};	
}

/* 将标签与Page对象关联  */
Phase.prototype.linkTo = function(pageInstance) {
	this.link = pageInstance;
}

/* 将标签插入指定容器 */
Phase.prototype.dockTo = function(container) {
	container.appendChild(this.object);
}

/* 释放纵向标签实例 */
Phase.prototype.dispose = function() {
	var curActiveTab = _display.getActiveTab();
	delete curActiveTab.phases[this.uniqueID];

	Element.removeNode(this.object);

	this.label = null;
	this.object = null;
	this.uniqueID = null;
	this.link = null;
}

/* 点击标签  */
Phase.prototype.click = function() {
	var curActiveTab = _display.getActiveTab();
	curActiveTab.inactiveAllPhases();

	this.active();
	this.scrollToView();

	var thisObj = this;

	//避免切换太快时显示内容跟不上响应
	clearTimeout( Event.timeout[_TIMEOUT_PHASE_CLICK_NAME] );
	
	Event.timeout[_TIMEOUT_PHASE_CLICK_NAME] = setTimeout( function() {
		if( thisObj.link ) {
			thisObj.showLink();
		}
	}, _TIMEOUT_PHASE_CLICK );
}

/* 将控制标签显示在可见区域内 */
Phase.prototype.scrollToView = function() {
	var tempTop = this.object.offsetTop;
	var tempBottom = this.object.offsetTop + this.object.offsetHeight;
	var areaTop = _display.phaseBox.scrollTop;
	var areaBottom = areaTop + _display.phaseBox.offsetHeight;
	if(tempTop < areaTop) {
		_display.phaseBox.scrollTop = tempTop;
	}
	else if(tempBottom > areaBottom) {
		_display.phaseBox.scrollTop = tempBottom - _display.phaseBox.offsetHeight;
	}
}

/* 显示关联子页面 */
Phase.prototype.showLink = function() {
	_display.showPage(this.link);

	var curActiveTab = _display.getActiveTab();
	curActiveTab.linkTo(this.link);
}

/* 关闭（隐藏）关联子页面  */
Phase.prototype.hideLink = function() {
	_display.hidePage(this.link);
}

/* 高亮纵向标签 */
Phase.prototype.active = function() {
	this.object.className = CSS_CLASS_PHASE_ACTIVE;
	this.isActive = true;
}

/* 低亮纵向标签 */
Phase.prototype.inactive = function() {
	this.object.className = CSS_CLASS_NO_CLASS;
	this.isActive = false;
}

/* 以文本方式输出对象信息 */
Phase.prototype.toString = function() {
	var str = [];
	str[str.length] = "[Phase 对象]";
	str[str.length] = "uniqueID = \"" + this.uniqueID+ "\"";
	str[str.length] = "label = \"" + this.label+ "\"";
	return str.join("\r\n");
}

/* 根据id获取Phase实例 */
Phase.getInstance = function(uniqueID) {
	var curActiveTab = _display.getActiveTab();
	return curActiveTab.phases[uniqueID];
}

/* ***********************************************************************************************
 *	对象名称：Display
 *	职责：负责生成整体界面显示
 * ************************************************************************************************/
function Display(element) {
	this.element = element;

	this.tabBox;
	this.phaseBox;
	this.rightBox;

	this.tabs = {};
	this.buttons = {};
	this.pages = {};

	// 初始化
	this.getAllPages();
	this.hideAllPages();
	this.createUI();
}

/* 获取所有子页面 */
Display.prototype.getAllPages = function() {
	var childs = this.element.getElementsByTagName(WS_TAG_PAGE);
	for(var i=0; i < childs.length; i++) {
		var curNode = childs[i];
		this.pages[curNode.id || curNode.uniqueID] = new Page(curNode);
	}
}

/* 隐藏所有子页面  */
Display.prototype.hideAllPages = function() {
	for(var item in this.pages) {
		var curPage = this.pages[item];
		if(curPage.isActive) {
			curPage.hide();
		}
	}
}

/* 创建界面展示 */
Display.prototype.createUI = function() {
	this.createTabBox();
	this.createRightBox();
	this.createPhaseBox();
	this.hideRightBox();
}

/* 创建Tab标签的容器 */
Display.prototype.createTabBox = function() {
	var tabBox = Element.createNSElement(WS_TAG_TAB_BOX, WS_NAMESPACE);
	var nobr   = Element.createNSElement(WS_TAG_NOBR);

	tabBox.appendChild(nobr);
	this.element.appendChild(tabBox);
	
	var refChild = this.element.firstChild;
	if(refChild != tabBox) {
		this.element.insertBefore(tabBox, refChild); // 插入到第一个
	}

	this.tabBox = tabBox;
}

/* 创建右侧容器 */
Display.prototype.createRightBox = function() {
	var rightBox = Element.createNSElement(WS_TAG_TABLE);
	rightBox.cellSpacing = 0;
	rightBox.cellPadding = 0;
	rightBox.border = 0;
	rightBox.className = CSS_CLASS_RIGHT_BOX;

	var row = rightBox.insertRow();
	row.insertCell();

	this.element.appendChild(rightBox);
	var refChild = this.element.childNodes[1];
	if(rightBox != refChild && refChild) {
		this.element.insertBefore(rightBox, refChild);
	}

	this.rightBox = rightBox;
}

/* 创建纵向Tab标签的容器 */
Display.prototype.createPhaseBox = function() {
	var phaseBox = Element.createNSElement(WS_TAG_PHASE_BOX, WS_NAMESPACE);
	this.rightBox.rows[0].cells[0].appendChild(phaseBox);
	this.phaseBox = phaseBox;
}

/* 显示右侧容器 */
Display.prototype.showRightBox = function() {
	this.rightBox.style.display = "inline";
}

/* 隐藏右侧容器 */
Display.prototype.hideRightBox = function() {
	this.rightBox.style.display = "none";
} 

/*
 *	打开一个新子页
 *	参数：	object:inf 配置参数
 *	返回值：Tab:tab    Tab标签实例
 */
Display.prototype.open = function(inf) {
	var tab = this.getTabBySID(inf.SID);
	
	// 不存在同一数据源tab则新建
	if(null == tab) {             
		tab = new Tab(inf.label, inf.phases, inf.callback);
		this.tabs[tab.uniqueID] = tab;

		var page = this.getPage(inf.defaultPage);
		tab.linkTo(page);
		tab.dockTo(this.tabBox.firstChild);
		tab.SID = inf.SID; // 标记数据源

		this.setHasTabStyle(true);
	}
	
	tab.click();
	return tab;
}

/*
 *	设置有Tab标签的容器样式
 *	参数：boolean:hasTab     容器是否有Tab
 */
Display.prototype.setHasTabStyle = function(hasTab) {
	this.tabBox.className = hasTab ? CSS_CLASS_TAB_BOX_HAS_TAB : CSS_CLASS_NO_CLASS;
}

/*
 *	根据id查找子页
 *	参数：	string:id		子页id
 */
Display.prototype.getPage = function(id) {
	return this.pages[id];
}
/*
 *	显示子页面
 *	参数：	Page:page		Page实例
 */
Display.prototype.showPage = function(page) {
	this.hideAllPages();
	page.show();
}
/*
 *	关闭（隐藏）子页面
 *	参数：	Page:page		Page实例
 */
Display.prototype.hidePage = function(page) {
	page.hide();
}

/*
 *	切换到指定Tab页
 *	参数：Tab:tab   Tab实例
 */
Display.prototype.switchToTab = function(tab) {
	if( tab ) {
		tab.click();
	} 
	else {
		this.setHasTabStyle(false);
	}
}

/* 低亮所有标签 */
Display.prototype.inactiveAllTabs = function() {
	for(var item in this.tabs) {
		var curTab = this.tabs[item];
		curTab.inactive();
	}
}

/* 获取当前激活标签 */
Display.prototype.getActiveTab = function() {
	for(var item in this.tabs) {
		var tab = this.tabs[item];
		if( tab.isActive ) {
			return tab;
		}
	}
}

/*
 *	根据SID获取Tab
 *	参数：  string:SID 
 */
Display.prototype.getTabBySID = function(SID) {
	for(var item in this.tabs) {
		if(SID == this.tabs[item].SID) {
			return this.tabs[item];
		}
	}
}


/* ***********************************************************************************************
   控件名称：标签式工作区
   功能说明：1、动态创建Tab标签
			 2、动态创建纵向Tab标签
			 3、Tab标签控制子页面显示
			 4、双击Tab标签可关闭 
* ***********************************************************************************************/
var _display;

var WorkSpace = function(wsElement) {
	_display = new Display(wsElement);
}
 
/* 打开子页面  */
WorkSpace.prototype.open = function(inf) {
	return _display.open(inf);
}

/* 获取当前激活标签 */
WorkSpace.prototype.getActiveTab = function() {
	return _display.getActiveTab();
}

/* 关闭当前激活标签 */
WorkSpace.prototype.closeActiveTab = function() {
	var tab = this.getActiveTab();
	if( tab ) {
		tab.close();
	}
}
 
/* 激活上一个Phase标签 */
WorkSpace.prototype.prevPhase = function() {
	var tab = this.getActiveTab();
	if( tab ) {
		return tab.prevPhase();
	}
}

/* 激活下一个Phase标签 */
WorkSpace.prototype.nextPhase = function() {
	var tab = this.getActiveTab();
	if( tab ) {
		return tab.nextPhase();
	}
}

/* 关闭指定Phase标签 */
WorkSpace.prototype.closePhase = function(pageId) {
	var tab = this.getActiveTab();
	if( tab ) {
		tab.closePhase(pageId);
	}
}

/* 切换到指定Phase  */
WorkSpace.prototype.switchToPhase = function(page) {
	var tab = this.getActiveTab();
	if( tab ) {
		var phase = tab.getActivePhase();
		if( phase && page != phase.link.id) {
			tab.switchToPhase(page);
		}
	}
}

WorkSpace.prototype.noTabOpend = function() {
	var length = 0; 
	for(var item in _display.tabs) {
		length ++;
	}
	return length == 0;
}