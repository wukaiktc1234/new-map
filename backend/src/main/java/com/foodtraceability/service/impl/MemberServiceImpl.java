package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.Member;
import com.foodtraceability.mapper.MemberMapper;
import com.foodtraceability.service.MemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements MemberService {
    
    @Override
    public List<Member> getAllMembers() {
        return list();
    }
    
    @Override
    public Member getByPhone(String phone) {
        return baseMapper.findByPhone(phone);
    }
    
    @Override
    public Member getByMemberNo(String memberNo) {
        return baseMapper.findByMemberNo(memberNo);
    }
    
    @Override
    @Transactional
    public Member registerMember(Member member) {
        if (member.getPhone() != null && getByPhone(member.getPhone()) != null) {
            throw new RuntimeException("该手机号已注册");
        }
        
        member.setMemberNo(generateMemberNo());
        member.setLevel(1);
        member.setLevelName("普通会员");
        member.setBalance(BigDecimal.ZERO);
        member.setPoints(0);
        member.setTotalSpent(BigDecimal.ZERO);
        member.setOrderCount(0);
        member.setStatus("active");
        member.setRegisteredAt(LocalDateTime.now());
        member.setCreatedAt(LocalDateTime.now());
        member.setUpdatedAt(LocalDateTime.now());
        
        save(member);
        return member;
    }
    
    @Override
    @Transactional
    public Member updateMember(Long id, Member member) {
        Member existing = getById(id);
        if (existing == null) {
            throw new RuntimeException("会员不存在");
        }
        
        existing.setName(member.getName());
        existing.setPhone(member.getPhone());
        existing.setGender(member.getGender());
        existing.setBirthday(member.getBirthday());
        existing.setUpdatedAt(LocalDateTime.now());
        
        updateById(existing);
        return existing;
    }
    
    @Override
    @Transactional
    public Member recharge(Long id, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("充值金额必须大于0");
        }
        Member member = getById(id);
        if (member == null) {
            throw new RuntimeException("会员不存在");
        }

        member.setBalance(member.getBalance().add(amount));
        member.setUpdatedAt(LocalDateTime.now());

        updateById(member);
        return member;
    }

    @Override
    @Transactional
    public Member consume(Long id, BigDecimal amount, Integer points) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("消费金额必须大于0");
        }
        Member member = getById(id);
        if (member == null) {
            throw new RuntimeException("会员不存在");
        }

        member.setTotalSpent(member.getTotalSpent().add(amount));
        member.setOrderCount(member.getOrderCount() + 1);
        member.setPoints(member.getPoints() + points);
        member.setLastVisitAt(LocalDateTime.now());
        member.setUpdatedAt(LocalDateTime.now());

        updateLevel(id);
        updateById(member);

        return member;
    }
    
    @Override
    @Transactional
    public void updateLevel(Long id) {
        Member member = getById(id);
        if (member == null) return;
        
        BigDecimal totalSpent = member.getTotalSpent();
        int newLevel;
        String levelName;
        
        if (totalSpent.compareTo(new BigDecimal("10000")) >= 0) {
            newLevel = 5;
            levelName = "钻石会员";
        } else if (totalSpent.compareTo(new BigDecimal("5000")) >= 0) {
            newLevel = 4;
            levelName = "白金会员";
        } else if (totalSpent.compareTo(new BigDecimal("2000")) >= 0) {
            newLevel = 3;
            levelName = "黄金会员";
        } else if (totalSpent.compareTo(new BigDecimal("500")) >= 0) {
            newLevel = 2;
            levelName = "白银会员";
        } else {
            newLevel = 1;
            levelName = "普通会员";
        }
        
        if (newLevel > member.getLevel()) {
            member.setLevel(newLevel);
            member.setLevelName(levelName);
            updateById(member);
        }
    }
    
    @Override
    public List<Member> getTopMembers(int limit) {
        return baseMapper.findTopMembers(limit);
    }

    @Override
    public Map<String, Object> getMemberStats() {
        Map<String, Object> stats = new HashMap<>();
        List<Member> allMembers = list();

        // 总会员数
        stats.put("totalMembers", allMembers.size());

        // 各状态会员数（status: active/inactive/probation）
        Map<String, Long> statusCount = allMembers.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getStatus() != null ? m.getStatus() : "unknown",
                        Collectors.counting()));
        stats.put("statusCount", statusCount);

        // 各等级会员数（level: 1-5）
        Map<Integer, Long> levelCount = allMembers.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getLevel() != null ? m.getLevel() : 0,
                        Collectors.counting()));
        stats.put("levelCount", levelCount);

        // 性别分布
        Map<String, Long> genderCount = allMembers.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getGender() != null ? m.getGender() : "unknown",
                        Collectors.counting()));
        stats.put("genderCount", genderCount);

        return stats;
    }

    @Override
    public Map<String, Object> getMemberOverview() {
        Map<String, Object> overview = new HashMap<>();
        List<Member> allMembers = list();

        // 总会员数
        overview.put("totalMembers", allMembers.size());

        // 今日新增会员数
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayNew = allMembers.stream()
                .filter(m -> m.getRegisteredAt() != null && !m.getRegisteredAt().isBefore(todayStart))
                .count();
        overview.put("todayNewMembers", todayNew);

        // 本月新增会员数
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long monthNew = allMembers.stream()
                .filter(m -> m.getRegisteredAt() != null && !m.getRegisteredAt().isBefore(monthStart))
                .count();
        overview.put("monthNewMembers", monthNew);

        // 活跃会员数（status为active）
        long activeCount = allMembers.stream()
                .filter(m -> "active".equals(m.getStatus()))
                .count();
        overview.put("activeMembers", activeCount);

        // 总余额
        BigDecimal totalBalance = allMembers.stream()
                .map(Member::getBalance)
                .filter(b -> b != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        overview.put("totalBalance", totalBalance);

        // 总积分
        int totalPoints = allMembers.stream()
                .mapToInt(m -> m.getPoints() != null ? m.getPoints() : 0)
                .sum();
        overview.put("totalPoints", totalPoints);

        // 总消费金额
        BigDecimal totalSpent = allMembers.stream()
                .map(Member::getTotalSpent)
                .filter(s -> s != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        overview.put("totalSpent", totalSpent);

        // 总订单数
        int totalOrders = allMembers.stream()
                .mapToInt(m -> m.getOrderCount() != null ? m.getOrderCount() : 0)
                .sum();
        overview.put("totalOrders", totalOrders);

        return overview;
    }

    private String generateMemberNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return "M" + dateStr + random;
    }
}
