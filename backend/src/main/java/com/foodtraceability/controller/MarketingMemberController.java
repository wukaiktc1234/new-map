package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.marketing.MemberCreateDTO;
import com.foodtraceability.dto.marketing.MemberQueryDTO;
import com.foodtraceability.dto.marketing.MemberUpdateDTO;
import com.foodtraceability.dto.marketing.MemberVO;
import com.foodtraceability.entity.MarketingMember;
import com.foodtraceability.service.MarketingMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 营销CRM会员管理控制器
 */
@RestController
@RequestMapping("/v1/members")
@Tag(name = "营销会员管理", description = "会员注册、信息管理、等级升级、标签管理等")
public class MarketingMemberController {

    private final MarketingMemberService memberService;

    public MarketingMemberController(MarketingMemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    @Operation(summary = "会员注册")
    @PreAuthorize("hasAuthority('member:manage') or hasAuthority('*')")
    public Result<MarketingMember> register(@RequestBody MemberCreateDTO createDTO) {
        try {
            MarketingMember member = memberService.register(createDTO);
            return Result.success(member, "注册成功");
        } catch (RuntimeException e) {
            return Result.error(4001, e.getMessage());
        }
    }

    @GetMapping("/{memberId}")
    @Operation(summary = "获取会员详情")
    @PreAuthorize("hasAuthority('member:view') or hasAuthority('*')")
    public Result<MemberVO> getDetail(@PathVariable Long memberId) {
        MemberVO vo = memberService.getMemberDetail(memberId);
        if (vo == null) {
            return Result.error(4004, "会员不存在");
        }
        return Result.success(vo);
    }

    @GetMapping
    @Operation(summary = "分页查询会员列表")
    @PreAuthorize("hasAuthority('member:view') or hasAuthority('*')")
    public Result<List<MemberVO>> queryList(MemberQueryDTO queryDTO) {
        List<MemberVO> list = memberService.queryMembers(queryDTO);
        return Result.success(list);
    }

    @GetMapping("/phone/{phone}")
    @Operation(summary = "根据手机号查询会员")
    @PreAuthorize("hasAuthority('member:view') or hasAuthority('*')")
    public Result<MarketingMember> getByPhone(@PathVariable String phone) {
        MarketingMember member = memberService.getByPhone(phone);
        if (member == null) {
            return Result.error(4004, "会员不存在");
        }
        return Result.success(member);
    }

    @PutMapping("/{memberId}")
    @Operation(summary = "更新会员信息")
    @PreAuthorize("hasAuthority('member:manage') or hasAuthority('*')")
    public Result<MarketingMember> update(@PathVariable Long memberId,
                                         @RequestBody MemberUpdateDTO updateDTO) {
        try {
            MarketingMember member = memberService.updateMember(memberId, updateDTO);
            return Result.success(member, "更新成功");
        } catch (RuntimeException e) {
            return Result.error(4002, e.getMessage());
        }
    }

    @PostMapping("/{memberId}/visit")
    @Operation(summary = "更新到店时间")
    @PreAuthorize("hasAuthority('member:manage') or hasAuthority('*')")
    public Result<Void> recordVisit(@PathVariable Long memberId) {
        try {
            memberService.updateVisitTime(memberId);
            return Result.success(null, "到店记录已更新");
        } catch (Exception e) {
            return Result.error(4003, e.getMessage());
        }
    }

    @PostMapping("/{memberId}/upgrade-level")
    @Operation(summary = "检查并执行等级升级")
    @PreAuthorize("hasAuthority('member:manage') or hasAuthority('*')")
    public Result<Boolean> checkUpgrade(@PathVariable Long memberId) {
        boolean upgraded = memberService.checkAndUpgradeLevel(memberId);
        return Result.success(upgraded, upgraded ? "已升级" : "无需升级");
    }

    @PutMapping("/{memberId}/status")
    @Operation(summary = "冻结/解冻/拉黑会员")
    @PreAuthorize("hasAuthority('member:manage') or hasAuthority('*')")
    public Result<Void> toggleStatus(@PathVariable Long memberId,
                                    @RequestParam Integer status) {
        if (status < 1 || status > 4) {
            return Result.error(4005, "无效的状态值");
        }
        try {
            memberService.toggleStatus(memberId, status);
            return Result.success(null, "状态更新成功");
        } catch (RuntimeException e) {
            return Result.error(4006, e.getMessage());
        }
    }

    @GetMapping("/statistics/summary")
    @Operation(summary = "会员统计概览")
    @PreAuthorize("hasAuthority('member:view') or hasAuthority('*')")
    public Result<Object> getSummary() {
        java.util.Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("totalMembers", memberService.countTotal());
        summary.put("todayNew", memberService.countTodayNew());
        return Result.success(summary);
    }

    /**
     * 会员概览统计（前端 MemberStatsOverview 对应接口）
     * 对应前端调用：GET /v1/members/stats/overview
     * 返回会员概览统计数据，当前为占位实现，返回零值
     */
    @GetMapping("/stats/overview")
    @Operation(summary = "会员概览统计", description = "获取会员概览统计数据（总数、新增、活跃、储值等）")
    @PreAuthorize("hasAuthority('member:view') or hasAuthority('*')")
    public Result<java.util.Map<String, Object>> getStatsOverview() {
        java.util.Map<String, Object> overview = new java.util.HashMap<>();
        overview.put("totalMembers", memberService.countTotal());
        overview.put("newThisMonth", 0);
        overview.put("newToday", memberService.countTodayNew());
        overview.put("activeMembers", 0);
        overview.put("dormantMembers", 0);
        overview.put("atRiskCount", 0);
        overview.put("totalBalance", "0.00");
        overview.put("rechargeThisMonth", "0.00");
        overview.put("consumeThisMonth", "0.00");
        overview.put("consumeRatio", "0.00");
        overview.put("avgOrderAmount", "0.00");
        overview.put("repurchaseRate", "0.00");
        return Result.success(overview);
    }
}
