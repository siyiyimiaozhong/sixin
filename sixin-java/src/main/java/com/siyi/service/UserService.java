package com.siyi.service;

import com.siyi.netty.ChatMsg;
import com.siyi.pojo.Users;
import com.siyi.vo.FriendRequestVO;
import com.siyi.vo.MyFriendsVO;

import java.util.List;

public interface UserService {
    /**
     * 判断用户名是否存在
     * @param username
     * @return
     */
    public boolean queryUsernameIsExist(String username);

    /**
     * 判断用户是否存在
     * @param username
     * @param password
     * @return
     */
    public Users queryUserForLogin(String username,String password);

    /**
     * 用户注册
     * @param user
     * @return
     */
    public Users saveUser(Users user);

    /**
     * 修改用户记录
     */
    public Users updateUserInfo(Users user);

    /**
     * 搜索朋友的前置条件
     * @param myUserId
     * @param friendUsername
     * @return
     */
    public Integer preconditionSearchFriend(String myUserId,String friendUsername);

    /**
     * 根据用户名查询用户对象
     * @param username
     * @return
     */
    public Users queryUserInfoByUsername(String username);

    /**
     * 添加好友请求记录
     * @param myUserId
     * @param friendUsername
     */
    public void sendFriendRequest(String myUserId, String friendUsername);

    /**
     * 查询好友请求
     * @param acceptUserId
     * @return
     */
    public List<FriendRequestVO> queryFriendRequestList(String acceptUserId);

    /**
     * 删除好友请求记录
     * @param sendUserId
     * @param acceptUserId
     */
    public void deleteFriendRequest(String sendUserId,String acceptUserId);

    /**
     * 通过好友请求
     * @param sendUserId
     * @param acceptUserId
     */
    public void passFriendRequest(String sendUserId,String acceptUserId);

    /**
     * 查询好友列表
     * @param userId
     * @return
     */
    public List<MyFriendsVO> queryMyFriends(String userId);

    /**
     * 保存聊天消息到数据库
     * @param chatMsg
     * @return
     */
    public String saveMsg(ChatMsg chatMsg);

    /**
     * 批量签收消息
     * @param msgIdList
     */
    public void updateMsgSigned(List<String> msgIdList);

    /**
     * 获取未签收消息列表
     * @param acceptUserId
     * @return
     */
    public List<com.siyi.pojo.ChatMsg> getUnReadMsgList(String acceptUserId);
}
