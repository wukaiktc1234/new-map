package com.foodtraceability.controller.pos;

import com.foodtraceability.common.Result;
import com.foodtraceability.entity.Member;
import com.foodtraceability.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * POS会员管理控制器
 * 对应前端 API 路径 /v1/pos/members
 *
 * 注意：/v1/members 路径已由 MarketingMemberController 占用（营销CRM会员管理），
 * 本Controller负责POS端会员操作（充值、消费、排行等），保持 /v1/pos/members 路径
 *
 * 端点说明：
 * 1. GET    /v1/pos/members                - 获取所有会员
 * 2. GET    /v1/pos/members/{id}           - 根据ID获取会员
 * 3. GET    /v1/pos/members/phone/{phone}  - 根据手机号获取会员
 * 4. GET    /v1/pos/members/member-no/{no} - 根据会员号获取会员
 * 5. GET    /v1/pos/members/top            - 获取消费排行
 * 6. GET    /v1/pos/members/export         - 导出会员列表
 * 7. GET    /v1/pos/members/stats          - 会员统计
 * 8. GET    /v1/pos/members/overview       - 会员概览
 * 9. POST   /v1/pos/members                - 注册会员
 * 10. PUT   /v1/pos/members/{id}           - 更新会员信息
 * 11. POST  /v1/pos/members/{id}/recharge  - 会员充值
 * 12. POST  /v1/pos/members/{id}/consume   - 会员消费
 * 13. DELETE /v1/pos/members/{id}          - 删除会员
 *
 * 路径匹配优先级：字面量路径（phone/member-no/top/export/stats/overview）优先于 {id} 匹配
 */
@RestController
@RequestMapping("/v1/pos/members")
@Tag(name = "POS会员API", description = "POS端会员管理接口")
public class MemberController {
    private final MemberService memberService;

    public MemberController(final MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    @Operation(summary = "获取所有会员")
    @PreAuthorize("hasAuthority('member:view') or hasAuthority('*')")
    public Result<List<Member>> getAllMembers() {
        return Result.success(memberService.getAllMembers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取会员")
    @PreAuthorize("hasAuthority('member:view') or hasAuthority('*')")
    public Result<Member> getMemberById(@PathVariable Long id) {
        Member member = memberService.getById(id);
        if (member == null) {
            return Result.error("会员不存在");
        }
        return Result.success(member);
    }

    @GetMapping("/phone/{phone}")
    @Operation(summary = "根据手机号获取会员")
    @PreAuthorize("hasAuthority('member:view') or hasAuthority('*')")
    public Result<Member> getMemberByPhone(@PathVariable String phone) {
        Member member = memberService.getByPhone(phone);
        if (member == null) {
            return Result.error("会员不存在");
        }
        return Result.success(member);
    }

    @GetMapping("/member-no/{memberNo}")
    @Operation(summary = "根据会员号获取会员")
    @PreAuthorize("hasAuthority('member:view') or hasAuthority('*')")
    public Result<Member> getMemberByMemberNo(@PathVariable String memberNo) {
        Member member = memberService.getByMemberNo(memberNo);
        if (member == null) {
            return Result.error("会员不存在");
        }
        return Result.success(member);
    }

    @GetMapping("/top")
    @Operation(summary = "获取消费排行")
    @PreAuthorize("hasAuthority('member:view') or hasAuthority('*')")
    public Result<List<Member>> getTopMembers(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(memberService.getTopMembers(limit));
    }

    /**
     * 导出会员列表
     * 返回全量会员数据，前端基于此进行文件导出（Excel/CSV）
     */
    @GetMapping("/export")
    @Operation(summary = "导出会员列表")
    @PreAuthorize("hasAuthority('member:export') or hasAuthority('member:view') or hasAuthority('*')")
    public Result<List<Member>> exportMembers() {
        try {
            List<Member> members = memberService.getAllMembers();
            return Result.success(members, "导出会员列表成功");
        } catch (Exception e) {
            return Result.error("导出会员列表失败: " + e.getMessage());
        }
    }

    /**
     * 会员统计
     * 返回总会员数、各状态会员数、各等级会员数、性别分布等统计指标
     */
    @GetMapping("/stats")
    @Operation(summary = "会员统计")
    @PreAuthorize("hasAuthority('member:view') or hasAuthority('*')")
    public Result<Map<String, Object>> getMemberStats() {
        try {
            Map<String, Object> stats = memberService.getMemberStats();
            return Result.success(stats, "获取会员统计成功");
        } catch (Exception e) {
            return Result.error("获取会员统计失败: " + e.getMessage());
        }
    }

    /**
     * 会员概览
     * 返回总会员数、今日新增、本月新增、活跃会员数、总余额、总积分等概览指标
     */
    @GetMapping("/overview")
    @Operation(summary = "会员概览")
    @PreAuthorize("hasAuthority('member:view') or hasAuthority('*')")
    public Result<Map<String, Object>> getMemberOverview() {
        try {
            Map<String, Object> overview = memberService.getMemberOverview();
            return Result.success(overview, "获取会员概览成功");
        } catch (Exception e) {
            return Result.error("获取会员概览失败: " + e.getMessage());
        }
    }

    @PostMapping
    @Operation(summary = "注册会员")
    @PreAuthorize("hasAuthority('member:manage') or hasAuthority('*')")
    public Result<Member> registerMember(@RequestBody Member member) {
        try {
            return Result.success(memberService.registerMember(member));
        } catch (Exception e) {
            return Result.error("注册失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新会员信息")
    @PreAuthorize("hasAuthority('member:manage') or hasAuthority('*')")
    public Result<Member> updateMember(@PathVariable Long id, @RequestBody Member member) {
        try {
            return Result.success(memberService.updateMember(id, member));
        } catch (Exception e) {
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/recharge")
    @Operation(summary = "会员充值")
    @PreAuthorize("hasAuthority('member:manage') or hasAuthority('*')")
    public Result<Member> recharge(@PathVariable Long id, @RequestParam BigDecimal amount) {
        try {
            return Result.success(memberService.recharge(id, amount));
        } catch (Exception e) {
            return Result.error("充值失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/consume")
    @Operation(summary = "会员消费")
    @PreAuthorize("hasAuthority('member:manage') or hasAuthority('*')")
    public Result<Member> consume(@PathVariable Long id, @RequestParam BigDecimal amount, @RequestParam(defaultValue = "0") Integer points) {
        try {
            return Result.success(memberService.consume(id, amount, points));
        } catch (Exception e) {
            return Result.error("消费失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除会员")
    @PreAuthorize("hasAuthority('member:manage') or hasAuthority('*')")
    public Result<Void> deleteMember(@PathVariable Long id) {
        try {
            memberService.removeById(id);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }
}
