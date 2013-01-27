package com.jinhe.tss.um.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.jinhe.tss.framework.component.progress.Progress;
import com.jinhe.tss.framework.component.progress.Progressable;
import com.jinhe.tss.framework.exception.BusinessException;
import com.jinhe.tss.framework.persistence.entityaop.DecodeUtil;
import com.jinhe.tss.framework.sso.Environment;
import com.jinhe.tss.um.UMConstants;
import com.jinhe.tss.um.dao.IApplicationDao;
import com.jinhe.tss.um.dao.IGroupDao;
import com.jinhe.tss.um.dao.IGroupUserDao;
import com.jinhe.tss.um.dao.IRoleDao;
import com.jinhe.tss.um.dao.IUserDao;
import com.jinhe.tss.um.entity.Application;
import com.jinhe.tss.um.entity.Group;
import com.jinhe.tss.um.entity.GroupUser;
import com.jinhe.tss.um.entity.RoleGroup;
import com.jinhe.tss.um.entity.User;
import com.jinhe.tss.um.permission.PermissionHelper;
import com.jinhe.tss.um.permission.ResourcePermission;
import com.jinhe.tss.um.service.IGroupService;
import com.jinhe.tss.util.BeanUtil;
import com.jinhe.tss.util.EasyUtils;
import com.jinhe.tss.util.InfoEncoder;
 
public class GroupService implements IGroupService, Progressable{

	@Autowired private IGroupDao groupDao;
	@Autowired private IRoleDao  roleDao;
	@Autowired private IUserDao  userDao;
	@Autowired private IGroupUserDao groupUserDao;
	@Autowired private ResourcePermission resourcePermission;
	@Autowired private IApplicationDao applicationDao;

    public Group getGroupById(Long id) {
        return groupDao.getEntity(id);
    }

    public List<User> findUsersByGroupId(Long groupId) {
        return groupDao.getUsersByGroupId(groupId);
    }

    public List<?> findRolesByGroupId(Long groupId) {
        return groupDao.findRolesByGroupId(groupId);
    }

    public List<?> findEditableRolesByOperatorId() {
        return roleDao.getEditableRoles();
    }

    public Object[] findGroups() {
    	Long operatorId = Environment.getOperatorId();
        List<Object> mainAndAssistantGroups = new ArrayList<Object>();
        mainAndAssistantGroups.addAll(groupDao.getGroupsByType(Group.MAIN_GROUP_TYPE, operatorId));
        mainAndAssistantGroups.addAll(groupDao.getGroupsByType(Group.ASSISTANT_GROUP_TYPE, operatorId));
        
        Object[] objs = getOtherGroupsByOperationId(UMConstants.GROUP_VIEW_OPERRATION);
        return new Object[]{ mainAndAssistantGroups, objs[0], objs[1], objs[2] };
    }
    
    public Object[] getAssistGroupsByOperationId(String operationId) {
        return getGroupsByGroupTypeAndOperationId(Group.ASSISTANT_GROUP_TYPE, operationId);
    }
    
    public Object[] getMainGroupsByOperationId(String operationId) {
        return getGroupsByGroupTypeAndOperationId(Group.MAIN_GROUP_TYPE, operationId);
    }
    
    private Object[] getGroupsByGroupTypeAndOperationId(Integer groupType, String operationId) {
    	Long operatorId = Environment.getOperatorId();
    	
        List<?> groups = groupDao.getGroupsByType(groupType, operatorId, operationId);
        List<Long> groupIds = new ArrayList<Long>();
        for( Object temp : groups ){
            Group group = (Group) temp;
            groupIds.add(group.getId());
        }
        List<?> parentGroups = groupDao.getParentGroupByGroupIds(groupType, groupIds, operatorId, UMConstants.GROUP_VIEW_OPERRATION);
        return new Object[]{groupIds, parentGroups};
    }

    public Object[] getOtherGroupsByOperationId(String operationId) {
    	Long operatorId = Environment.getOperatorId();
        List<?> groups = groupDao.getGroupsByType(Group.OTHER_GROUP_TYPE, operatorId, operationId);  
        
        Group otherAppGroupRoot = null;   
        List<Group> otherGroups = new ArrayList<Group>();
        for( Object temp : groups ){
            Group group = (Group) temp;
            if(UMConstants.OTHER_APPLICATION_GROUP_ID.equals(group.getId())) {
                otherAppGroupRoot = group; // 其他用户组
                continue;
            }
            otherGroups.add(group);
        }
        
        List<?> appIds = resourcePermission.getResourceIds(UMConstants.TSS_APPLICATION_ID, 
                UMConstants.APPLICATION_RESOURCE_TYPE_ID, UMConstants.APPLICATION_VIEW_OPERRATION, operatorId);
        List<?> apps = applicationDao.getApplications(appIds, UMConstants.OTHER_SYSTEM_APP); // 应用系统类型
        return new Object[]{otherAppGroupRoot, otherGroups, apps};
    }
    
    public Object[] getGroupsUnderAppByOperationId(String operationId, String applicationId){
        List<?> groups = groupDao.getGroupsByType(Group.OTHER_GROUP_TYPE, Environment.getOperatorId(), operationId);  
        
        Group otherAppGroupRoot = null;   
        List<Group> otherGroups = new ArrayList<Group>();
        for( Object temp : groups ){
            Group group = (Group) temp;
            if(UMConstants.OTHER_APPLICATION_GROUP_ID.equals(group.getId())) {
                otherAppGroupRoot = group; // 其他用户组
                continue;
            }
            otherGroups.add(group);
        }
 
        List<Application> apps = new ArrayList<Application>();
        apps.add(applicationDao.getApplication(applicationId));
        
        return new Object[]{otherAppGroupRoot, otherGroups, apps};
    }
    
	public void createNewGroup(Group group, String userIdsStr, String roleIdsStr) {
		Long parentId = group.getParentId();
		group.setSeqNo(groupDao.getNextSeqNo(parentId));
		group.setDisabled(groupDao.getEntity(parentId).getDisabled());
		groupDao.saveGroup(group);
        
		saveGroupToUser(group.getId(), userIdsStr);
		saveGroupToRole(group.getId(), roleIdsStr);
	}
    
    public void editExistGroup(Group group, String userIdsStr, String roleIdsStr) {
        groupDao.saveGroup(group);
        
        // 其他用户组只是简单的编辑组的属性，也不改变关系，所以不能调用saveGroupToRole 和 saveGroupToUser
        if (Group.OTHER_GROUP_TYPE.equals(group.getGroupType())) return;
        
        saveGroupToRole(group.getId(), roleIdsStr);
        
        // 主用户组中组对用户的关系不能随便更改，所以不能调用saveGroupToUser
        if (Group.MAIN_GROUP_TYPE.equals(group.getGroupType())) return;
        
        // 只有辅助用户组可以选择组对应的用户(group2UserExistTree有效)，可以调用saveGroupToUser方法       
        saveGroupToUser(group.getId(), userIdsStr);
    }
    
    /** 组对用户：先把该组对应的用户都找到，再把提交上来的用户和找到的数据比较，多的做增加操作。 */
    private void saveGroupToUser(Long groupId, String userIdsStr) {
        List<?> group2Users = groupDao.findGroup2UserByGroupId(groupId);
        
        Map<Long, Object> historyMap = new HashMap<Long, Object>(); //把老的组对用户记录做成一个map，以"userId"为key
        for (Object temp : group2Users) {
            GroupUser groupUser = (GroupUser) temp;
            historyMap.put(groupUser.getUserId(), groupUser);
        }
        
        if ( !EasyUtils.isNullOrEmpty(userIdsStr) ) {
            String[] userIds = userIdsStr.split(",");
            for (String temp : userIds) {
                // 如果historyMap里面没有，则新增用户组对用户的关系；如果historyMap里面有，则从历史记录中移出；剩下的将被删除
                Long userId = Long.valueOf(temp);
                if (historyMap.remove(userId) == null) { 
                    GroupUser group2User = new GroupUser(userId, groupId, groupUserDao.getNextSeqNo(groupId));
                    groupUserDao.create(group2User);
                } 
            }
        }
        
        // historyMap中剩下的就是该删除的了
        groupDao.deleteAll(historyMap.values());
    }
    
    /** 组对角色。先把该组对应的角色都找到，再把提交上来的用户和找到的数据比较，多的做增加操作 */
    private void saveGroupToRole(Long groupId, String roleIdsStr) {
        List<?> group2Roles = groupDao.findGroup2RoleByGroupId(groupId);
        
        Map<Long, Object> historyMap = new HashMap<Long, Object>();// 把老的组对角色记录做成一个map，以"roleId"为key
        for (Object temp : group2Roles) {
            RoleGroup roleGroup = (RoleGroup) temp;
            historyMap.put(roleGroup.getRoleId(), roleGroup);
        }
        
        if ( !EasyUtils.isNullOrEmpty(roleIdsStr) ) {
            String[] roleIds = roleIdsStr.split(",");
            for (String temp : roleIds) {
                // 如果historyMap里面没有，则新增用户组对角色的关系; 如果historyMap里面有，则从历史记录中移出；剩下的将被删除
                Long roleId = Long.valueOf(temp);
                if (historyMap.remove(roleId) == null) {
                    RoleGroup role2Group = new RoleGroup();
                    role2Group.setRoleId(roleId);
                    role2Group.setGroupId(groupId);
                    groupDao.createObject(role2Group);
                } 
            }
        }
        
        // historyMap中剩下的就是该删除的了
        groupDao.deleteAll(historyMap.values());
    }

	public void moveGroup(Long groupId, Long toGroupId) {
		Group group   = groupDao.getEntity(groupId);
		if (toGroupId.equals(group.getParentId())) return;  //向自己的父节点移动,等于没有移动
        
        if(groupDao.getParentsById(toGroupId).contains(group)) {
            throw new BusinessException("不能向自己里面的节点移动"); // 节点向自己或者自己的子节点
        }
		
		group.setSeqNo(groupDao.getNextSeqNo(toGroupId));
		group.setParentId(toGroupId);
        
		String oldDecode = group.getDecode();            //移动的oldDecode值肯定非null
        groupDao.moveGroup(group);                       //被拦截调整整个移动枝的decode值, 同时拦截资源补齐调整
        
        // 移动后组的decode值改变了，需修复组下（包括子组）所有对应用户（GroupUser）的decode值。
        String decode = group.getDecode();
        List<?> list = groupDao.getEntities("from GroupUser o where o.decode like ?", oldDecode + "%" );
        DecodeUtil.repairSubNodeDecode(list, oldDecode, decode);
        
        groupDao.flush();
        
        //如果移动到的组是停用状态，则停用所有移动过来的组和里面的用户。（如果用户自己在移动的组中，则有可能把自己停用掉，允许这么做。）
        Group toGroup = groupDao.getEntity(toGroupId);
        if (UMConstants.TRUE.equals(toGroup.getDisabled()) && UMConstants.FALSE.equals(group.getDisabled())) {
            stopGroup(groupId);
        }
	}

    public void sortGroup(Long groupId, Long toGroupId, int direction) {
        Group sourceGroup = groupDao.getEntity(groupId);
        Long parentId = sourceGroup.getParentId();
        
        // 要对父节点有排序权限才能够对此节点排序
        String resourceTypeId = Group.getResourceType(sourceGroup.getGroupType());
        List<?> parentOperations = PermissionHelper.getInstance().getOperationsByResource(resourceTypeId, parentId, Environment.getOperatorId());
        if(!parentOperations.contains(UMConstants.GROUP_SORT_OPERRATION)) {
            throw new BusinessException("您对这一层树没有排序权限，排序失败！");
        }
        
        List<Group> relationalGroups = groupDao.sort(groupId, toGroupId, direction);
        
        /* 排序后多个同级多个组（包括组下的子组）的decode值发生了变化，GroupUser的decode也需要跟着重新生成。
         * 此处循环一次只维护当前组下的用户（GroupUser）decode值，不包括子组的。 */
        for(Group temp : relationalGroups) {
            List<?> list = groupDao.getEntities("from GroupUser o where o.groupId = ?", temp.getId() );
 
            // 排序后各几点的层级不变，老的decode和新的decode长度一致。
            String decode = temp.getDecode();
            DecodeUtil.repairSubNodeDecode(list, decode, decode);
        }
        groupDao.flush();
    }
    
	public List<Group> copyGroup(Long groupId, Long toGroupId, boolean isCascadeUser) {
		Group copyGroup = groupDao.getEntity(groupId);// 获得将要拷贝的用户组
		groupDao.evict(copyGroup);
        
		Group toGroup = null;
		if (toGroupId == null) {// 复制其他用户组的第一层节点,没有parentGroupId。临时造一个group出来
            toGroup = new Group();
			toGroup.setId(UMConstants.OTHER_APPLICATION_GROUP_ID);
			toGroup.setApplicationId(copyGroup.getApplicationId());
			toGroup.setGroupType(copyGroup.getGroupType());
		} else {
			toGroup = groupDao.getEntity(toGroupId);// 获得拷贝到的用户组
		}
        
		 /* 判断用户有没有拷贝用户组和用户的权限  */
        checkCopyGroupPrivilege(copyGroup, toGroup);
        
        boolean isCopyInTheSameLevel = toGroup.getId().equals(copyGroup.getParentId()); //  是否同层复制
        
        List<?> groups = groupDao.getVisibleSubGroups(groupId);
        Map<Long, Long> idMapping = new HashMap<Long, Long>(); // key:原组的id -- value:新组的id
        List<Group> result = new ArrayList<Group>();
        for ( Object temp : groups ) {
            Group group = (Group) temp;
            Long sourceGroupId = group.getId(); // 新组复制之前的原组Id
            
            groupDao.evict(group);
            group.setId(null);
            group.setApplicationId(toGroup.getApplicationId());
            group.setGroupType(toGroup.getGroupType());
            group.setDisabled(toGroup.getDisabled());
               
            if (sourceGroupId.equals(copyGroup.getId())) {
                group.setSeqNo(groupDao.getNextSeqNo(toGroup.getId()));
                group.setParentId(toGroup.getId());
                if(isCopyInTheSameLevel) {
                	group.setName(UMConstants.COPY_PREFIX_NAME + copyGroup.getName()); // 同层下复制,需要给组名添加前缀，以免重名
                }
            }
            else {
                group.setParentId(idMapping.get(group.getParentId()));
            }
            
            group = groupDao.saveGroup(group);
            idMapping.put(sourceGroupId, group.getId());
            result.add(group);
        }
        
        //只有从其他组复制到主用户组的时候才及联复制用户 !!!!!!
        if(isCascadeUser) { 
        	List<User> users = groupDao.getUsersByGroupIds(idMapping.keySet());
            for (User otherUser : users) {
                User mainUser = new User();
                BeanUtil.copy(mainUser, otherUser);
                mainUser.setId(null);
                mainUser.setApplicationId(UMConstants.TSS_APPLICATION_ID);
                mainUser.setAppUserId(null);
                mainUser.setDisabled(toGroup.getDisabled()); // 如果复制到的组是停用状态，则停用所有复制过来的组和里面的用户。无需判断权限。
                mainUser = userDao.create(mainUser);

                otherUser.setAppUserId(mainUser.getId());
                userDao.update(otherUser);
                
                // 新建一个用户组对应用户关系
                saveGroupUser(idMapping.get(otherUser.getGroupId()), mainUser.getId());
            }
        }
 
		return result;
	}

    /** 判断用户有没有拷贝用户组和用户的权限  */
    private void checkCopyGroupPrivilege(Group copyGroup, Group toGroup) {
        String resourceTypeId = copyGroup.getResourceType();
        
        // 将其他用户组复制到主用户组
        if( !copyGroup.getGroupType().equals(toGroup.getGroupType()) ){
            resourceTypeId = UMConstants.MAINGROUP_RESOURCE_TYPE_ID; // 主用户组资源id             
        }       
        
        //如果是复制，则toGroup为copyGroup父节点
        List<?> parentOperations = PermissionHelper.getInstance().getOperationsByResource(resourceTypeId, toGroup.getId(), Environment.getOperatorId());
        if(!parentOperations.contains(UMConstants.GROUP_ADD_OPERRATION)) {
            throw new BusinessException("对父组（即复制到的目标节点）没有新增权限，不能复制此节点！");
        }
    }
    
    /**
     * 保存组对用户关系，并在groupUserDao中进行补齐。
     * 对用户的权限信息也在groupUserDao.saveGroupUser方法中补齐。
     */
    private void saveGroupUser(Long groupId, Long userId) {
        GroupUser groupUser = new GroupUser(userId, groupId, groupUserDao.getNextSeqNo(groupId));
        groupUserDao.saveGroupUser(groupUser);
    }
    
    public List<Group> copyGroup2OtherApp(Long groupId, Long appId){
    	Application application = applicationDao.getEntity(appId);
        
        List<?> groups = groupDao.getVisibleSubGroups(groupId);
        Map<Long, Long> idMapping = new HashMap<Long, Long>(); // key:原组的id -- value:新组的id
        List<Group> result = new ArrayList<Group>();
        for (int i = 0; i < groups.size(); i++) {
            Group group = (Group) groups.get(i);
            Long sourceGroupId = group.getId(); // 新组复制之前的原组Id
            
            groupDao.evict(group);
            group.setId(null);
            group.setApplicationId(application.getApplicationId());
               
            if (sourceGroupId.equals(groupId)) {
            	group.setSeqNo(groupDao.getNextSeqNo(UMConstants.OTHER_APPLICATION_GROUP_ID));
                group.setParentId(UMConstants.OTHER_APPLICATION_GROUP_ID);
            }
            else {
                group.setParentId(idMapping.get(group.getParentId()));
            }
            
            group = groupDao.saveGroup(group);
            idMapping.put(sourceGroupId, group.getId());
            result.add(group);
        }
        
    	return result;
    }

    public void startOrStopGroup(String applicationId, Long groupId, Integer disabled, Integer groupType) {
        String resourceTypeId = Group.getResourceType(groupType);
        
        if (UMConstants.TRUE.equals(disabled)) { // 停用            
            String operationId = UMConstants.GROUP_DISABLE_OPERRATION;
            if (!checkSubGroupsPermission(applicationId, groupId, resourceTypeId, operationId)) {
                throw new BusinessException("您对停用节点下的某些资源（用户组）没有停用操作权限，不能停用此节点！");
            }
            
            stopGroup(groupId);
        } 
        else { // 启用一个组,该组的父节点也得全部启用
            List<?> groups = groupDao.getParentsById(groupId);
            String operationId = UMConstants.GROUP_ENABLE_OPERRATION;
            List<?> canDoGroups = resourcePermission.getParentResourceIds(applicationId, resourceTypeId, groupId, operationId, 
                    Environment.getOperatorId());
            
            if (groups.size() > canDoGroups.size()) {
                throw new BusinessException("节点之上有用户组没有启用操作权限，不能启用此节点！");
            }
            
            for(Iterator<?> it = groups.iterator();it.hasNext();){
                Group group = (Group) it.next();
                if(group.getDisabled().equals(UMConstants.TRUE)) {
                    group.setDisabled(UMConstants.FALSE);
                    groupDao.update(group); 
                }
            }
        }
    }
    
    // 停用组以及组下的子组和所有的用户
    private void stopGroup(Long groupId) {
        Group group = groupDao.getEntity(groupId);
        groupDao.executeHQL("update Group set disabled = ? where decode like ?", UMConstants.TRUE, group.getDecode() + "%");
       
        /* 
         * 停用主用户组和其他用户组需要停用组下的用户。停用辅助用户组不停用用户，因为辅助用户组当中的用户是从主用户组中选取的.
         * 停用其它用户组用户无需判断权限；停用主用户组用户上面已经判断过了。
         */
        Integer groupType = group.getGroupType();
        if (Group.MAIN_GROUP_TYPE.equals(groupType) || Group.OTHER_GROUP_TYPE.equals(groupType)) {
            List<User> users = groupDao.getUsersByGroupIdDeeply(groupId);
            for( User user : users) {
                if(!UMConstants.TRUE.equals(user.getDisabled())){
                    user.setDisabled(UMConstants.TRUE);
                }
            }       
        }
    }

    public void deleteGroup(String applicationId, Long groupId, Integer groupType) {
        if(groupDao.isOperatorInGroup(groupId, Environment.getOperatorId()))
            throw new BusinessException("当前用户在要操作的组中，不能删除此节点！");
        
        Group group = groupDao.getEntity(groupId);
        String resourceTypeId = Group.getResourceType(groupType);
        String operationId = UMConstants.GROUP_DEL_OPERRATION;
        if ( !checkSubGroupsPermission(applicationId, groupId, resourceTypeId, operationId) ) {
            throw new BusinessException("没有删除用户组权限，不能删除此节点！");
        }
        
        // 辅助用户组里面的用户都是从主用户组选过来的,所以删除的时候只是删除辅助用户组的结构，里面的用户是不删的
        if ( Group.ASSISTANT_GROUP_TYPE.equals(groupType) ) {// 辅助用户组
            groupDao.removeAssistmentGroup(group);
        } 
        else {// 删除主用户组和其他用户组
            groupDao.removeGroup(group);
        }
    }
    
    // 判断对所有子节点是否都拥有指定的操作权限
    private boolean checkSubGroupsPermission(String applicationId, Long groupId, String resourceTypeId, String operationId) {
        List<?> allGroups = groupDao.getChildrenById(groupId);
        List<?> canDoGroups = resourcePermission.getSubResourceIds(applicationId, resourceTypeId, groupId, operationId, 
                Environment.getOperatorId());
        
        //如果将要操作的数量==能够操作的数量,说明对所有组都有操作权限,则返回true
        return allGroups.size() == canDoGroups.size();
    }
    
    public void setPasswordRule(Long groupId, Long ruleId){
        List<?> groups = groupDao.getVisibleSubGroups(groupId);
        for(Object temp : groups){
            Group group = (Group) temp;
            group.setPasswordRuleId(ruleId);
            groupDao.saveGroup(group);
        }
        
        List<User> users = groupDao.getUsersByGroupIdDeeply(groupId);
        for(User user : users){
            user.setPasswordRuleId(ruleId);
            userDao.update(user);
        }
    }
    
    //-------------------------------------用户组从其它用户组导入到主用户组，加入进度条显示------------------------------------------
    public Map<String, Object> getImportGroupData(Long groupId, Long toGroupId){
        Group copyGroup = getGroupById(groupId);
        groupDao.evict(copyGroup);
        Group toGroup = getGroupById(toGroupId);
        checkCopyGroupPrivilege(copyGroup, toGroup);
        
        List<?> groups = groupDao.getVisibleSubGroups(groupId);
        List<Long> groupIds = new ArrayList<Long>();
        for (int i = 0; i < groups.size(); i++) {
            Group group = (Group) groups.get(i);
            groupIds.add(group.getId());
        }
        List<User> users = groupDao.getUsersByGroupIds(groupIds);

        Map<String, Object> paramsMap = new HashMap<String, Object>();
        paramsMap.put("groupId", groupId);
        paramsMap.put("toGroup", toGroup);
        paramsMap.put("groups", groups);
        paramsMap.put("users", users);
        
        return paramsMap;
    }

    public void execute(Map<String, Object> paramsMap, Progress progress) {
        Long groupId = (Long)paramsMap.get("groupId");
        Group toGroup = (Group)paramsMap.get("toGroup");
        List<?> groups = (List<?>)paramsMap.get("groups");
        List<?> users  = (List<?>)paramsMap.get("users");
        
        Map<Long, Long> idMapping = copyGroup(groupId, groups, toGroup, progress);
        copyUser(users, idMapping, progress);
    }

    private Map<Long, Long> copyGroup(Long groupId, List<?> groups, Group toGroup, Progress progress){
        Map<Long, Long> idMapping = new HashMap<Long, Long>(); // key:原组的id -- value:新组的id
        for (int i = 0; i < groups.size(); i++) {
            Group group = (Group) groups.get(i);
            Long sourceGroupId = group.getId(); // 新组复制之前的原组Id
            
            groupDao.evict(group);
            group.setId(null);
            group.setApplicationId(toGroup.getApplicationId());
            group.setGroupType(toGroup.getGroupType());
               
            if (sourceGroupId.equals(groupId)) {
                group.setSeqNo(groupDao.getNextSeqNo(toGroup.getId()));
                group.setParentId(toGroup.getId());
            }
            else {
                group.setParentId(idMapping.get(group.getParentId()));
            }
            
            group = groupDao.saveGroup(group);
            idMapping.put(sourceGroupId, group.getId());
            
            updateProgressInfo(progress, groups.size(), i);
        }
        
        return idMapping;
    }
    
    private void copyUser(List<?> users, Map<Long, Long> idMapping, Progress progress){
        for (int i = 0; i < users.size(); i++) {
            User otherUser = (User) users.get(i);

            User mainGroupUser = new User();
            BeanUtil.copy(mainGroupUser, otherUser);
            mainGroupUser.setId(null);
            mainGroupUser.setApplicationId(UMConstants.TSS_APPLICATION_ID);
            mainGroupUser.setAppUserId(null);
            String password = InfoEncoder.string2MD5(mainGroupUser.getLoginName() + "_" + mainGroupUser.getPassword());
            mainGroupUser.setPassword(password); //加密密码
            mainGroupUser = userDao.create(mainGroupUser);

            //设置主用户组和其它用户组用户的对应关系
            otherUser.setAppUserId(mainGroupUser.getId());
            userDao.create(otherUser);
            
            // 新建一个用户组对应用户关系
            saveGroupUser(idMapping.get(otherUser.getGroupId()), mainGroupUser.getId());
            
            updateProgressInfo(progress, users.size(), i);
        }
    }
    
    /**
     * 更新进度信息
     */
    private void updateProgressInfo(Progress progress, long total, int index){
        groupDao.flush();
        
        index = index + 1; // index 从0开始计数
        if(index % 20 == 0) {
            progress.add(20); // 每复制20个更新一次进度信息
        }
        else if(index == total) {
            progress.add(index % 20); // 如果已经同步完，则将总数除以20取余数做为本次完成个数来更新进度信息
        }
    }
}