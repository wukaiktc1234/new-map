package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.Member;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface MemberService extends IService<Member> {

    List<Member> getAllMembers();

    Member getByPhone(String phone);

    Member getByMemberNo(String memberNo);

    Member registerMember(Member member);

    Member updateMember(Long id, Member member);

    Member recharge(Long id, BigDecimal amount);

    Member consume(Long id, BigDecimal amount, Integer points);

    void updateLevel(Long id);

    List<Member> getTopMembers(int limit);

    /**
     * 会员统计
     * 返回总会员数、各状态会员数、各等级会员数等统计指标
     *
     * @return 统计数据Map
     */
    Map<String, Object> getMemberStats();

    /**
     * 会员概览
     * 返回总会员数、今日新增、本月新增、活跃会员数、总余额、总积分等概览指标
     *
     * @return 概览数据Map
     */
    Map<String, Object> getMemberOverview();
}
