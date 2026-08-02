package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.VoucherHeader;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;
import java.util.Date;

/**
 * 凭证头Mapper
 * @author example
 * @since 2025-12-06
 */
@Mapper
public interface VoucherHeaderMapper extends BaseMapper<VoucherHeader> {
    
    /**
     * 根据凭证状态获取凭证列表
     * @param status 凭证状态
     * @return 凭证列表
     */
    @Select("SELECT * FROM voucher_header WHERE status = #{status} ORDER BY voucher_date DESC, voucher_no")
    List<VoucherHeader> selectByStatus(String status);
    
    /**
     * 根据会计期间获取凭证列表
     * @param period 会计期间
     * @return 凭证列表
     */
    @Select("SELECT * FROM voucher_header WHERE period = #{period} ORDER BY voucher_date DESC, voucher_no")
    List<VoucherHeader> selectByPeriod(String period);
    
    /**
     * 根据凭证日期范围获取凭证列表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 凭证列表
     */
    @Select("SELECT * FROM voucher_header WHERE voucher_date BETWEEN #{startDate} AND #{endDate} ORDER BY voucher_date DESC, voucher_no")
    List<VoucherHeader> selectByDateRange(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
    
    /**
     * 根据凭证编号获取凭证
     * @param voucherNo 凭证编号
     * @return 凭证信息
     */
    @Select("SELECT * FROM voucher_header WHERE voucher_no = #{voucherNo}")
    VoucherHeader selectByVoucherNo(String voucherNo);
    
    /**
     * 更新凭证状态
     * @param id 凭证ID
     * @param status 新状态
     * @return 更新结果
     */
    @Update("UPDATE voucher_header SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);
    
    /**
     * 获取下一个凭证编号
     * @param prefix 编号前缀
     * @return 下一个凭证编号
     */
    @Select("SELECT CONCAT(TO_CHAR(NOW(), 'YYYYMMDD'), LPAD(COALESCE(MAX(SUBSTRING(voucher_no, 9)), 0) + 1, 3, '0')) FROM voucher_header WHERE voucher_no LIKE CONCAT(TO_CHAR(NOW(), 'YYYYMMDD'), '%')")
    String getNextVoucherNo(@Param("prefix") String prefix);
    
    /**
     * 批量更新凭证状态
     * @param ids 凭证ID列表
     * @param status 新状态
     * @return 更新结果
     */
    @Update({"<script>",
        "UPDATE voucher_header SET status = #{status}, update_time = NOW() WHERE id IN ",
        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
        "#{id}",
        "</foreach>",
        "</script>"})
    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") String status);
    
    /**
     * 生成凭证编号
     * @return 凭证编号
     */
    @Select("SELECT CONCAT(TO_CHAR(NOW(), 'YYYYMMDD'), LPAD(COALESCE(MAX(SUBSTRING(voucher_no, 9)), 0) + 1, 3, '0')) FROM voucher_header WHERE voucher_no LIKE CONCAT(TO_CHAR(NOW(), 'YYYYMMDD'), '%')")
    String generateVoucherNumber();

    /**
     * 查询指定会计期间的最大凭证号
     * 凭证号格式：V + YYYYMM + 6位流水号（如 V202607000001）
     * 用于凭证号生成器计算下一个流水号，保证凭证号连续性与唯一性
     * @param period 会计期间（格式：yyyy-MM）
     * @return 当前期间最大凭证号；无数据时返回 null
     */
    @Select("SELECT MAX(voucher_no) FROM voucher_header WHERE period = #{period} AND voucher_no IS NOT NULL")
    String selectMaxVoucherNoByPeriod(@Param("period") String period);
}
