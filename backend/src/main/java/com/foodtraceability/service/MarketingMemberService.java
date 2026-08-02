package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.marketing.MemberCreateDTO;
import com.foodtraceability.dto.marketing.MemberQueryDTO;
import com.foodtraceability.dto.marketing.MemberUpdateDTO;
import com.foodtraceability.dto.marketing.MemberVO;
import com.foodtraceability.entity.MarketingMember;

import java.util.List;

/**
 * 营销CRM会员服务接口
 * 提供会员注册、信息管理、等级升级、标签管理等核心功能
 */
public interface MarketingMemberService extends IService<MarketingMember> {

    /**
     * 会员注册
     * @param createDTO 注册信息
     * @return 新创建的会员
     */
    MarketingMember register(MemberCreateDTO createDTO);

    /**
     * 根据手机号查询会员
     */
    MarketingMember getByPhone(String phone);

    /**
     * 根据会员卡号查询会员
     */
    MarketingMember getByMemberNo(String memberNo);

    /**
     * 更新会员基本信息
     */
    MarketingMember updateMember(Long memberId, MemberUpdateDTO updateDTO);

    /**
     * 分页查询会员列表
     */
    List<MemberVO> queryMembers(MemberQueryDTO queryDTO);

    /**
     * 获取会员详情VO
     */
    MemberVO getMemberDetail(Long memberId);

    /**
     * 获取会员总数（用于统计）
     */
    long countTotal();

    /**
     * 获取今日新增会员数
     */
    long countTodayNew();

    /**
     * 获取指定日期范围内的新增数量
     */
    long countByDateRange(String startDate, String endDate);

    /**
     * 更新会员最后到店时间
     */
    void updateVisitTime(Long memberId);

    /**
     * 检查并执行会员升级
     * @return true表示已升级，false表示无需升级
     */
    boolean checkAndUpgradeLevel(Long memberId);

    /**
     * 冻结/解冻会员
     */
    void toggleStatus(Long memberId, Integer targetStatus);

    /**
     * 更新会员标签
     */
    void updateTags(Long memberId, List<String> tags);
}
