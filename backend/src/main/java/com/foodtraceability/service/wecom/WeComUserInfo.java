package com.foodtraceability.service.wecom;

/**
 * 企业微信用户信息接口
 * 描述从企业微信获取的用户基本信息
 */
public interface WeComUserInfo {
    String getUserId();
    void setUserId(String userId);
    String getName();
    void setName(String name);
    String getMobile();
    void setMobile(String mobile);
    String getDepartment();
    void setDepartment(String department);
    String getAvatar();
    void setAvatar(String avatar);
    String getPosition();
    void setPosition(String position);
}
