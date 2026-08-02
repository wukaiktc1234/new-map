package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.MemberLevel;
import com.foodtraceability.mapper.MemberLevelBaseMapper;
import com.foodtraceability.service.MemberLevelService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会员等级服务实现
 */
@Service
public class MemberLevelServiceImpl extends ServiceImpl<MemberLevelBaseMapper, MemberLevel> implements MemberLevelService {

    @Override
    public List<MemberLevel> listEnabledLevels() {
        LambdaQueryWrapper<MemberLevel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemberLevel::getStatus, 1)
               .orderByAsc(MemberLevel::getSortOrder);
        return list(wrapper);
    }

    @Override
    public MemberLevel getByCode(String levelCode) {
        LambdaQueryWrapper<MemberLevel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemberLevel::getLevelCode, levelCode);
        return getOne(wrapper);
    }

    @Override
    public Long calculateUpgradeLevel(Integer points, Long totalConsumption) {
        // 获取所有启用的等级，按minPoints降序排列（从高到低）
        List<MemberLevel> levels = listEnabledLevels();
        levels.sort(Comparator.comparingInt(
            (MemberLevel l) -> l.getMinPoints() != null ? l.getMinPoints() : 0
        ).reversed());

        // 找到满足条件的最高等级
        for (MemberLevel level : levels) {
            boolean pointsMatch = level.getMinPoints() == null || points >= level.getMinPoints();
            boolean consumptionMatch = level.getMinConsumption() == null ||
                                       totalConsumption >= level.getMinConsumption();

            if (pointsMatch && consumptionMatch) {
                return level.getLevelId();
            }
        }
        return null;
    }

    @Override
    public Map<String, Object> getLevelDistribution() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> distribution = baseMapper.selectLevelDistribution();
        result.put("distribution", distribution);
        result.put("total", listEnabledLevels().size());
        return result;
    }
}
