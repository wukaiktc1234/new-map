package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.VoucherArchiveDTO;
import com.foodtraceability.dto.VoucherArchiveQueryDTO;

import java.util.List;

/**
 * 电子凭证归档服务接口
 *
 * 依据：财政部《电子凭证会计数据标准应用指南(推广应用版1.0)》
 * 实现电子凭证的电子档案管理功能
 */
public interface VoucherArchiveService {

    /**
     * 归档电子凭证
     *
     * @param electronicVoucherIds 电子凭证ID列表
     * @param archiveInfo 归档信息
     * @return 归档结果
     */
    boolean archiveVouchers(List<Long> electronicVoucherIds, VoucherArchiveDTO archiveInfo);

    /**
     * 查询归档列表
     *
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<VoucherArchiveDTO> queryArchivePage(VoucherArchiveQueryDTO query);

    /**
     * 获取归档详情
     *
     * @param archiveId 归档ID
     * @return 归档详情
     */
    VoucherArchiveDTO getArchiveDetail(Long archiveId);

    /**
     * 生成归档包
     *
     * @param archiveId 归档ID
     * @return 归档包下载路径
     */
    String generateArchivePackage(Long archiveId);

    /**
     * 验证归档完整性
     *
     * @param archiveId 归档ID
     * @return 验证结果
     */
    ArchiveVerifyResult verifyArchiveIntegrity(Long archiveId);

    /**
     * 导出XBRL格式归档文件
     *
     * @param archiveId 归档ID
     * @return XBRL文件路径
     */
    String exportXbrlArchive(Long archiveId);

    /**
     * 归档验证结果
     */
    class ArchiveVerifyResult {
        private boolean valid;
        private String message;
        private int totalCount;
        private int validCount;
        private int invalidCount;

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
        public int getValidCount() { return validCount; }
        public void setValidCount(int validCount) { this.validCount = validCount; }
        public int getInvalidCount() { return invalidCount; }
        public void setInvalidCount(int invalidCount) { this.invalidCount = invalidCount; }
    }
}
