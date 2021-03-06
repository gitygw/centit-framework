package com.centit.framework.model.basedata;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.Date;

/**
 * Userinfo entity.
 * 系统用户信息
 * @author codefan@sina.com
 */
public interface IUserInfo{
    /**
    * 用户编码，是用户的主键
    * @return 用户编码，是用户的主键
    */
    String getUserCode();

    /**
    *  String getUserPin()
    * @return getUserPin
    */
    @JSONField(serialize = false)
    String getUserPin();

    /**
     * 密码有效期时间
     * @return 密码有效期时间
     */
    Date getPwdExpiredTime();

    /**
    * 用户是否有效 T/F/A  T 正常 ， F 禁用,A为新建可以删除
    * @return 用户是否有效 T/F/A  T 正常 ， F 禁用,A为新建可以删除
    */
    String getIsValid();

    /**
    * 用户名称  ，和 getUsername()不同后者返回的是用户登录名称
    * @return 用户名称
    */
    String getUserName();

    /**
    * 用户登录名 同 getUsername
    * @return 用户登录名
    */
    String getLoginName();
    /**
    * 用户默认机构（主机构）代码
    * @return 用户默认机构（主机构）代码
    */
    String getPrimaryUnit();
    /**
    * 用户类别，各个业务系统自定义类别信息
    * @return 用户类别，各个业务系统自定义类别信息
    */
    String getUserType();

    /**
    * 用户注册邮箱
    * @return 用户注册邮箱
    */
    String getRegEmail();
    /**
    * 用户注册手机号码
    * @return 用户注册手机号码
    */
    String getRegCellPhone();

    /**
    * 用户排序号
    * @return 用户排序号
    */
    Long getUserOrder();

    /**
     * 获取用户身份证号码;这个方法不是必须的可以直接返回 null
     * @return 用户身份证号码
     */
    String getIdCardNo();

    /**
     * 获取和第三方对接数据，一般为第三方业务数据组件
     * @return 用户第三发业务中的主键
     */
    String getUserTag();

    /**
     * @return 英文名
     */
    String getEnglishName();

    /**
     * @return 用户描述
     */
    String getUserDesc();
}
