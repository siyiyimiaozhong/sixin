package com.siyi.service.impl;

import com.siyi.enums.MsgActionEnum;
import com.siyi.enums.MsgSignFlagEnum;
import com.siyi.enums.SearchFriendsStatusEnum;
import com.siyi.mapper.*;
import com.siyi.netty.ChatMsg;
import com.siyi.netty.DataContent;
import com.siyi.netty.UserChannelRel;
import com.siyi.pojo.FriendsRequest;
import com.siyi.pojo.MyFriends;
import com.siyi.pojo.Users;
import com.siyi.service.UserService;
import com.siyi.utils.FastDFSClient;
import com.siyi.utils.FileUtils;
import com.siyi.utils.JsonUtils;
import com.siyi.utils.QRCodeUtils;
import com.siyi.vo.FriendRequestVO;
import com.siyi.vo.MyFriendsVO;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.n3r.idworker.Sid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tk.mybatis.mapper.entity.Example;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@Service("userService")
public class UserServiceImpl implements UserService {

    @Autowired
    private UsersMapper usersMapper;
    @Autowired
    private UsersMapperCustom usersMapperCustom;
    @Autowired
    private QRCodeUtils qrCodeUtils;
    @Autowired
    private ChatMsgMapper chatMsgMapper;
    @Autowired
    private Sid sid;
    @Autowired
    private FastDFSClient fastDFSClient;
    @Autowired
    private MyFriendsMapper myFriendsMapper;
    @Autowired
    private FriendsRequestMapper friendsRequestMapper;

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public boolean queryUsernameIsExist(String username) {
        Users user = new Users();
        user.setUsername(username);
        Users result = usersMapper.selectOne(user);
        return result!=null;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users queryUserForLogin(String username, String password) {
        Example userExample = new Example(Users.class);
        Example.Criteria criteria = userExample.createCriteria();
        criteria.andEqualTo("username",username);
        criteria.andEqualTo("password",password);
        Users result = usersMapper.selectOneByExample(userExample);
        return result;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Users saveUser(Users user) {
        String userId = sid.nextShort();
        //为每个用户生成一个唯一的二维码
        String qrCodePath = "/data/"+userId+"qrcode.png";
        // sixin_qrcode:[id]
        qrCodeUtils.createQRCode(qrCodePath,"sixin_qrcode:["+user.getUsername()+"]");
        MultipartFile qrCodeFile = FileUtils.fileToMultipart(qrCodePath);
        String qrCodeUrl = "";
        try {
            qrCodeUrl = fastDFSClient.uploadQRCode(qrCodeFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        user.setQrcode(qrCodeUrl);
        user.setId(userId);
        usersMapper.insert(user);
        return user;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Users updateUserInfo(Users user) {
        usersMapper.updateByPrimaryKeySelective(user);
        return queryUserById(user.getId());
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    Users queryUserById(String userId){
        return usersMapper.selectByPrimaryKey(userId);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Integer preconditionSearchFriend(String myUserId, String friendUsername) {
        //前置条件 - 1.搜索的用户如果不存在，返回【无此用户】
        Users user = queryUserInfoByUsername(friendUsername);
        if(user == null){
            return SearchFriendsStatusEnum.USER_NOT_EXIST.status;
        }
        //前置条件 - 2.搜索账号是你自己，返回【不能添加自己】
        if(user.getId().equals(myUserId)){
            return SearchFriendsStatusEnum.NOT_YOURSELF.status;
        }
        //前置条件 - 3.搜索的用户已经是你的好友，返回【该用户已经是你的好友】
        Example example = new Example(MyFriends.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("myUserId",myUserId);
        criteria.andEqualTo("myFriendUserId",user.getId());
        MyFriends myFriends = myFriendsMapper.selectOneByExample(example);
        if(myFriends != null){
            return SearchFriendsStatusEnum.ALREADY_FRIENDS.status;
        }
        return SearchFriendsStatusEnum.SUCCESS.status;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public Users queryUserInfoByUsername(String username){
        Example example = new Example(Users.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("username",username);
        return usersMapper.selectOneByExample(example);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void sendFriendRequest(String myUserId, String friendUsername) {
        //根据用户名把朋友信息查询出来
        Users friend = queryUserInfoByUsername(friendUsername);
        //1.查询发送好友请求记录表
        Example example = new Example(FriendsRequest.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("sendUserId",myUserId);
        criteria.andEqualTo("acceptUserId",friend.getId());
        FriendsRequest friendsRequest = friendsRequestMapper.selectOneByExample(example);
        if(friendsRequest == null){
            //2.如果不是你的好友，并且好友记录没有添加，则新增好友请求记录
            String requestId = sid.nextShort();
            FriendsRequest request = new FriendsRequest();
            request.setId(requestId);
            request.setSendUserId(myUserId);
            request.setAcceptUserId(friend.getId());
            request.setRequestDateTime(new Date());
            friendsRequestMapper.insert(request);
        }

    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<FriendRequestVO> queryFriendRequestList(String acceptUserId) {
        return usersMapperCustom.queryFriendRequestList(acceptUserId);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void deleteFriendRequest(String sendUserId, String acceptUserId) {
        Example example = new Example(FriendsRequest.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("sendUserId",sendUserId);
        criteria.andEqualTo("acceptUserId",acceptUserId);
        friendsRequestMapper.deleteByExample(example);
    }

    @Override
    public void passFriendRequest(String sendUserId, String acceptUserId) {
        saveFriends(sendUserId,acceptUserId);
        saveFriends(acceptUserId,sendUserId);
        deleteFriendRequest(sendUserId,acceptUserId);

        Channel sendChannel = UserChannelRel.get(sendUserId);
        if(sendChannel != null){
            //使用websocket主动推送消息到请求发起者，更新他的通讯录列表为最新
            DataContent dataContent = new DataContent();
            dataContent.setAction(MsgActionEnum.PULL_FRIEND.type);
            sendChannel.writeAndFlush(new TextWebSocketFrame(JsonUtils.objectToJson(dataContent)));
        }
    }

    private void saveFriends(String sendUserId, String acceptUserId) {
        MyFriends myFriends = new MyFriends();
        myFriends.setId(sid.nextShort());
        myFriends.setMyFriendUserId(acceptUserId);
        myFriends.setMyUserId(sendUserId);
        myFriendsMapper.insert(myFriends);
    }

    @Override
    public List<MyFriendsVO> queryMyFriends(String userId) {
        return usersMapperCustom.queryMyFriends(userId);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public String saveMsg(ChatMsg chatMsg) {
        com.siyi.pojo.ChatMsg msgDB = new com.siyi.pojo.ChatMsg();
        String msgId = sid.nextShort();
        msgDB.setId(msgId);
        msgDB.setAcceptUserId(chatMsg.getReceiverId());
        msgDB.setSendUserId(chatMsg.getSenderId());
        msgDB.setCreateTime(new Date());
        msgDB.setSignFlag(MsgSignFlagEnum.unsign.type);
        msgDB.setMsg(chatMsg.getMsg());
        chatMsgMapper.insert(msgDB);
        return msgId;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public void updateMsgSigned(List<String> msgIdList) {
        usersMapperCustom.batchUpdateMsgSigned(msgIdList);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Override
    public List<com.siyi.pojo.ChatMsg> getUnReadMsgList(String acceptUserId) {
        Example example = new Example(com.siyi.pojo.ChatMsg.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("signFlag",0);
        criteria.andEqualTo("acceptUserId",acceptUserId);
        List<com.siyi.pojo.ChatMsg> result = chatMsgMapper.selectByExample(example);
        return result;
    }
}
