package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.MemberLevel;

import java.util.List;
import java.util.Map;

/**
 * 会员等级服务接口
 */
public interface MemberLevelService extends IService<MemberLevel> {

    /**
     * 获取所有启用的等级列表（按排序）
     */
    List<MemberLevel> listEnabledLevels();

    /**
     * 根据等级编码获取等级
     */
    MemberLevel getByCode(String levelCode);

    /**
     * 根据积分和消费额判断应该升级到的等级
     * @param points 当前积分
     * @param totalConsumption 累计消费（分）
     * @return 应该升级的等级ID，如果不需要升级返回null
     */
    Long calculateUpgradeLevel(Integer points, Long totalConsumption);

    /**
     * 获取等级分布统计
     */
    Map<String, Object> getLevelDistribution();
}
