package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.PasswordPolicy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 密码策略数据访问层
 */
@Mapper
public interface PasswordPolicyMapper extends BaseMapper<PasswordPolicy> {

    /**
     * 获取默认密码策略
     */
    PasswordPolicy selectDefaultPolicy();

    /**
     * 根据策略名称查询
     */
    PasswordPolicy selectByPolicyName(@Param("policyName") String policyName);
}
