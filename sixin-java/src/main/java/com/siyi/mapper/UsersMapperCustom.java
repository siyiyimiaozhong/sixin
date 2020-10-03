package com.siyi.mapper;

import com.siyi.pojo.Users;
import com.siyi.utils.MyMapper;
import com.siyi.vo.FriendRequestVO;
import com.siyi.vo.MyFriendsVO;

import java.util.List;

public interface UsersMapperCustom extends MyMapper<Users> {

    public List<FriendRequestVO> queryFriendRequestList(String acceptUserId);

    public List<MyFriendsVO> queryMyFriends(String userId);

    public void batchUpdateMsgSigned(List<String> msgIdList);
}