package com.foodtraceability.mapper.marketing;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.marketing.MemberLevel;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 会员等级 Mapper
 */
@Mapper
@Repository
public interface MemberLevelMapper extends BaseMapper<MemberLevel> {
}
