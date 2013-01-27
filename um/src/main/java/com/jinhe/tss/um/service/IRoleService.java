package com.jinhe.tss.um.service;

import java.util.List;
import java.util.Map;

import com.jinhe.tss.framework.component.log.Logable;
import com.jinhe.tss.um.UMConstants;
import com.jinhe.tss.um.entity.Role;
import com.jinhe.tss.um.permission.filter.PermissionFilter4Sort;
import com.jinhe.tss.um.permission.filter.PermissionTag;

public interface IRoleService {	

	/**
	 * 根据ID查询角色
	 * 
	 * @param id
	 * @return
	 */
	Role getRoleById(Long id);

	/**
	 * <p>
	 * 修改一个Role对象的明细信息、角色对用户信息、角色对用户组的信息
	 * </p>
	 * @param role
	 * @param userIdsStr
	 * @param groupIdsStr
	 */
    @Logable(operateTable="角色", operateType="新建/修改", 
            operateInfo="新建/修改${args[0]}角色（角色对用户信息：${args[1]}、角色对用户组的信息 ：${args[2]}）"
        )
	void saveRole2UserAndRole2Group(Role role, String userIdsStr, String groupIdsStr);	

	/**
	 * 根据用户组的id查找该组下所有用户
	 * 
	 * @param groupId
	 * @return
	 */
	List<?> getUsersByGroupId(Long groupId);
	
	/**
	 * 保存角色组
	 * @param entity
	 * @return
	 */
    @Logable(operateTable="角色组", operateType="新建/修改", 
            operateInfo="新建/修改了角色组 ${args[0]}"
        )
	Role saveRoleGroup(Role entity);
	
	/**
	 * 删除角色
	 * @param id
	 */
    @Logable(operateTable="角色", operateType="删除", 
            operateInfo="删除 (ID: ${args[0]})角色(组)"
        )
	void delete(Long id);
	
	/**
	 * 0-停用/1-启用角色
	 * 
	 * @param id
	 * @param disabled
	 */
    @Logable(operateTable="角色", operateType="停用/启动", 
            operateInfo="停用或启动 (ID: ${args[0]})角色(组)(disabled: ${args[1]})"
        )
	void disable(Long id, Integer disabled);
	
	/**
	 * 排序
	 * @param id
	 * @param targetId
	 * @param direction
	 */
    @Logable(operateTable="角色", operateType="排序", operateInfo="(ID: ${args[0]})节点移动到了(ID: ${args[1]})节点<#if args[2]=1>之下<#else>之上</#if>")
    @PermissionTag(
            operation = UMConstants.ROLE_SORT_OPERRATION, 
            resourceType = UMConstants.ROLE_RESOURCE_TYPE_ID,
            filter = PermissionFilter4Sort.class)
	void sort(Long id, Long targetId, int direction);
	
	/**
	 * 跨父节点移动
	 * @param id
	 * @param targetId
	 */
    @Logable(operateTable="角色", operateType="移动", 
            operateInfo="移动(ID: ${args[0]}) 角色至 (ID: ${args[1]}) 角色组 "
        )
	void move(Long id, Long targetId);

	/**
	 * 获得平台应用系统
	 * @return
	 */
	List<?> getPlatformApplication();	

	/**
	 * 根据应用id查询资源类型
	 * @param applicationId
	 * @return
	 */
	List<?> getResourceTypeByAppId(String applicationId) ;

	/**
	 * 新建角色使用的信息
	 * @return
	 */
	Map<String, Object> getInfo4CreateNewRole();
	
	/**
	 * 编辑角色使用的信息
	 * @param roleId
	 * @return
	 */
	Map<String, Object> getInfo4UpdateExistRole(Long roleId);
	
    /**
     * <p>
     * 返回用户被转授予的角色列表：用户拥有的角色以及用户所在组拥有的角色（供转授使用），包括匿名角色<br/>
     * 用户只能对自身拥有的角色进行转授，如果是因为转授而获得的角色不能再转授。 <br/>
     * 停用的角色虽然可以转授出去，但是使用时会过滤掉的。 <br/>
     * </p>
     * @param userId
     * @return
     */
    @PermissionTag(
            operation = UMConstants.ROLE_VIEW_OPERRATION, 
            resourceType = UMConstants.ROLE_RESOURCE_TYPE_ID
    )
	List<?> getAllVisiableRole();
	
    /**
     * <p>
     * 获取用户有新建权限的角色组。
     * </p>
     * @return
     */
    @PermissionTag(
            operation = UMConstants.ROLE_ADD_OPERRATION, 
            resourceType = UMConstants.ROLE_RESOURCE_TYPE_ID
    )
    List<?> getAddableRoleGroups();

	// ===========================================================================
	// 展示外部资源的授权信息时需要的操作
	// 1.从um中取到当前用户的角色信息
	// ===========================================================================
	/**
     * 获取登陆用户的所有权限
	 * @return
	 */
	List<Long[]> getRoles4Permission();
}