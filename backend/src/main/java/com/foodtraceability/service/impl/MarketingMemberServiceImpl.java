package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.marketing.MemberCreateDTO;
import com.foodtraceability.dto.marketing.MemberQueryDTO;
import com.foodtraceability.dto.marketing.MemberUpdateDTO;
import com.foodtraceability.dto.marketing.MemberVO;
import com.foodtraceability.entity.MarketingMember;
import com.foodtraceability.entity.MemberLevel;
import com.foodtraceability.mapper.MarketingMemberMapper;
import com.foodtraceability.service.MemberLevelService;
import com.foodtraceability.service.MarketingMemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 营销CRM会员服务实现
 */
@Service
public class MarketingMemberServiceImpl extends ServiceImpl<MarketingMemberMapper, MarketingMember>
        implements MarketingMemberService {

    private static final Logger logger = LoggerFactory.getLogger(MarketingMemberServiceImpl.class);

    private final MarketingMemberMapper marketingMemberMapper;

    private final MemberLevelService memberLevelService;

    private final PasswordEncoder passwordEncoder;

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * @param marketingMemberMapper 营销会员Mapper
     * @param memberLevelService 会员等级服务
     * @param passwordEncoder 密码编码器
     */
    public MarketingMemberServiceImpl(MarketingMemberMapper marketingMemberMapper, MemberLevelService memberLevelService, PasswordEncoder passwordEncoder) {
        this.marketingMemberMapper = marketingMemberMapper;
        this.memberLevelService = memberLevelService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketingMember register(MemberCreateDTO createDTO) {
        // 检查手机号是否已注册
        MarketingMember existing = getByPhone(createDTO.getPhone());
        if (existing != null) {
            throw new RuntimeException("该手机号已注册");
        }

        // 创建会员实体
        MarketingMember member = new MarketingMember();
        // 生成会员卡号（使用时间戳+随机数）
        String memberNo = generateMemberNo();
        member.setMemberNo(memberNo);
        member.setPhone(createDTO.getPhone());

        // 密码加密存储（如果提供）
        if (createDTO.getPassword() != null && !createDTO.getPassword().isEmpty()) {
            member.setPassword(passwordEncoder.encode(createDTO.getPassword()));
        }

        member.setNickname(createDTO.getNickname());
        member.setAvatarUrl(createDTO.getAvatarUrl());
        member.setGender(createDTO.getGender() != null ? createDTO.getGender() : 0);
        member.setBirthday(createDTO.getBirthday());
        member.setEmail(createDTO.getEmail());
        member.setRegisterChannel(createDTO.getRegisterChannel());
        member.setStatus(1); // 默认正常状态
        member.setPoints(0);
        member.setBalance(0L);

        // 设置默认等级（普通会员）
        MemberLevel defaultLevel = memberLevelService.getByCode("NORMAL");
        if (defaultLevel != null) {
            member.setMemberLevelId(defaultLevel.getLevelId());
        }

        // 设置标签为空列表（JacksonTypeHandler 自动序列化为 JSONB 空数组）
        member.setTags(new ArrayList<>());

        // 保存到数据库
        save(member);
        logger.info("会员注册成功: memberId={}, phone={}, memberNo={}", member.getMemberId(), member.getPhone(), member.getMemberNo());

        return member;
    }

    @Override
    public MarketingMember getByPhone(String phone) {
        return marketingMemberMapper.findByPhone(phone);
    }

    @Override
    public MarketingMember getByMemberNo(String memberNo) {
        return marketingMemberMapper.findByMemberNo(memberNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketingMember updateMember(Long memberId, MemberUpdateDTO updateDTO) {
        MarketingMember member = getById(memberId);
        if (member == null) {
            throw new RuntimeException("会员不存在");
        }

        // 只更新非空字段
        if (updateDTO.getNickname() != null) {
            member.setNickname(updateDTO.getNickname());
        }
        if (updateDTO.getAvatarUrl() != null) {
            member.setAvatarUrl(updateDTO.getAvatarUrl());
        }
        if (updateDTO.getGender() != null) {
            member.setGender(updateDTO.getGender());
        }
        if (updateDTO.getBirthday() != null) {
            member.setBirthday(updateDTO.getBirthday());
        }
        if (updateDTO.getEmail() != null) {
            member.setEmail(updateDTO.getEmail());
        }
        if (updateDTO.getRemark() != null) {
            member.setRemark(updateDTO.getRemark());
        }

        updateById(member);
        return member;
    }

    @Override
    public List<MemberVO> queryMembers(MemberQueryDTO queryDTO) {
        LambdaQueryWrapper<MarketingMember> wrapper = buildQueryWrapper(queryDTO);
        wrapper.orderByDesc(MarketingMember::getCreateTime);

        Page<MarketingMember> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        Page<MarketingMember> resultPage = page(page, wrapper);

        return resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public MemberVO getMemberDetail(Long memberId) {
        MarketingMember member = getById(memberId);
        if (member == null) {
            return null;
        }
        return convertToVO(member);
    }

    @Override
    public long countTotal() {
        return count(new LambdaQueryWrapper<MarketingMember>()
                .eq(MarketingMember::getStatus, 1));
    }

    @Override
    public long countTodayNew() {
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        return count(new LambdaQueryWrapper<MarketingMember>()
                .ge(MarketingMember::getCreateTime, todayStart));
    }

    @Override
    public long countByDateRange(String startDate, String endDate) {
        // 简化处理：实际应解析日期字符串
        return count(new LambdaQueryWrapper<MarketingMember>()
                .ge(MarketingMember::getStatus, 1));
    }

    @Override
    public void updateVisitTime(Long memberId) {
        marketingMemberMapper.updateLastVisitTime(memberId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean checkAndUpgradeLevel(Long memberId) {
        MarketingMember member = getById(memberId);
        if (member == null || member.getStatus() != 1) {
            return false;
        }

        // 计算应该升级到的等级
        Long newLevelId = memberLevelService.calculateUpgradeLevel(
                member.getPoints(),
                member.getTotalConsume()
        );

        if (newLevelId != null && !newLevelId.equals(member.getMemberLevelId())) {
            // 执行升级
            marketingMemberMapper.updateMemberLevel(memberId, newLevelId);
            logger.info("会员升级: memberId={}, oldLevel={}, newLevel={}",
                    memberId, member.getMemberLevelId(), newLevelId);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long memberId, Integer targetStatus) {
        MarketingMember member = getById(memberId);
        if (member == null) {
            throw new RuntimeException("会员不存在");
        }
        member.setStatus(targetStatus);
        updateById(member);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTags(Long memberId, List<String> tags) {
        MarketingMember member = getById(memberId);
        if (member == null) {
            throw new RuntimeException("会员不存在");
        }
        member.setTags(tags);
        updateById(member);
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<MarketingMember> buildQueryWrapper(MemberQueryDTO queryDTO) {
        LambdaQueryWrapper<MarketingMember> wrapper = new LambdaQueryWrapper<>();

        if (queryDTO.getMemberNo() != null && !queryDTO.getMemberNo().isEmpty()) {
            wrapper.like(MarketingMember::getMemberNo, queryDTO.getMemberNo());
        }
        if (queryDTO.getPhone() != null && !queryDTO.getPhone().isEmpty()) {
            wrapper.eq(MarketingMember::getPhone, queryDTO.getPhone());
        }
        if (queryDTO.getNickname() != null && !queryDTO.getNickname().isEmpty()) {
            wrapper.like(MarketingMember::getNickname, queryDTO.getNickname());
        }
        if (queryDTO.getMemberLevelId() != null) {
            wrapper.eq(MarketingMember::getMemberLevelId, queryDTO.getMemberLevelId());
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(MarketingMember::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getRegisterChannel() != null) {
            wrapper.eq(MarketingMember::getRegisterChannel, queryDTO.getRegisterChannel());
        }
        if (queryDTO.getCustomerSegment() != null) {
            wrapper.eq(MarketingMember::getCustomerSegment, queryDTO.getCustomerSegment());
        }
        if (queryDTO.getPointsMin() != null) {
            wrapper.ge(MarketingMember::getPoints, queryDTO.getPointsMin());
        }
        if (queryDTO.getPointsMax() != null) {
            wrapper.le(MarketingMember::getPoints, queryDTO.getPointsMax());
        }
        if (queryDTO.getBalanceMin() != null) {
            wrapper.ge(MarketingMember::getBalance, queryDTO.getBalanceMin());
        }
        if (queryDTO.getBalanceMax() != null) {
            wrapper.le(MarketingMember::getBalance, queryDTO.getBalanceMax());
        }

        return wrapper;
    }

    /**
     * 实体转VO
     */
    private MemberVO convertToVO(MarketingMember member) {
        MemberVO vo = new MemberVO();
        vo.setMemberId(member.getMemberId());
        vo.setMemberNo(member.getMemberNo());
        vo.setPhone(desensitizePhone(member.getPhone()));
        vo.setNickname(member.getNickname());
        vo.setAvatarUrl(member.getAvatarUrl());
        vo.setGender(member.getGender());
        vo.setGenderName(getGenderName(member.getGender()));
        vo.setBirthday(member.getBirthday());
        vo.setEmail(member.getEmail());
        vo.setMemberLevelId(member.getMemberLevelId());
        vo.setPoints(member.getPoints());
        vo.setTotalPointsEarned(member.getTotalPointsEarned());
        vo.setTotalPointsUsed(member.getTotalPointsUsed());
        vo.setBalance(member.getBalance());
        vo.setBalanceDisplay(formatFenToYuan(member.getBalance()));
        vo.setTotalRecharge(member.getTotalRecharge());
        vo.setTotalConsume(member.getTotalConsume());
        vo.setTotalConsumeDisplay(formatFenToYuan(member.getTotalConsume()));
        vo.setOrderCount(member.getOrderCount());
        vo.setLastOrderTime(member.getLastOrderTime());
        vo.setLastVisitTime(member.getLastVisitTime());
        vo.setRegisterChannel(member.getRegisterChannel());
        vo.setRegisterChannelName(getChannelName(member.getRegisterChannel()));
        vo.setStatus(member.getStatus());
        vo.setStatusName(getStatusName(member.getStatus()));
        vo.setRemark(member.getRemark());
        vo.setrScore(member.getrScore());
        vo.setfScore(member.getfScore());
        vo.setmScore(member.getmScore());
        vo.setCustomerSegment(member.getCustomerSegment());
        vo.setCustomerSegmentDesc(getSegmentDesc(member.getCustomerSegment()));
        vo.setCreateTime(member.getCreateTime());

        // 填充等级名称
        if (member.getMemberLevelId() != null) {
            MemberLevel level = memberLevelService.getById(member.getMemberLevelId());
            if (level != null) {
                vo.setLevelName(level.getLevelName());
                vo.setLevelCode(level.getLevelCode());
            }
        }

        // 解析标签（JacksonTypeHandler 已自动反序列化为 List<String>）
        if (member.getTags() != null) {
            vo.setTagsList(member.getTags());
        } else {
            vo.setTagsList(new ArrayList<>());
        }

        return vo;
    }

    /**
     * 生成会员卡号
     */
    private String generateMemberNo() {
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 10000);
        return "M" + timestamp + String.format("%04d", random);
    }

    /** 手机号脱敏 */
    private String desensitizePhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /** 分转元格式化 */
    private String formatFenToYuan(Long fen) {
        if (fen == null) return "0.00";
        return String.format("%.2f", fen / 100.0);
    }

    private String getGenderName(Integer gender) {
        if (gender == null) return "未知";
        switch (gender) {
            case 1: return "男";
            case 2: return "女";
            default: return "未知";
        }
    }

    private String getChannelName(Integer channel) {
        if (channel == null) return "未知";
        switch (channel) {
            case 1: return "APP注册";
            case 2: return "收银台注册";
            case 3: return "扫码注册";
            case 4: return "导入";
            case 5: return "小程序";
            default: return "未知";
        }
    }

    private String getStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1: return "正常";
            case 2: return "冻结";
            case 3: return "黑名单";
            case 4: return "已注销";
            default: return "未知";
        }
    }

    private String getSegmentDesc(String segment) {
        if (segment == null) return "未分类";
        switch (segment) {
            case "VIP_VALUE": return "重要价值客户";
            case "VIP_DEVELOP": return "重要发展客户";
            case "VIP_KEEP": return "重要保持客户";
            case "GENERAL": return "一般客户";
            case "CHURN_RISK": return "流失风险";
            default: return segment;
        }
    }
}
