package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.MemberLevel;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 会员等级数据访问层（重命名为 MemberLevelBaseMapper 避免与 marketing 包下的 MemberLevelMapper 冲突）
 */
@Mapper
public interface MemberLevelBaseMapper extends BaseMapper<MemberLevel> {

    /**
     * 查询会员等级分布统计
     * @return 各等级的会员数量分布
     */
    List<Map<String, Object>> selectLevelDistribution();
}
