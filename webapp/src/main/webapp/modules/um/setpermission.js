/* 后台响应数据节点名称 */
XML_SEARCH_PERMISSION = "SearchPermissionFrom";
XML_RESOURCE_TYPE = "ResourceTypeList";
XML_PERMISSION_MATRIX = "PermissionMatrix";

/* XMLHTTP请求地址汇总 */
URL_INIT            = AUTH_PATH + "role/permission/initsearch/";  // {isRole2Resource}/{roleId}
URL_RESOURCE_TYPES  = AUTH_PATH + "role/resourceTypes/";  // {applicationId}
URL_PERMISSION      = AUTH_PATH + "role/permission/matrix/";  // {permissionRank}/{isRole2Resource}/{roleId}
URL_SAVE_PERMISSION = AUTH_PATH + "role/permission/";  // {permissionRank}/{isRole2Resource}/{roleId} POST

if(IS_TEST) {
	URL_INIT            = "data/setpermission_init.xml?";
	URL_RESOURCE_TYPES  = "data/resourcetypeList.json?";
	URL_PERMISSION      = "data/setpermission.xml?";
	URL_SAVE_PERMISSION = "data/_success.xml?";
}
 
function init() {	
    var params = {};

	$.ajax({
		url : URL_INIT + params.isRole2Resource + "/" + params.roleId,
		params  : params, 
		onresult : function() {
			var xmlData = this.getNodeValue(XML_SEARCH_PERMISSION);
			var isRole2Resource = ("0" == params["isRole2Resource"]);

			$.cache.XmlDatas[XML_SEARCH_PERMISSION] = xmlData;
			
			if(isRole2Resource) {
				// 设置用户、用户组权限，自动隐藏应用系统和资源类型字段
				var hideCells = xmlData.querySelectorAll("layout>TR>TD[binding='applicationId' or binding='resourceType']");
				$.each(hideCells, function(i, cell){
					cell.setAttribute("style", "display:none");
				});
			}

			var xform = $.F("permissionForm", xmlData);		 

			// 设置查询按钮操作
			$1("page3BtSearch").onclick = function() {
				searchPermission();
			}
		}
	});
}

function getResourceTypes(applicationId) {
	$.ajax({
		url : URL_RESOURCE_TYPES + applicationId,
		type : "json",
		ondata : function() { 
			var result = this.getResponseJSON();
			if( result && result.length > 0) {
				var sEl = $1("resourceType");
				sEl.options.length = 0; // 先清空
				for(var i = 0; i < result.length; i++) {
					sEl.options[i] = new Option(result[i].name, result[i].id);
				}

				// 设置为默认选中第一个
				$.F("permissionForm").updateDataExternal("resourceType", sEl.options[0].value);
			}				
		}
	});
}

function searchPermission() {
    var xformObj = $.F("permissionForm");
    var permissionRank  = xformObj.getData("permissionRank");
    var isRole2Resource = xformObj.getData("isRole2Resource");
    var roleID          = xformObj.getData("roleId");
	var applicationId   = xformObj.getData("applicationId");
    var resourceType    = xformObj.getData("resourceType");

	$.ajax({
		url : URL_PERMISSION + permissionRank + "/" + isRole2Resource + "/" + roleID,
		params : {"applicationId": applicationId, "resourceType": resourceType}, 
		onresult : function() { 
			var role2PermissionNode = this.getNodeValue(XML_PERMISSION_MATRIX);

			// 给树节点加搜索条件属性值，以便保存时能取回
			role2PermissionNode.setAttribute("applicationId", applicationId);
			role2PermissionNode.setAttribute("resourceType", resourceType);
			role2PermissionNode.setAttribute("permissionRank", permissionRank);
			role2PermissionNode.setAttribute("isRole2Resource", isRole2Resource);
			role2PermissionNode.setAttribute("roleId", roleID);

			$.cache.XmlDatas[XML_PERMISSION_MATRIX] = role2PermissionNode;

			if(role2PermissionNode == null) {
				var xmlReader = new XmlReader("<actionSet></actionSet>");
				role2PermissionNode = new XmlNode(xmlReader.documentElement);
			}

			$.PT("permissionTree", role2PermissionNode);
		}
	});
}

function savePermission() {
	var tree = $.PT("permissionTree");

    // 用户对权限选项
	var role2PermissionNode = $.cache.XmlDatas[XML_PERMISSION_MATRIX];

	// 取回搜索条件，加入到提交数据
	var applicationId   = role2PermissionNode.getAttribute("applicationId");
	var resourceType    = role2PermissionNode.getAttribute("resourceType");
	var permissionRank  = role2PermissionNode.getAttribute("permissionRank");           
	var isRole2Resource = role2PermissionNode.getAttribute("isRole2Resource");
	var roleID          = role2PermissionNode.getAttribute("roleId");

	var nodesPermissions = [];
	tree.getAllNodes().each(function(i, curNode) {
		var curNodeOptionStates = "";
		$.each(tree._options, function(_optionId, _option) {
			var curNodeOptionState = curNode.attrs[_optionId];

			// 父节点是2(即所有子节点全选中)的，则子节点不需要传2，后台会自动补齐
			if( curNodeOptionState == "2" ) {
				if(curNode.parent && curNode.parent.getAttribute(_optionId) == "2") {
					curNodeOptionState = "0";
				}
			}

			curNodeOptionStates += curNodeOptionState || "0";
		} );
 
		// 整行全部标记至少有一个为1或者2才允许提交，都是0的话没必要提交
		if("0" == isRole2Resource || true == /(1|2)/.test(curNodeOptionStates)) {
			nodesPermissions.push(curNode.id + "|" + curNodeOptionStates);                
		}
	});

	// 即使一行数据也没有，也要执行提交 
	$.ajax({
		url : URL_SAVE_PERMISSION + permissionRank + "/" + isRole2Resource + "/" + roleID,
		params : {
			"applicationId": applicationId, 
			"resourceType": resourceType, 
			"permissions":nodesPermissions.join(",")
		}
	});
}

window.onload = init;




;(function($, factory) {

    $.PTree = factory($);

    var TreeCache = {};

    $.PT = function(id, data) {
        var tree = TreeCache[id];
        if( tree == null || data ) {
            tree = new $.PTree($1(id), data);
            TreeCache[id] = tree;   
        }
        
        return tree;
    }

})(tssJS, function($) {

    'use strict';

    var _Option = function(node) {
    	this.id = $.XML.getText( node.querySelector("operationId") );
    	this.name = $.XML.getText( node.querySelector("operationName") );
    	this.dependId = $.XML.getText( node.querySelector("dependId") );
    	this.dependParent = $.XML.getText( node.querySelector("dependParent") );

    	this.dependers = []; // 横向依赖我的。双向维护横向依赖。
    },

	/*
	 * 设定选中状态
	 *			 0 未选中
	 *			 1 仅此节点有权限
	 *			 2 所有子节点有权限
	 *			 3 未选中禁用
	 *			 4 选中禁用
	 */
	setOptionCheckState = function(optionChecker, nextState) {
		optionChecker.state = nextState;
        optionChecker.node.attrs[optionChecker.option.id] = nextState;

	    nextState = nextState || "0";
        $(optionChecker).css("backgroundImage", "url(images/optionState" + nextState + ".gif)");

        // 横向依赖
        if("1" == nextState) { setDependSelectedState(optionChecker, nextState); } // 仅此选中时同时选中依赖项
        if("2" == nextState) { setDependSelectedState(optionChecker, nextState); } // 所有子节点选中时同时选中依赖项
        if("0" == nextState) { setDependedSelectedState(optionChecker, nextState); } // 取消时同时取消被依赖项
	},

    /* 设置横向依赖项选中状态 */
    setDependSelectedState = function(optionChecker, nextState) {
        var treeNode  = optionChecker.node;
        var curOption = optionChecker.option;
        var dependId  = curOption.dependId;
        if( dependId ) {
            var curState = treeNode.attrs[dependId];

            // 目标状态与当前状态不同(如果当前已经是2，而目标是1则不执行)
            if(nextState != curState && ("2" != curState || "1" != nextState)) {
                var dependOptionChecker = $("span[oid='" + dependId + "']", optionChecker.parentNode)[0];
                setOptionCheckState(dependOptionChecker, nextState);
            }
        }
    },

    /* 设置横向被依赖项选中状态 */
    setDependedSelectedState = function(optionChecker, nextState) {
        var treeNode  = optionChecker.node;
        $.each(treeNode._options, function(_optionId, _option){
            if(_option.dependId == optionChecker.option.id) {
                var dependOptionChecker = $("span[oid='" + _option.id + "']", optionChecker.parentNode)[0];
                if(nextState != dependOptionChecker.state) {
                    setOptionCheckState(dependOptionChecker, nextState);
                }
            }
        });
    },

    /*
     * 改变权限项选中状态为下一状态
     *       选中状态：1仅此选中 / 2当前及所有子节点选中 / 0未选
     *       纵向依赖：2选中上溯，取消下溯 / 3选中下溯，取消上溯
     *
     * 参数：  optionId                    权限项id
               boolean: shiftKey           是否同时按下shiftKey
               nextState                   指定的click后的状态，可选
     */
    changeOptionCheckState = function(ev, optionChecker, shiftKey, nextState) {
        var curState = optionChecker.state;
        var pState   = optionChecker.node.attrs["pstate"];
        
        // nextState 不能超过pState
        if("2" == nextState && "2" != pState) {
            nextState = "1";
        }        

        if("3" == curState || "4" == curState) { // 当前若是禁用状态则不变
            nextState = curState;
        }
        else if(nextState == null ) { // 自动切换状态
            switch(curState || "") {            
                case "0":
                case "":
                    nextState = "1";
                    break;
                case "1":
                    if("2" == pState) {
                        nextState = "2";
                    } else {
                        nextState = "0";
                    }
                    break;
                case "2":
                    nextState = "0";
                    break;
            }        
        }

        if(curState == nextState) return;

        // 修改权限项显示图标
        setOptionCheckState(optionChecker, nextState);

        var treeNode = optionChecker.node;
        var option = optionChecker.option;
        var optionId = option.id;
        var dependParent = option.dependParent;
        
        

        // // 纵向依赖
        // if( dependParent ) {
        //     if(("2" == dependParent && "1" == nextState) || ("3" == dependParent && "0" == nextState)) {                   
        //         setParentSelectedState(treeNode, optionId, nextState);  // 纵向依赖2选中或者3取消时，上溯
        //     }
        //     else if("2" == dependParent && "2" == nextState) {                   
        //         setParentSelectedState(treeNode, optionId, "1"); // 纵向依赖2选中，上溯，父节点半勾
        //     }
        //     else if(("2" == dependParent && "0" == nextState) || ("3" == dependParent && "1" == nextState)) {                   
        //         setChildsSelectedState(treeNode, optionId, nextState); // 纵向依赖2取消或者3选中时，下溯
        //     }
        // }

        // // 当前节点目标状态是2(所有子节点)时，下溯
        // if("2" == nextState) {
        //     setChildsSelectedState(treeNode, optionId, nextState);
        // }

        // // 当前节点目标状态是0或者1，则设置父节点仅此
        // if("0" == nextState || "1" == nextState) {
        //     setParentSingleState(treeNode, optionId);
        // }

        // // 同时按下shift键时
        // if( shiftKey ) {
        //     setChildsSelectedState(treeNode, optionId, nextState, true);
        // }
    },

    PTree = function(el, data) {
        this.el = el;
        this.rootList = [];
        this._options = {};

        this.init = function() {
            loadXML(data);

            $(this.el).html("");         

            var headEl = $.createElement("div", "optionTitle");
            $.each(this._options, function(id, _option) {
            	var optionTitle = $.createElement("span");
            	$(optionTitle).html(_option.name);
            	headEl.appendChild(optionTitle);

            });
            this.el.appendChild(headEl);

            var ul = $.createElement("ul");
            this.rootList.each(function(i, root){
                var li = root.toHTMLTree();
                ul.appendChild(li);
            });
            this.el.appendChild(ul);
        }

        var tThis = this;
        var loadXML = function(data) {
            var nodes = data.querySelectorAll("treeNode");
            var parents = {};
            $.each(nodes, function(i, xmlNode) {
                var nodeAttrs = {};
                $.each(xmlNode.attributes, function(j, attr) {
                    nodeAttrs[attr.nodeName] = attr.value;
                });

                var parentId = xmlNode.parentNode.getAttribute(_TREE_NODE_ID);
                var parent = parents[parentId];
                var treeNode = new TreeNode(nodeAttrs, parent);

                if(parent == null) {
                    tThis.rootList.push(treeNode);
                }   
                parents[treeNode.id] = treeNode;
            });

            var optionNodes = data.querySelectorAll("options>option");
            $.each(optionNodes, function(i, node) {
            	var _option = new _Option(node);
            	tThis._options[_option.id] = _option;
            });

            $.each(tThis._options, function(id, _option) {
            	if(_option.dependId) {
            		tThis._options[_option.dependId].dependers.push(_option);
            	}
            });
        };

        // 树控件上禁用默认右键和选中文本（默认双击会选中节点文本）
        this.el.oncontextmenu = this.el.onselectstart = function(_event) {
            $.Event.cancel(_event || window.event);
        }   

        var 
            _TREE_NODE = "treeNode",
            _TREE_NODE_ID = "id",
            _TREE_NODE_NAME = "name",

        clickSwich = function(node) {
            node.opened = !node.opened;

            var styles = ["node_close", "node_open"],
                index = node.opened ? 0 : 1;

            $(node.li.switchIcon).removeClass(styles[index]).addClass(styles[++index % 2]);

            if(node.li.ul) {
                if(node.opened) {
                    $(node.li.ul).removeClass("hidden");
                    var parent = node;
                    while(parent = parent.parent) {
                        $(parent.li.ul).removeClass("hidden");
                        $(parent.li.switchIcon).removeClass(styles[0]).addClass(styles[1]);
                    }
                } 
                else {
                    $(node.li.ul).addClass("hidden");
                }
            }
        },

        TreeNode = function(attrs, parent) {            
            this.id   = attrs[_TREE_NODE_ID];
            this.name = attrs[_TREE_NODE_NAME];
            this.opened = (attrs._open == "true");
            this.attrs = attrs;

            // 维护成可双向树查找
            this.children = [];
            this._options = tThis._options;
          
            if(parent) {
            	this.parent = parent;
                this.level = this.parent.level + 1;
                this.parent.children.push(this);
            } else {
                this.level = 1;
            }               

            this.toHTMLTree = function() {
                var stack = [];
                stack.push(this);

                var current, currentEl, rootEl, ul;
                while(stack.length > 0) {
                    current = stack.pop();
                    var currentEl = current.toHTMLEl();
                    if(rootEl == null) {
                        rootEl = currentEl;
                    }
                    else {
                        ul = rootEl.querySelector("ul[pID ='" + current.parent.id + "']");
                        ul.pNode = current;
                        ul.insertBefore(currentEl, ul.firstChild);
                    }

                    current.children.each(function(i, child) {
                        stack.push(child);
                    });
                }

                return rootEl;
            };
        };

        TreeNode.prototype = {
            toHTMLEl: function() {
                var li = $.createElement("li");
                li.setAttribute("nodeID", this.id);
                li.node = this;
                this.li = li;

                // 节点打开、关闭开关
                li.switchIcon = $.createElement("span", "switch");
                li.appendChild(li.switchIcon);

                // 自定义图标
                var selfIcon = $.createElement("div", "selfIcon");
                li.appendChild(selfIcon);
                li.selfIcon = $(selfIcon);

                // 节点名称
                li.a = $.createElement("a");
                li.a.innerText = li.a.title = this.name;
                li.appendChild(li.a);

                // 每个节点都可能成为父节点
                li.ul = $.createElement("ul");
                li.ul.setAttribute("pID", this.id);
                li.appendChild(li.ul);

                if(this.children.length > 0) {                  
                    this.opened = !this.opened;
                    clickSwich(this);

                    li.selfIcon.addClass("folder");
                }
                else { // is leaf
                    $(li.switchIcon).addClass("node_leaf").css("cursor", "default");
                    li.selfIcon.addClass("leaf");
                }

                // 添加option（权限操作项）
                var nThis = this;
                var optionBox = $.createElement("span", "optionBox");
                $.each(tThis._options, function(_optionId, _option) {
                	var optionChecker = $.createElement("span");
                    optionChecker.setAttribute("oid", _optionId);
                    optionChecker.option = _option;
                    optionChecker.node = nThis;

                	var checkState = nThis.attrs[_optionId] || "0";
                	$(optionChecker).html(_option.name)
                		.css("backgroundImage", "url(images/optionState" + checkState + ".gif)");
                	optionBox.appendChild(optionChecker);

                    $(optionChecker).click(function(ev) {
                        var shiftKey = (ev || event).shiftKey; // 是否同时按下shiftKey
                        changeOptionCheckState(ev, optionChecker, shiftKey);
                    });

                });
                li.insertBefore(optionBox, li.ul);

                // 添加事件
                $(li.switchIcon).click( function() { clickSwich(nThis); } );

                return li;
            },

            openNode: function() {
                clickSwich(this);
            }
        };

        /********************************************* 定义树节点TreeNode end *********************************************/

        tThis.init();
        tThis.TreeNode = TreeNode;
    };

    PTree.prototype = {
    	getOption: function(optionId) {
    		return this._options[optionId];
    	}

    };

    return PTree;
});