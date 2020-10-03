package com.siyi.controller;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.siyi.bo.UsersBO;
import com.siyi.enums.OperatorFriendRequestTypeEnum;
import com.siyi.enums.SearchFriendsStatusEnum;
import com.siyi.mapper.UsersMapper;
import com.siyi.pojo.ChatMsg;
import com.siyi.pojo.Users;
import com.siyi.service.UserService;
import com.siyi.utils.FastDFSClient;
import com.siyi.utils.FileUtils;
import com.siyi.utils.JSONResult;
import com.siyi.utils.MD5Utils;
import com.siyi.vo.MyFriendsVO;
import com.siyi.vo.UsersVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("u")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private FastDFSClient fastDFSClient;

    @PostMapping("/registOrLogin")
    public JSONResult registOrLogin(@RequestBody Users user) throws Exception {
        if(StringUtils.isBlank(user.getUsername()) || StringUtils.isBlank(user.getPassword())){
            return JSONResult.errorMsg("用户名或密码不能为空");
        }
        boolean usernameIsExist = userService.queryUsernameIsExist(user.getUsername());
        Users userResult = null;
        if(usernameIsExist){
            //1.登录
            userResult = userService.queryUserForLogin(user.getUsername(), MD5Utils.getMD5Str(user.getPassword()));
            if(userResult == null){
                return JSONResult.errorMsg("用户名或密码错误");
            }
        }else{
            //2.注册
            user.setNickname(user.getUsername());
            user.setFaceImage("group1/M00/00/00/rB_zn18o0VWAWi_-ABld8HKs4es059_80x80.png");
            user.setFaceImageBig("group1/M00/00/00/rB_zn18o0VWAWi_-ABld8HKs4es059.png");
            user.setPassword(MD5Utils.getMD5Str(user.getPassword()));
            userResult = userService.saveUser(user);
        }
        UsersVO userVO = new UsersVO();
        BeanUtils.copyProperties(userResult,userVO);
        return JSONResult.ok(userVO);
    }

    @PostMapping("uploadFaceBase64")
    public JSONResult uploadFaceBase64(@RequestBody UsersBO usersBO) throws Exception {
        //获取前端传过来的base64字符串，然后转换为文件对象再上传
        String base64Data = usersBO.getFaceData();
        String userFacePath = "D:\\user"+usersBO.getUserId()+"userface64.png";
        FileUtils.base64ToFile(userFacePath,base64Data);

        //上传文件到fastdfs
        MultipartFile faceFile = FileUtils.fileToMultipart(userFacePath);
        System.out.println(faceFile);
        String url = fastDFSClient.uploadBase64(faceFile);
        System.out.println(url);

        //获取缩略图的url
        String thump = "_80x80.";
        String[] arr = url.split("\\.");
        String thumpImgUrl = arr[0] + thump + arr[1];

        //更新用户头像
        Users user = new Users();
        user.setId(usersBO.getUserId());
        user.setFaceImage(thumpImgUrl);
        user.setFaceImageBig(url);
        user = userService.updateUserInfo(user);
        UsersVO usersVO = new UsersVO();
        BeanUtils.copyProperties(user,usersVO);
        System.out.println("----");
        return JSONResult.ok(usersVO);
    }

    /**
     * 设置昵称
     * @param usersBO
     * @return
     * @throws Exception
     */
    @PostMapping("setNickname")
    public JSONResult setNickname(@RequestBody UsersBO usersBO) throws Exception {
        Users user = new Users();
        user.setId(usersBO.getUserId());
        user.setNickname(usersBO.getNickname());
        user = userService.updateUserInfo(user);
        UsersVO usersVO = new UsersVO();
        BeanUtils.copyProperties(user,usersVO);
        return JSONResult.ok(usersVO);
    }

    /**
     * 搜索好友接口，根据账号做匹配查询而不是模糊查询
     * @param myUserId
     * @param friendUsername
     * @return
     */
    @PostMapping("search")
    public JSONResult searchUser(String myUserId,String friendUsername){
        //判断myUserId friendUsername 不能为空
        if(StringUtils.isBlank(myUserId)
                || StringUtils.isBlank(friendUsername)){
            return JSONResult.errorMsg("");
        }
        //前置条件 - 1.搜索的用户如果不存在，返回【无此用户】
        //前置条件 - 2.搜索账号是你自己，返回【不能添加自己】
        //前置条件 - 3.搜索的用户已经是你的好友，返回【该用户已经是你的好友】
        Integer status = userService.preconditionSearchFriend(myUserId,friendUsername);
        if(status != SearchFriendsStatusEnum.SUCCESS.status){
            String errorMsg = SearchFriendsStatusEnum.getMsgByKey(status);
            return JSONResult.errorMsg(errorMsg);
        }
        Users user = userService.queryUserInfoByUsername(friendUsername);
        UsersVO usersVO = new UsersVO();
        BeanUtils.copyProperties(user,usersVO);
        return JSONResult.ok(usersVO);
    }

    /**
     * 添加好友
     * @param myUserId
     * @param friendUsername
     * @return
     */
    @PostMapping("addFriendRequest")
    public JSONResult addFriendRequest(String myUserId,String friendUsername){
        //判断myUserId friendUsername 不能为空
        if(StringUtils.isBlank(myUserId)
                || StringUtils.isBlank(friendUsername)){
            return JSONResult.errorMsg("");
        }
        //前置条件 - 1.搜索的用户如果不存在，返回【无此用户】
        //前置条件 - 2.搜索账号是你自己，返回【不能添加自己】
        //前置条件 - 3.搜索的用户已经是你的好友，返回【该用户已经是你的好友】
        Integer status = userService.preconditionSearchFriend(myUserId,friendUsername);
        if(status != SearchFriendsStatusEnum.SUCCESS.status){
            String errorMsg = SearchFriendsStatusEnum.getMsgByKey(status);
            return JSONResult.errorMsg(errorMsg);
        }
        userService.sendFriendRequest(myUserId,friendUsername);
        return JSONResult.ok();
    }

    @PostMapping("/queryFriendRequests")
    public JSONResult queryFriendRequest(String userId){
        if(StringUtils.isBlank(userId)){
            return JSONResult.errorMsg("");
        }
        //查询用户接收到的朋友申请
        return JSONResult.ok(userService.queryFriendRequestList(userId));
    }

    @PostMapping("operFriendRequest")
    public JSONResult operFriendRequest(String acceptUserId,String sendUserId,Integer operType){
        if(StringUtils.isBlank(acceptUserId) || StringUtils.isBlank(sendUserId) || operType==null){
            return JSONResult.errorMsg("");
        }
        if(StringUtils.isBlank(OperatorFriendRequestTypeEnum.getMsgByType(operType))){
            return JSONResult.errorMsg("");
        }
        if(operType == OperatorFriendRequestTypeEnum.IGNORE.type){
            //判断如果忽略好友请求，则直接删除好友请求的数据库表记录
            userService.deleteFriendRequest(sendUserId,acceptUserId);
        }else if(operType == OperatorFriendRequestTypeEnum.PASS.type){
            //判断如果是通过好友请求，则互相增加好友记录到对应数据库对应的表
            //然后删除好友请求的数据库表记录
            userService.passFriendRequest(sendUserId,acceptUserId);
        }
        List<MyFriendsVO> myFriends = userService.queryMyFriends(acceptUserId);
        return JSONResult.ok(myFriends);
    }

    /**
     * 查询我的好友列表
     * @param userId
     * @return
     */
    @PostMapping("myFriends")
    public JSONResult myFriends(String userId){
        if(StringUtils.isBlank(userId)){
            return JSONResult.errorMsg("");
        }
        List<MyFriendsVO> myFriends = userService.queryMyFriends(userId);
        return JSONResult.ok(myFriends);
    }

    @PostMapping("getUnReadMsgList")
    public JSONResult getUnReadMsgList(String acceptUserId){
        if(StringUtils.isBlank(acceptUserId)){
            return JSONResult.errorMsg("");
        }
        //查询列表
        List<ChatMsg> unReadMsgList = userService.getUnReadMsgList(acceptUserId);
        return JSONResult.ok(unReadMsgList);
    }
}