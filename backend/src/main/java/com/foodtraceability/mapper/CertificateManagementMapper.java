package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.CertificateManagement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 索证索票管理Mapper接口
 */
@Mapper
public interface CertificateManagementMapper extends BaseMapper<CertificateManagement> {

    /**
     * 根据供应商ID查询证件列表
     * @param supplierId 供应商ID
     * @return 证件列表
     */
    List<CertificateManagement> selectBySupplierId(@Param("supplierId") Long supplierId);

    /**
     * 根据证件类型查询
     * @param certType 证件类型
     * @return 证件列表
     */
    List<CertificateManagement> selectByCertType(@Param("certType") Integer certType);

    /**
     * 查询即将到期的证件
     * @param days 天数
     * @return 证件列表
     */
    List<CertificateManagement> selectExpiringWithinDays(@Param("days") int days);

    /**
     * 查询已过期的证件
     * @param currentDate 当前日期
     * @return 证件列表
     */
    List<CertificateManagement> selectExpired(@Param("currentDate") LocalDate currentDate);

    /**
     * 查询需要提醒的证件（根据各自的reminder_days）
     * @param currentDate 当前日期
     * @return 证件列表
     */
    List<CertificateManagement> selectNeedReminder(@Param("currentDate") LocalDate currentDate);
}
