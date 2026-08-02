package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.VoucherLine;
import com.foodtraceability.mapper.VoucherLineMapper;
import com.foodtraceability.service.VoucherLineService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 凭证行服务实现类
 * 提供凭证行的按凭证ID/科目ID查询、批量保存、按凭证ID删除等功能
 */
@Service
public class VoucherLineServiceImpl extends ServiceImpl<VoucherLineMapper, VoucherLine> implements VoucherLineService {

    @Override
    public List<VoucherLine> getByVoucherId(Long voucherId) {
        if (voucherId == null) {
            return Collections.emptyList();
        }
        return this.list(new LambdaQueryWrapper<VoucherLine>()
                .eq(VoucherLine::getVoucherId, voucherId)
                .orderByAsc(VoucherLine::getLineNo));
    }

    @Override
    public List<VoucherLine> getBySubjectId(Long subjectId) {
        if (subjectId == null) {
            return Collections.emptyList();
        }
        return this.list(new LambdaQueryWrapper<VoucherLine>()
                .eq(VoucherLine::getSubjectId, subjectId));
    }

    @Override
    public boolean batchSave(List<VoucherLine> voucherLines) {
        if (voucherLines == null || voucherLines.isEmpty()) {
            return true;
        }
        return this.saveBatch(voucherLines);
    }

    @Override
    public boolean deleteByVoucherId(Long voucherId) {
        if (voucherId == null) {
            return false;
        }
        return this.remove(new LambdaQueryWrapper<VoucherLine>()
                .eq(VoucherLine::getVoucherId, voucherId));
    }
}
